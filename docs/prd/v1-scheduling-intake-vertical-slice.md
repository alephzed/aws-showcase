# PRD v1 — Patient Scheduling & Intake: AppointmentBooked Vertical Slice

> **Status:** Draft (PRD as document, per project convention — not a tracker issue).
> **Scope:** The runnable Fidelity-B vertical slice of the architecture described in
> [`docs/architecture.md`](../architecture.md). Implementation issues are derived from this PRD
> via `/to-issues`, each linking back to this path.
> **Related ADRs:** [0001](../adr/0001-bounded-context-decomposition.md),
> [0003](../adr/0003-event-driven-backbone-eventbridge-sqs.md),
> [0004](../adr/0004-choreography-and-reschedule-saga.md),
> [0007](../adr/0007-relational-and-nosql-data-stores.md),
> [0008](../adr/0008-security-and-hipaa.md), [0009](../adr/0009-ha-and-resilience.md),
> [0011](../adr/0011-observability.md).

## Problem Statement

A patient or clinic staff member needs to book and manage appointments and complete intake quickly
and reliably. On the legacy monolith, booking, intake, eligibility, and notifications are entangled
in one application and one database: a slow insurance-eligibility lookup can stall a booking, the
system can't be scaled or deployed per-capability, and there's no safe, demonstrable seam to evolve
one capability without risking the others. From the user's perspective this shows up as slow,
fragile booking and an inability to ship improvements safely.

## Solution

Deliver the core booking journey as a small set of independently deployable services integrated by
events, so that:

- Booking is a fast, strongly-consistent operation that **cannot double-book a slot**.
- Downstream effects (intake form creation, confirmation/reminders, eligibility) happen
  **asynchronously** and **cannot block or fail the booking**.
- A slow/failed external payer **degrades gracefully** — the appointment is still booked, eligibility
  is marked `pending`.
- The whole slice **runs locally** (docker-compose + LocalStack) so the behavior is demonstrable
  end-to-end, and the same test harness runs in CI.

The slice realizes three of the five bounded contexts — **Scheduling**, **Intake**, **Notification** —
driven by the `AppointmentBooked` domain event, plus a standalone **Eligibility circuit-breaker**
artifact demonstrating graceful degradation.

## User Stories

1. As a patient, I want to see a provider's available appointment slots, so that I can choose a time that works for me.
2. As a patient, I want to book an available slot, so that I can secure an appointment.
3. As a patient, I want to receive a confirmation after booking, so that I know my appointment is set.
4. As a patient, I want a reminder before my appointment, so that I don't forget it.
5. As a patient, I want to reschedule an appointment, so that I can move it without losing my place if the new time is unavailable.
6. As a patient, I want to cancel an appointment, so that the slot is freed for others.
7. As a patient, I want my booking to succeed even when the insurance system is slow or down, so that I'm not blocked by problems outside my control.
8. As a patient, I want to complete an intake form for my appointment, so that the clinic has what it needs before I arrive.
9. As a patient, I want intake forms appropriate to the visit type, so that I'm only asked relevant questions.
10. As clinic staff, I want a booking to be rejected if the slot was just taken, so that two patients are never given the same slot.
11. As clinic staff, I want to see the eligibility status of an appointment (verified/pending/failed), so that I can follow up when needed.
12. As clinic staff, I want an intake form automatically created when an appointment is booked, so that I don't have to create it manually.
13. As clinic staff, I want confirmations and reminders to be sent automatically, so that no-shows are reduced without manual effort.
14. As clinic staff, I want a rescheduled appointment to atomically release the old slot and take the new one, so that no slot is lost or double-held.
15. As an operator, I want each service to be independently deployable, so that I can ship one capability without redeploying the others.
16. As an operator, I want failed event processing to land in a dead-letter queue, so that no notification or intake creation is silently lost.
17. As an operator, I want event processing to be idempotent, so that at-least-once redelivery doesn't create duplicate intake forms or duplicate notifications.
18. As an operator, I want to trace a single booking across services via a correlation ID, so that I can diagnose issues in the asynchronous flow.
19. As an operator, I want PHI redacted from logs, so that we don't leak sensitive data into log storage.
20. As an operator, I want sensitive fields (e.g. SSN, insurance member ID) encrypted at the field level, so that a database read alone cannot expose them.
21. As a developer, I want to run the entire slice locally (docker-compose + LocalStack), so that I can demonstrate and test the end-to-end flow without AWS.
22. As a developer, I want the same test harness to run locally and in CI, so that behavior is validated consistently.
23. As a developer, I want event and API contracts verified by contract tests, so that a producer cannot ship a breaking change without a failing build.
24. As a developer, I want the booking API to return a clear `409 Conflict` when a slot is already taken, so that clients can retry cleanly.
25. As a developer, I want database schema changes applied via versioned migrations, so that schema evolution is reviewable and repeatable.

