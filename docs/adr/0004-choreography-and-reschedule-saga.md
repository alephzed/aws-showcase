# ADR-0004: Choreography by default; orchestrated saga for reschedule

- **Status:** Accepted
- **Date:** 2021-03-15
- **Deciders:** Lead Engineer, Backend Team

## Context

Cross-service workflows need a coordination model. Orchestrating everything recreates a monolith in
a state machine; choreographing flows that need a timeout + rollback invites lost-update bugs. We
must choose deliberately per flow, and decide whether eligibility gates a booking.

## Decision

- The **core booking write is a single-service ACID transaction** (slot reservation in PostgreSQL),
  not a distributed transaction. No saga for the happy path.
- **Cross-service reactions are choreographed** — services react to `AppointmentBooked`
  independently with eventual consistency.
- **Eligibility is informational, not a gate:** on failure/timeout the appointment is kept and
  flagged `pending`; front-desk follows up. This is how clinics actually operate and avoids a
  distributed transaction on the booking path.
- **Reschedule is the one orchestrated saga** (AWS **Step Functions**): reserve new slot → release
  old on success; **compensate** (release new, keep old) on failure/timeout — the one flow that
  genuinely needs central coordination + rollback.

## Consequences

**Positive:** strong consistency only where required; loose coupling elsewhere; demonstrates both
patterns and the judgment of when each applies.

**Negative / mitigations:** choreography has no single end-to-end view of state → mitigated by
correlation IDs propagated through every event + EventBridge archive/replay + X-Ray
([ADR-0011](0011-observability.md)).

## Alternatives considered

- **Eligibility hard-gates booking (orchestrated saga for every booking)** — rejected: imposes an
  external slow/flaky dependency on the critical booking path and a distributed transaction where a
  status flag suffices.
- **Orchestrate all flows** — rejected: centralizes coupling, recreates the monolith.
