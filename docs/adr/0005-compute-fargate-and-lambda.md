# ADR-0005: Spring Boot on ECS Fargate; Lambda for event glue

- **Status:** Accepted
- **Date:** 2021-03-22
- **Deciders:** Lead Engineer, Platform Team

## Context

Target framework is **Spring Boot** (the modernization destination from JBoss). We need a compute
model for the services that is operationally lean, scales independently, and — given the 2021
timeline — handles Java startup characteristics well. (Lambda **SnapStart** does not yet exist; it
launches Nov 2022.)

## Decision

- **Spring Boot services run on ECS Fargate** (containers behind an ALB) as the primary compute.
  This is also the migration target: the JBoss monolith is containerized onto Fargate first, then
  strangled into Spring Boot service containers on the **same** platform — one compute story.
- **Lambda is used for lightweight event-driven glue only** (e.g. Notification fan-out,
  S3-triggered tasks) — idiomatic in 2021 and avoiding multi-second Java cold starts on the
  request path.
- **RDS Proxy** fronts PostgreSQL to pool connections from elastic compute.

## Consequences

**Positive:** no Java cold-start problem on the request path; seamless migration (monolith and
services share Fargate); independent service scaling; RDS Proxy prevents connection exhaustion.

**Negative / mitigations:** containers carry more ops than pure serverless → mitigated by Fargate
(no node management) and blue/green via CodeDeploy ([ADR-0010](0010-iac-and-cicd.md)).

## Alternatives considered

- **Lambda-first for all services** — rejected for 2021: Spring Boot cold starts were painful and
  SnapStart did not exist. (Forward note: revisit today, SnapStart changes the calculus.)
- **EKS / Kubernetes** — rejected as over-engineering at this scale; justified only with existing
  k8s investment or a multi-cloud portability mandate.