## Implementation Decisions

**Services (modules) built in this slice**

- **Scheduling service** (Spring Boot, PostgreSQL via RDS Proxy locally emulated by Postgres):
  owns appointments and provider availability slots. Exposes the booking/reschedule/cancel API.
  Publishes domain events. Source of truth for the booking transaction.
- **Intake service** (Spring Boot, DynamoDB single-table): consumes `AppointmentBooked` and creates
  a blank intake form for the appointment; exposes form retrieval.
- **Notification service** (Lambda-style consumer, stateless): consumes `AppointmentBooked` and
  `AppointmentRescheduled`; sends confirmation + schedules reminder (provider stubbed locally).
- **Eligibility circuit-breaker artifact** (standalone module): wraps a stubbed external payer call
  with circuit breaker + timeout + bounded retry; demonstrates graceful degradation. Not wired as a
  full service in this slice.

**API contracts (OpenAPI 3.1 is the source of truth; see [ADR-0006](../adr/0006-api-rest-cognito.md))**

- `GET /providers/{providerId}/slots?from&to` → available slots.
- `POST /appointments` → book; body references `providerId`, `slotId`, `patientId`. Returns `201`
  with appointment on success; **`409 Conflict`** when the slot is no longer available or the slot
  version is stale.
- `POST /appointments/{id}/reschedule` → initiates the reschedule saga; returns the updated/created
  appointment or a conflict.
- `DELETE /appointments/{id}` → cancel; frees the slot.
- `GET /appointments/{id}/intake` → intake form (served by Intake service).

**Booking concurrency (see [ADR-0007](../adr/0007-relational-and-nosql-data-stores.md))**

- Availability modeled as discrete **slot rows**: `availability_slots(id, provider_id, start_ts,
  end_ts, status, version, appointment_id?)`.
- Booking is a single ACID transaction flipping a slot `AVAILABLE → BOOKED`, guarded by an
  **optimistic `version`** check (fast path → `409`) **and** a **unique partial index** on
  `(provider_id, start_ts) WHERE status = 'BOOKED'` (hard safety net). Correctness must not depend
  on application logic.

**Eventing (see [ADR-0003](../adr/0003-event-driven-backbone-eventbridge-sqs.md))**

- Events: `AppointmentBooked`, `AppointmentRescheduled`, `AppointmentCancelled`. Each carries the
  data consumers need (avoid sync coupling) plus a **correlation ID** and an **event ID**.
- EventBridge custom bus → content-routed to a **dedicated SQS queue per consumer**, each with a
  **DLQ**. Locally emulated by LocalStack.
- Consumers are **idempotent** — dedupe on event ID (conditional write / processed-events record).

**Reschedule saga (see [ADR-0004](../adr/0004-choreography-and-reschedule-saga.md))**

- Orchestrated (Step Functions; locally emulated or substituted with an in-process orchestrator for
  the slice): reserve new slot → on success release old; on failure/timeout **compensate** (release
  new, keep old).

**Eligibility is informational, not a gate** — failure marks the appointment `pending`; booking
still completes.

