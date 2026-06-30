# ADR-0011: Observability — X-Ray tracing across async boundaries + SLOs

- **Status:** Accepted
- **Date:** 2021-05-03
- **Deciders:** Lead Engineer, Platform Team

## Context

The event-driven design's main weakness is that choreography offers no single end-to-end view of a
booking's progress ([ADR-0004](0004-choreography-and-reschedule-saga.md)). We need to follow one
request across synchronous and asynchronous hops, watch business health (not just infra), and
operate to explicit reliability targets.

## Decision

- **Tracing: AWS X-Ray** across API Gateway → Fargate → EventBridge → Lambda. Because X-Ray does not
  auto-propagate across async event boundaries, **inject trace/correlation context into EventBridge
  event metadata** and rejoin the trace in the consumer — making the choreography end-to-end traceable.
- **Logs:** CloudWatch, structured JSON, **PHI-redacted** ([ADR-0008](0008-security-and-hipaa.md)),
  correlation ID on every line.
- **Metrics:** CloudWatch dashboards including **business SLIs** — bookings/min, eligibility-failure
  rate, DLQ depth.
- **SLOs & error budgets:** define SLIs (booking-API availability & p99 latency; booked→confirmation
  time) → SLOs with error budgets → CloudWatch Alarms → SNS → PagerDuty/Slack.

## Consequences

**Positive:** end-to-end traceability across async hops (closes the choreography gap); business-level
visibility; alerting tied to user-facing objectives; DLQ-depth alarms tie observability to resilience.

**Negative / mitigations:** correlation/trace propagation must be consistently implemented in every
producer/consumer → standardized in a shared library.

## Alternatives considered

- **OpenTelemetry / ADOT** — the vendor-neutral direction, but only emerging in 2021; X-Ray chosen
  for maturity then. **Forward note:** standardize on OpenTelemetry today.
- **Logs/metrics only (no distributed tracing)** — rejected: cannot follow a request across the
  event-driven choreography.
