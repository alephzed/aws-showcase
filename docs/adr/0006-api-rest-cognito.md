# ADR-0006: Contract-first REST over API Gateway (REST API) with Cognito

- **Status:** Accepted
- **Date:** 2021-03-29
- **Deciders:** Lead Engineer, Backend Team

## Context

Clients (patient/staff apps) and B2B/EHR partners need stable, secure service interfaces. We need a
governed contract, edge security suitable for a regulated domain, and a clear separation between
synchronous client APIs and internal async eventing.

## Decision

- **Contract-first REST** with **OpenAPI 3.1 as the source of truth** — drives request validation,
  generated client/server stubs, and reviewable, versioned contracts.
- **API Gateway REST API** flavor (not HTTP API): **AWS WAF attaches to REST APIs**, plus request
  validation against the OpenAPI schema and usage plans.
- **Amazon Cognito** user pools (OIDC/JWT) validated by an API Gateway JWT authorizer; scopes/claims
  for coarse authz, per-service checks for fine-grained PHI access; OAuth2 client-credentials for
  B2B/EHR partners.
- **Client APIs are synchronous** request/response (booking returns `201`, not `202`). "Event-driven"
  describes internal reactions only.

## Consequences

**Positive:** governed interfaces; WAF + validation at the edge; standard, cacheable, partner-friendly;
clear sync/async separation.

**Negative / mitigations:** REST API costs more and adds latency vs HTTP API → acceptable for the
regulated edge; can front with CloudFront if needed.

## Alternatives considered

- **API Gateway HTTP API** — cheaper/faster but no native WAF attachment; rejected for a HIPAA edge.
- **GraphQL / AppSync** — earns its place only with a rich aggregating frontend; this is a backend
  showcase.
- **gRPC** — great for chatty internal sync calls, but inter-service comms here are async events.
- **Federate to enterprise IdP (Okta/Entra)** — the choice if the org already runs one; Cognito
  chosen for a self-contained showcase.
