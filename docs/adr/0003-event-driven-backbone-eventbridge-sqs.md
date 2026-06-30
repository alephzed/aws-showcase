# ADR-0003: Event-driven backbone — EventBridge + per-consumer SQS + DLQs

- **Status:** Accepted
- **Date:** 2021-03-08
- **Deciders:** Lead Engineer, Backend Team

## Context

Decomposed services must integrate without tight synchronous coupling. We need pub/sub fan-out
(one booking triggers intake, eligibility, notification), durable delivery, independent consumer
failure isolation, governed event contracts, and ideally replay for recovery/audit.

## Decision

Use a custom **EventBridge** bus as the domain-event router, fanning out via content-based **rules**
to a **dedicated SQS queue per consumer**, each with a **dead-letter queue**.

- **EventBridge** = broker/router: pub/sub, content filtering, **Schema Registry** (versioned,
  discoverable contracts), and **archive + replay** (doubles as a HIPAA audit/recovery story).
- **SQS** = per-consumer durable buffer: independent retry, backpressure, DLQ, failure isolation.
- Consumers are **idempotent** (dedupe on event ID) because SQS is at-least-once.

## Consequences

**Positive:** loose coupling; per-consumer isolation and retry; governed schemas; replay for
recovery and audit; backpressure protects downstreams.

**Negative / mitigations:** more moving parts and eventual consistency → mitigated by correlation
IDs + tracing ([ADR-0011](0011-observability.md)) and idempotent consumers; slightly higher latency
than raw SNS → acceptable for async reactions off the synchronous booking path.

## Alternatives considered

- **SNS → SQS fan-out** — simpler, lower latency, but no content routing, schema registry, or replay.
- **Kinesis** — rejected as overkill; no stream-scale ingest here. (Right tool only if device/RPM
  telemetry were added.)
- **EventBridge direct-to-Lambda (no SQS)** — loses per-consumer durable buffering and DLQ isolation.
