# syntax=docker/dockerfile:1

# --- Build stage: compiles the jar with the Gradle Wrapper. Never receives secrets: OPENAI_API_KEY,
# database credentials, and app security credentials are only ever supplied at container run time.
#
# Base image tag policy: both stages track the minor-version floating tags (21-jdk-jammy,
# 21-jre-alpine) rather than a pinned digest, and `docker build --pull` fetches the latest
# published layer on every rebuild. That alone is not sufficient, though: Eclipse Temurin does not
# republish a tag every time upstream Alpine ships a package patch, so the runtime stage below
# also runs its own `apk upgrade` against Alpine's package repository directly - confirmed (see
# docs/security/security-controls.md) to carry newer libexpat/p11-kit builds than the image tag
# had baked in, from the same v3.23 repository the image already trusts. Trivy in CI
# (docker-security.yml) is the control that catches drift if either mechanism falls behind. ---
FROM eclipse-temurin:22-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

# Tests run in CI (see .github/workflows/ci.yml), not in the image build, so this stage never
# needs Docker-in-Docker for the Testcontainers-based tests.
RUN ./gradlew clean bootJar --no-daemon -x test

# --- Runtime stage: minimal JRE, non-root user, nothing but the jar. ---
FROM eclipse-temurin:24-jre-alpine AS runtime
WORKDIR /app

# Patches OS packages from the same Alpine repository the base image already uses (no new
# package source added). Cache cleared in the same layer so no package index is left behind.
RUN apk update && apk upgrade --no-cache && rm -rf /var/cache/apk/*

RUN addgroup -S spring && adduser -S -G spring -H -h /app -s /sbin/nologin spring

COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
