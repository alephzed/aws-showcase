# ADR-0001: Decompose the monolith into five bounded contexts

- **Status:** Accepted
- **Date:** 2021-02-15
- **Deciders:** Lead Engineer, Backend Team

## Context

The legacy JBoss monolith bundled scheduling, patient identity, intake, eligibility, and
notification behind a single shared database. These capabilities have different rates of change,
scaling profiles, and access-control needs, but the monolith forced them to deploy and scale as
one unit. We need service boundaries that enable independent deployment and scaling without
fragmenting into per-entity "nano-services."

## Decision

Decompose along **business capability + data ownership + rate of change** into **five bounded
contexts**, each owning its own datastore (**database-per-service; no shared database**):

- **Scheduling** — appointments & provider slots (PostgreSQL)
- **Patient / MPI** — demographics & identity (PostgreSQL)
- **Intake** — clinical forms (DynamoDB)
- **Eligibility** — insurance verification + anti-corruption layer (DynamoDB cache)
- **Notification** — reminders/confirmations (stateless consumer)

Cross-service data travels by event payload or a sync read API — never a shared schema.

## Consequences

**Positive:** independent deploy/scale; clear ownership; failure isolation (Notification can fail
without blocking booking); access policies scoped per PHI sensitivity.

**Negative / mitigations:** more operational surface and cross-service data flows → mitigated by
not migrating all five at once (see [ADR-0002](0002-strangler-fig-cdc-migration.md)) and by
carrying needed data in events to avoid sync coupling.

## Alternatives considered

- **Per-entity microservices** — rejected as over-decomposition (distributed monolith, chatty sync calls).
- **Keep Patient inside Scheduling** — defensible if Patient were only used by Scheduling; rejected because Patient is referenced by Intake and Eligibility too and has a distinct lifecycle/access policy.
