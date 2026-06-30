# ADR-0010: Terraform IaC + Maven + GitHub Actions with blue/green deploys

- **Status:** Accepted
- **Date:** 2021-04-26
- **Deciders:** Lead Engineer, Platform Team

## Context

Independent per-service delivery requires reproducible infrastructure, a quality/security gate that
applies to every change, safe deploys with fast rollback, and schema-change governance. The stack is
Java/Spring Boot.

## Decision

- **IaC: Terraform**, **directory/module-per-environment** (real prod isolation, separate state in
  S3 + DynamoDB lock table). Chosen over CDK for ubiquity and multi-cloud portability.
- **Build: Maven** (period- and enterprise-accurate for Spring Boot; widely readable).
- **CI: GitHub Actions + OIDC** federation into AWS (no long-lived keys). Per-service pipeline:
  unit (JUnit5/Mockito/JaCoCo) → static analysis (SpotBugs/Sonar) → **security scans** (SAST,
  OWASP Dependency-Check, Trivy image scan, tfsec) → build image → ECR → `terraform plan` + **policy
  gate** (OPA/Conftest) → **integration tests (Testcontainers + LocalStack)** → **Pact contract
  tests** (gate breaking REST + EventBridge schema changes) → staging smoke → prod (manual approval).
- **DB migrations: Flyway**, versioned, run as a pipeline step.
- **Deploys: blue/green via ECS + CodeDeploy** (traffic shifts at ALB target groups; instant rollback).

## Consequences

**Positive:** reproducible infra; AI/human/any change passes the same gates; zero-downtime deploys
with instant rollback; contract tests prevent inter-service breakage; the Testcontainers/LocalStack
harness is shared by local dev and CI.

**Negative / mitigations:** Terraform separates infra from app language (no CDK type-sharing) →
accepted for ubiquity; blue/green needs a second task set briefly → cost bounded to deploy window.

## Alternatives considered

- **AWS CDK** — typed, testable, co-located infra; rejected here in favor of Terraform's ubiquity/portability.
- **Terraform workspaces for envs** — rejected: weaker prod isolation than directory-per-env.
- **Rolling deploys** — simpler, but slower rollback than blue/green.
- **Jenkins / CodePipeline** — the likely 2021 enterprise choice; GitHub Actions used for the
  portable showcase artifact.
