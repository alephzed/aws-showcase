# ADR-0007: PostgreSQL for transactional core; DynamoDB single-table for intake

- **Status:** Accepted
- **Date:** 2021-04-05
- **Deciders:** Lead Engineer, Backend Team, DBA

## Context

Different contexts have different data shapes and consistency needs. Scheduling must prevent
double-booking under concurrency (strong consistency). Intake forms vary by specialty (flexible
schema, bursty, read-light). Database-per-service is already established ([ADR-0001](0001-bounded-context-decomposition.md)).

## Decision

- **Scheduling → PostgreSQL (RDS Multi-AZ).** Availability modeled as discrete **slot rows**.
  Double-booking prevented by **optimistic `version`** (fast path → `409 Conflict`) **plus a unique
  partial index** on `(provider_id, start_ts) WHERE status='BOOKED'` as the hard safety net — so
  correctness never depends on application logic.
- **Intake → DynamoDB, single-table design**, modeled **from access patterns first**:
  - `PK = APPOINTMENT#<id>`, `SK = FORM#<id>`; `GSI1: PK = PATIENT#<id>, SK = APPOINTMENT#<ts>`.
  - The variable, specialty-specific form is stored as a schemaless JSON attribute — the explicit
    reason it is NoSQL, while the keys remain rigidly access-pattern-shaped.

## Consequences

**Positive:** ACID exactly where it matters; flexible schema and independent scaling for intake;
each store fits its access pattern.

**Negative / mitigations:** optimistic concurrency surfaces `409`s under contention → clients retry;
single-table design has a learning curve → documented access patterns and key schema.

## Alternatives considered

- **Pessimistic `SELECT … FOR UPDATE`** for booking — defensible for hot slots, but hurts throughput
  and risks lock waits; optimistic + unique constraint preferred for typical contention.
- **DynamoDB multi-table** — simpler to reason about, but single-table demonstrates and exploits
  key-overloading + GSI design for the known access patterns.
- **Putting intake in PostgreSQL** — rejected: rigid schema is a poor fit for per-specialty form variation.
