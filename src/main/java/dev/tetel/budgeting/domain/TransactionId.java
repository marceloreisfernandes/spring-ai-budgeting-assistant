package dev.tetel.budgeting.domain;

import java.util.Objects;
import java.util.UUID;

/** Strongly typed identifier for {@link Transaction}, avoiding a bare {@link UUID} in the domain API. */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId value must not be null");
    }

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }
}
