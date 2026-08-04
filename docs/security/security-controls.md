# Security Controls

Control-by-control breakdown. See [`threat-model.md`](threat-model.md) for the attacker's-eye
view and [`prompt-injection.md`](prompt-injection.md) for the AI-specific controls in depth.

## Authentication and session handling

- **Control:** stateless HTTP Basic authentication (`infrastructure/security/SecurityConfig.java`)
  in front of every endpoint. Credentials come from `APP_SECURITY_USERNAME`/`APP_SECURITY_PASSWORD`
  (environment-backed, no default in real deployments beyond a local-only placeholder - see
  [`secret-management.md`](secret-management.md)). Session creation policy is `STATELESS`; no
  cookies, no server-side session state.
- **Why HTTP Basic, and why this is a real limitation:** Basic sends `username:password`
  base64-encoded on every request - not encrypted, just encoded. This is acceptable for
  `localhost` development and is what this project's brief asks for as the educational baseline,
  but **outside localhost this must run behind TLS/HTTPS**, or the credentials are exposed in
  transit to anyone who can observe the connection.
- **Test:** `TransactionControllerTest`, `AssistantControllerTest` (401 without credentials, 2xx
  with correct ones).

## CSRF

- **Decision:** disabled (`http.csrf(AbstractHttpConfigurer::disable)`).
- **Why this is safe here:** CSRF attacks work by getting a victim's browser to replay an
  *ambient* credential - a session cookie the browser attaches automatically - to a state-changing
  endpoint on a site the victim is authenticated to. This API is stateless and uses HTTP Basic, not
  cookies: nothing is attached to a request automatically. A forged cross-site request has no way
  to include a valid `Authorization: Basic ...` header, because the victim's browser doesn't store
  or auto-attach one. If this project ever added cookie/session-based auth or a browser UI, this
  decision would need to be revisited.

## CORS

- **Decision:** no CORS configuration is added; Spring's default (no
  `Access-Control-Allow-Origin`) applies. Browsers will block cross-origin JavaScript calls to
  this API from other sites by default. There is no permissive `*` origin anywhere in this codebase.

## Input validation

Two layers, deliberately redundant:

1. **REST DTO validation** (`CreateTransactionRequest`): Jakarta Bean Validation annotations
   (`@NotBlank`, `@Positive`, `@Size`, `@NotNull`) give a fast, cheap 400 for obviously bad input
   before touching the application layer.
2. **Domain construction** (`Transaction.create(...)`, `Money.ofCents(...)`): the actual source of
   truth. Both the REST controller and the AI tool adapter (`TransactionTools`) end up here, so
   the same rules apply regardless of where the data came from - a human filling a form, or a model
   proposing tool arguments.

Money is never a `double`/`float`; it is `long amountInCents`, converted to `BigDecimal` only for
display via `BigDecimal.valueOf(cents, 2)` - see `MoneyTest` for the exact conversions this proves
(8000 → 80.00, 12533 → 125.33, 1 → 0.01) and for the zero/negative/above-limit rejection cases.

## Audio upload validation

`AudioUploadValidator` (see [`security-testing.md`](security-testing.md) for how to reproduce this
locally) rejects, before any OpenAI call:

