# Secret Management

## What counts as a secret here

- `OPENAI_API_KEY`
- `POSTGRES_PASSWORD`
- `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD`

None of these ever has a real value committed to this repository, in any file, at any point.

## Where secrets live

Every secret is read exclusively from the environment, via Spring's `${VAR_NAME}` placeholder
syntax in `src/main/resources/application.properties`:

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY:sk-local-placeholder-not-a-real-key}
spring.datasource.password=${POSTGRES_PASSWORD:}
app.security.username=${APP_SECURITY_USERNAME:local-dev-user}
app.security.password=${APP_SECURITY_PASSWORD:local-dev-password}
```

**Why the fallback values, and why they are not a secret leak:** Spring eagerly constructs the
OpenAI client and the datasource/security beans as part of application context startup, in every
test that loads a Spring context - including tests that have nothing to do with AI, like the JPA
integration tests. Without *some* value, `${OPENAI_API_KEY}` with no default fails context startup
outright ("could not resolve placeholder"), and the standard test suite would then require a real
key to even run - the opposite of what this project's brief requires. The fallbacks above are
obviously non-functional (`sk-local-placeholder-not-a-real-key` is rejected by OpenAI itself;
`local-dev-user`/`local-dev-password` are only ever compared against what the developer put in
their own `.env`) and only ever apply when the real environment variable is absent, i.e. local
dev and CI - never a real deployment, which always sets the real variables.

- `.env` (git-ignored) holds real local values, loaded by `docker compose` / your shell.
- `.env.example` (tracked) holds only placeholders - see its content in the repository root.
- The standard build and standard test suite (`./gradlew build`, `./gradlew test`,
  `./gradlew check`) never require `OPENAI_API_KEY`, never make a live call to OpenAI, and never
  need a real `POSTGRES_PASSWORD` (Testcontainers provisions its own throwaway database).
- CI (`.github/workflows/*.yml`) never sets any of these to a real value.

## What is never done, anywhere in this project

- No API key or password in source code, `build.gradle`, `compose.yaml`, `Dockerfile`,
  `.env.example`, tests, test fixtures, GitHub Actions workflows, or this documentation.
- No API key or password in a log line, an HTTP error response, a prompt, or `ToolContext` (see
  `VoiceAssistantOrchestrationServiceTest.neverPlacesTheApiKeyInThePromptOrToolContext`).
- No live OpenAI call during normal validation of this project (see
  [`security-testing.md`](security-testing.md) for the separate, explicitly opt-in
  `openAiIntegrationTest` Gradle task).

## Secret scanning

- **CI:** `.github/workflows/gitleaks.yml` runs the official Gitleaks image
  (`zricethezav/gitleaks`, pinned by digest) against the full git history on every push and pull
  request.
- **Local:** the exact same command can be run on a developer machine - see
  [`security-testing.md`](security-testing.md) for the copy-pasteable command and its output
  against this repository.

## Incident response: what to do if a real key leaks

If `OPENAI_API_KEY` (or any other secret) is ever accidentally committed or otherwise exposed:

1. **Revoke the key immediately** in the OpenAI dashboard. Do this before anything else - a
   leaked key can be used the moment it's visible, regardless of what you do to the repository
   afterward.
2. **Create a replacement key** and store it only in your local `.env` / deployment secret store.
3. **Remove the key from the current source tree** (this step alone is necessary but not
   sufficient - see the next step).
4. **Inspect git history.** `git log -p -- <file>` and `git log -S<the-leaked-value>` across the
   whole repository. Deleting the key from the latest revision of a file does **not** remove it
   from history - anyone with clone access can still `git checkout` an earlier commit and read it.
5. **Clean history when required** (e.g. `git filter-repo` or the BFG Repo-Cleaner), then force-push
   the rewritten history and have every collaborator re-clone. Treat this as a destructive,
   coordinated operation, not something to run unilaterally on a shared branch.
6. **Audit provider usage** for the compromised key's active window (OpenAI's usage dashboard) for
   unexpected spend or activity.
7. **Rotate related credentials** if there's any reason to believe the exposure wasn't isolated
   (e.g. the same commit also touched database credentials).

This project does not claim that step 3 alone is sufficient - it explicitly is not.

## Safe learning lab: fake secret detection

See [`security-testing.md`](security-testing.md) for a walkthrough that plants a **fake** OpenAI-
looking key in a scratch file, confirms Gitleaks flags it, and then removes it - without ever
touching a real credential.