**Security in the slice (see [ADR-0008](../adr/0008-security-and-hipaa.md))**

- **Field-level envelope encryption** on at least one ultra-sensitive field (e.g. SSN / member ID),
  implemented and runnable locally.
- **PHI-redacted structured logging** with correlation ID on every line.
- Full VPC/KMS/WAF/Object-Lock topology is described in IaC + docs, not built in the local slice.

**Schema & migrations** — PostgreSQL schema applied via **Flyway** versioned migrations.

**Persistence model** — Intake uses DynamoDB single-table: `PK=APPOINTMENT#<id>`, `SK=FORM#<id>`;
`GSI1: PK=PATIENT#<id>, SK=APPOINTMENT#<ts>`; form body stored as a schemaless JSON attribute.

## Testing Decisions

**What makes a good test here:** tests assert **external behavior at the highest seam**, never
implementation details. A test books an appointment through the API and asserts the HTTP response,
the resulting persisted state, and the downstream consumer outcome — it does not assert on private
methods, internal class structure, or SQL specifics.

**Seams (confirmed scope):**

1. **HTTP API boundary (highest seam):** drive Scheduling through `POST /appointments`,
   `/reschedule`, `DELETE`, asserting status codes (incl. `409` on contention) and persisted state.
2. **Event boundary:** assert `AppointmentBooked` is emitted with the right payload/correlation ID,
   and that Intake creates exactly one form and Notification sends exactly one confirmation —
   including under **duplicate delivery** (idempotency) and **forced consumer failure** (DLQ).
3. **Contract seam:** **Pact** consumer-driven contracts for the REST endpoints and the event
   schemas; a breaking change fails the build.

**Modules tested:**

- **Scheduling** — API behavior, booking concurrency (concurrent booking of the same slot yields one
  `201` and one `409`), reschedule saga happy-path and compensation.
- **Intake** — creates exactly one form per `AppointmentBooked`; idempotent under redelivery.
- **Notification** — sends exactly one confirmation per booking; idempotent; failures land in DLQ.
- **Eligibility circuit-breaker artifact** — opens on repeated payer failure/timeout; booking flow
  remains unaffected (eligibility `pending`).

**Harness / prior art:** **Testcontainers** for real PostgreSQL; **LocalStack** for EventBridge,
SQS, DynamoDB. The same docker-compose/LocalStack harness runs locally and in CI (see
[ADR-0010](../adr/0010-iac-and-cicd.md)). Integration tests are the primary level for the event
flow; unit tests cover slot-state transitions and the circuit breaker; Pact covers contracts.

## Out of Scope

- **Patient (MPI)** and full **Eligibility** services as deployed services — described in ADRs/IaC
  only; the slice uses a stubbed patient reference and a stubbed payer.
- Real SMS/email delivery (Notification provider is stubbed locally).
- Full production AWS deployment (Fidelity C), VPC/KMS/WAF/Object-Lock provisioning end-to-end,
  multi-region DR — expressed as IaC + architecture docs, not built/run.
- Cognito-backed auth flows beyond a stubbed JWT in the local slice (the contract is defined; the
  identity provider is not stood up locally).
- AI-assisted development tooling (intentionally excluded; the narrative is set ~2021 —
  see [`docs/architecture.md` §13](../architecture.md)).
- UI / frontend — this is a backend slice; interaction is via API + tests.

## Further Notes

- This PRD specifies the **build spec** for the demonstrable slice; the surrounding system design,
  trade-offs, and rejected alternatives live in [`docs/architecture.md`](../architecture.md) and the
  ADRs and should be treated as binding context.
- Era coherence: implementation should avoid anachronisms (no Lambda SnapStart, no AI tooling) to
  keep the ~2021 narrative consistent; "present-day deltas" are documented separately.
- When derived into issues (`/to-issues`), prefer **tracer-bullet vertical slices**: the first issue
  should stand up the thinnest end-to-end path (book → event → one consumer) before broadening.
