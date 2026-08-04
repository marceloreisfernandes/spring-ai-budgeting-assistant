package dev.tetel.budgeting.infrastructure.web.error;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable, safe error shape returned to every API caller. Never carries a stack trace, SQL,
 * database names, provider payloads, environment variables, or internal class names.
 */
public record ErrorResponse(Instant timestamp, String requestId, String code, String message) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(Instant.now(), UUID.randomUUID().toString(), code, message);
    }
}
