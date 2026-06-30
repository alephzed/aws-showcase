# ADR-0002: Migrate via strangler-fig with CDC + dual-write

- **Status:** Accepted
- **Date:** 2021-02-22
- **Deciders:** Lead Engineer, Backend Team, DBA

## Context

The system is in continuous clinical use; a big-bang rewrite/cutover is unacceptable. The hardest
problem is the **shared database** — the monolith's tables have foreign keys that cross what should
be service boundaries. We need a zero-downtime, reversible path to peel off one capability at a time.

## Decision

Apply the **strangler-fig pattern at the data layer**, one seam at a time, **Scheduling first**
(highest churn, cleanest boundary), **Patient/MPI last** (most referenced):

1. Containerize the JBoss monolith onto **Fargate** (lift-and-shift) to exit on-prem.
2. Put a **façade** (ALB / API Gateway) in front so traffic redirects per-route, invisibly to clients.
3. Stand up the new Spring Boot service with its **own** datastore.
4. **CDC + dual-write:** Debezium/AWS DMS replicate the monolith's tables into the new store and
   backfill history; new operations are **dual-written** to both during transition.
5. **Reconciliation jobs** (row counts / checksums) **prove data parity before** cutover.
6. Cut over reads, then writes, **per route behind the façade**; roll back by flipping the route.
   Decommission the seam in the monolith once traffic is 100% on the new service. Repeat.

## Consequences

**Positive:** zero downtime; every step reversible; risk taken in small increments; parity proven,
not assumed.

**Negative / mitigations:** dual-write introduces temporary write amplification and consistency
windows → bounded by reconciliation + short transition windows; CDC adds operational complexity →
isolated to the migration phase and removed when the seam is decommissioned.

## Alternatives considered

- **New service as source-of-truth from day one (ACL back to monolith)** — simpler sync, but
  requires modifying the monolith to call out and offers a weaker reconciliation story. Reserved
  for low-churn seams.
- **Big-bang cutover** — rejected: unacceptable downtime/risk for a live clinical system.
