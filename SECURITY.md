# Security Policy

This is an educational project (a budgeting assistant API built with Spring Boot and Spring AI)
built to practice AppSec and DevSecOps alongside the application itself. It does not claim to be
completely secure, and does not claim immunity from prompt injection - see
[`docs/security/threat-model.md`](docs/security/threat-model.md) for the explicit list of
non-claims.

## Full documentation

| Document | Covers |
|---|---|
| [`docs/security/threat-model.md`](docs/security/threat-model.md) | Assets, trust boundaries, threats mapped to controls/tests/evidence |
| [`docs/security/security-controls.md`](docs/security/security-controls.md) | Every implemented control, in detail |
| [`docs/security/prompt-injection.md`](docs/security/prompt-injection.md) | The AI-specific threat model and its 14 required mitigations |
| [`docs/security/secret-management.md`](docs/security/secret-management.md) | How `OPENAI_API_KEY` and other secrets are handled, and the leaked-key incident response process |
| [`docs/security/security-testing.md`](docs/security/security-testing.md) | How to run the test suite, local security scans, and safe fake-data learning labs |

## Supported version

This project has a single active branch (`main`); there is no historical version to maintain
security patches for.

## Reporting a vulnerability

This is a learning project without a production deployment or a user base, so there is no formal
disclosure program. If you find an issue while studying this code, please open a GitHub issue
describing it (without including any real credential, even accidentally) or reach out to the
repository owner directly for anything sensitive enough that public disclosure would be
irresponsible before a fix lands.

## What this project does not do

- It does not use a real production OpenAI key anywhere in source control, CI, or documentation.
- It does not claim the Docker image has zero vulnerabilities - see the Trivy scan results
  referenced in the project's final validation report.
- It does not claim any AI-facing control makes prompt injection impossible - only that specific,
  tested capabilities remain unavailable to the model regardless of what it is told.
