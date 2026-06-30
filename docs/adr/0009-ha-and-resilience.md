# ADR-0009: High availability and resilience patterns

- **Status:** Accepted
- **Date:** 2021-04-19
- **Deciders:** Lead Engineer, Platform Team

## Context

The booking path must stay available during AZ failures and degrade gracefully when the external
payer is down. Elastic compute must not overwhelm the relational database. Multi-region is not
warranted at current scale but DR targets must be stated.

## Decision

- **Multi-AZ baseline:** RDS Multi-AZ (synchronous standby, automatic failover); DynamoDB, Lambda,
  SQS, EventBridge multi-AZ by design; subnets span ≥2 AZs.
- **Resilience patterns:** DLQs on every queue; **exponential backoff with jitter**; **idempotent
  consumers** (safe at-least-once retries); async **failure isolation** (Notification/Intake can be
  down without blocking booking).
- **Backpressure:** **Lambda reserved concurrency** caps protect PostgreSQL — Lambda scales
  infinitely, the database does not; SQS absorbs bursts, consumers drain at a safe rate.
- **Graceful degradation:** **circuit breaker + timeout + bounded retry** on the Eligibility ACL.
  Payer down → breaker opens → eligibility `pending` → **booking still completes** (ties to the
  informational-eligibility decision, [ADR-0004](0004-choreography-and-reschedule-saga.md)).
- **DR (described, not built):** **RTO 1h / RPO 5min** via DynamoDB global tables, RDS cross-region
  read replica / snapshots, Route 53 health-check failover, region-redeployable Terraform.

## Consequences

**Positive:** survives AZ loss; bounded blast radius for dependency failures; database protected
from traffic spikes; explicit, testable DR targets.

**Negative / mitigations:** reserved concurrency caps throughput ceilings → sized against DB
capacity; circuit breaker adds state → standard library (e.g. Resilience4j).

## Alternatives considered

- **Multi-region active-active** — rejected as over-engineering at current scale; documented as the
  next step with stated RTO/RPO.
- **No circuit breaker (rely on timeouts)** — rejected: a slow payer would exhaust threads/connections
  and degrade the booking path.
