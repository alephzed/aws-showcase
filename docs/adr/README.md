# Architecture Decision Records

These ADRs capture the significant decisions behind the Patient Scheduling & Intake
modernization (legacy JBoss monolith → Spring Boot microservices on AWS, ~2020–2021).

> **Note.** This repository documents a reconstruction of that modernization as a reference /
> portfolio artifact. Dates reflect the original engagement's timeline so the decisions read in
> the context (and platform capabilities) of when they were made.

Format: lightweight [MADR](https://adr.github.io/madr/) — Status, Context, Decision,
Consequences, Alternatives.

| # | Title | Status |
|---|---|---|
| [0001](0001-bounded-context-decomposition.md) | Bounded-context decomposition (5 contexts) | Accepted |
| [0002](0002-strangler-fig-cdc-migration.md) | Strangler-fig migration with CDC + dual-write | Accepted |
| [0003](0003-event-driven-backbone-eventbridge-sqs.md) | Event-driven backbone: EventBridge + SQS + DLQs | Accepted |
| [0004](0004-choreography-and-reschedule-saga.md) | Choreography default + Step Functions reschedule saga | Accepted |
| [0005](0005-compute-fargate-and-lambda.md) | Compute: Spring Boot on Fargate, Lambda for event glue | Accepted |
| [0006](0006-api-rest-cognito.md) | API: contract-first REST, API Gateway REST, Cognito | Accepted |
| [0007](0007-relational-and-nosql-data-stores.md) | Data: PostgreSQL + DynamoDB single-table | Accepted |
| [0008](0008-security-and-hipaa.md) | Security & HIPAA posture | Accepted |
| [0009](0009-ha-and-resilience.md) | HA & resilience | Accepted |
| [0010](0010-iac-and-cicd.md) | IaC & CI/CD: Terraform, Maven, GitHub Actions, blue/green | Accepted |
| [0011](0011-observability.md) | Observability: X-Ray + SLOs | Accepted |
