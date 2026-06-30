# ADR-0008: HIPAA-grade, defense-in-depth security posture

- **Status:** Accepted
- **Date:** 2021-04-12
- **Deciders:** Lead Engineer, Security/Compliance

## Context

The system handles PHI and is subject to HIPAA. Security and governance must be layered, auditable,
and enforced as code — not bolted on. A common failure mode (logging PHI into CloudWatch) must be
designed out.

## Decision

- **Encryption:** KMS customer-managed keys at rest (RDS, DynamoDB, S3, SQS) with rotation; TLS in
  transit everywhere; **application-level field-level envelope encryption** on ultra-sensitive
  fields (SSN, insurance member ID) so a DB read alone cannot expose them.
- **Network:** services in **private subnets**; **VPC endpoints** for AWS-service calls (no internet
  egress); public surface limited to API Gateway + WAF.
- **Identity & access:** Cognito (OIDC/JWT); **one least-privilege IAM execution role per service**
  (Scheduling cannot read Intake's table).
- **Secrets:** Secrets Manager with automatic rotation; nothing in env vars or code.
- **Audit:** CloudTrail for control/data-plane; an **append-only PHI access log** in S3 with
  **Object Lock** (immutable, tamper-evident); **PHI scrubbed from application/CloudWatch logs**
  via structured logging with redaction.
- **Governance:** BAA-eligible services only; data-classification resource tags; **AWS Config /
  policy-as-code** to detect drift (no unencrypted store, no public RDS).

## Consequences

**Positive:** layered defense; auditable, tamper-evident PHI access trail; least privilege limits
blast radius; governance enforced continuously, not by review.

**Negative / mitigations:** field-level encryption adds app complexity and is not queryable →
applied only to the few fields that need it; VPC endpoints/private networking add setup cost →
codified in Terraform.

## Alternatives considered

- **At-rest encryption + IAM only (no field-level)** — simpler and compliant for most fields, but
  field-level adds defense-in-depth for the highest-sensitivity attributes.
- **Public subnets + security groups only** — rejected: weaker isolation for a regulated workload.