- missing or empty file
- file above 5 MB (`AudioUploadValidator.MAX_AUDIO_SIZE_BYTES`)
- extension outside `mp3`/`wav`/`m4a`
- declared content type outside the matching allowlist
- a filename that looks like a path (`/`, `\`, `..`)

Neither the extension nor the content type is trusted alone - both must be on the allowlist. The
original filename is only ever used to read its extension; it is never used to build a filesystem
path, and the uploaded bytes are never written to disk (they go straight from the multipart
request to a `Resource` handed to `TranscriptionModel`).

`spring.servlet.multipart.max-file-size=5MB` / `max-request-size=6MB` add a second, container-level
limit on top of the application-level check.

## Idempotency / duplicate prevention

See [`threat-model.md`](threat-model.md), threat 4. Two independent layers: an application-level
check (`PersistTransactionUseCase` looks up `requestId` before inserting) and a database `UNIQUE`
constraint on `transactions.request_id` (`V1__create_transactions_table.sql`). The `requestId` is
always server-generated (`UUID.randomUUID()`), never accepted from the REST body or from a model
tool argument.

## Safe error handling

`GlobalExceptionHandler` (`infrastructure/web/error/`) is the single place every exception becomes
an `ErrorResponse { timestamp, requestId, code, message }`. It never serializes a stack trace, SQL,
a database name, a provider payload, an environment variable, or an internal class name into an
HTTP response. Full exceptions are logged server-side (`log.error(..., ex)`) for debugging, never
sent to the client. `TransactionControllerTest.errorResponseNeverLeaksInternalDetails` asserts the
response body has no `stackTrace`/`exception`/`trace` field.

## Logging

- `logging.level.org.springframework.ai`, `com.openai`, `org.springframework.web.client`, and
  `org.apache.http` are all pinned to `INFO` - never `DEBUG`/`TRACE` by default, which is what
  would otherwise leak full HTTP bodies (including the Authorization header and provider payloads)
  into the logs.
- Application logs (`VoiceAssistantOrchestrationService`) log `operation`, `outcome`,
  `ownerUsername`, and `requestId` - never the transcript, never the reply text, never a financial
  amount or description.
- Nothing in this codebase logs `OPENAI_API_KEY`, `POSTGRES_PASSWORD`, or an `Authorization`
  header value.

## Database privilege separation

The official `postgres` Docker image always grants `POSTGRES_USER` the `SUPERUSER` attribute
during bootstrap (it needs it to run `initdb`). `docker/postgres/init/01-restrict-app-role.sh`
runs once, immediately after bootstrap, and does:

```sql
ALTER ROLE "budgeting_app" NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
```

`budgeting_app` still owns the `budgeting` database and everything Flyway creates in it, so this
has no functional effect on the application - only a strictly smaller privilege set for every
connection made after this script runs. Verified manually with `\du` inside the running container
(see the final report for the transcript).

## Non-root container

`Dockerfile` is a two-stage build: `eclipse-temurin:21-jdk-jammy` compiles the jar,
`eclipse-temurin:21-jre-alpine` runs it as a dedicated `spring` user/group created with
`addgroup -S` / `adduser -S`, never as root. Verified with `docker run --rm --entrypoint id
<image>` → `uid=100(spring) gid=101(spring)` (see the final report).

## CI/CD pipeline security

- **Least privilege:** every workflow starts from `permissions: contents: read`; only
  `codeql.yml` additionally needs `security-events: write` to upload its SARIF results.
- **Pinned dependencies, not floating tags:** every GitHub Action is pinned to a full commit SHA
  (with the human version as a trailing comment); the Gitleaks and Trivy scanners run as official
  Docker images pinned by digest instead of the corresponding marketplace Actions. See threat 6 in
  [`threat-model.md`](threat-model.md) for why (the March 2026 `aquasecurity/trivy-action`
  supply-chain compromise, researched while building this project).
- **No live OpenAI calls, no production secrets:** `.github/workflows/ci.yml` never sets
  `OPENAI_API_KEY` or a real `POSTGRES_PASSWORD` - the standard test suite does not need them
  (see [`secret-management.md`](secret-management.md)).
- **What CI checks:** `ci.yml` (build + full test suite, including Testcontainers/PostgreSQL),
  `gitleaks.yml` (secret scanning), `codeql.yml` (static analysis for Java), `dependency-review.yml`
  (fails a PR that introduces a high-severity vulnerable dependency), `docker-security.yml` (builds
  the image, scans it with Trivy, generates a CycloneDX SBOM as a build artifact).
- **No unexplained `continue-on-error`:** none is used anywhere in this pipeline. A failing
  Gitleaks/CodeQL/Trivy/test step fails the workflow.

## Non-claims

This project does not claim to be completely secure, does not claim the container image has zero
vulnerabilities (Trivy findings are tracked, not hidden - see the final report for the actual scan
output), and does not claim immunity from prompt injection. See
[`threat-model.md`](threat-model.md) for the explicit non-claims list.
