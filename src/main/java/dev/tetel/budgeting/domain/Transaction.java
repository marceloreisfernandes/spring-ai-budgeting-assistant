package dev.tetel.budgeting.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A single financial movement recorded by the user, via REST or the voice assistant.
 * This is a class rather than a record because a transaction has identity: two transactions
 * with identical field values are still different transactions.
 */
public final class Transaction {

    /** Documented domain upper limit for descriptions in this educational project. */
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    private final TransactionId id;
    private final String description;
    private final Money amount;
    private final Category category;
    private final TransactionSource source;
    private final String ownerUsername;
    private final String requestId;
    private final Instant createdAt;

    private Transaction(TransactionId id, String description, Money amount, Category category,
            TransactionSource source, String ownerUsername, String requestId, Instant createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.source = source;
        this.ownerUsername = ownerUsername;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    /** Creates a brand-new transaction. Identity and timestamp are generated here, by trusted code. */
    public static Transaction create(String description, long amountInCents, Category category,
            TransactionSource source, String ownerUsername, String requestId) {
        return new Transaction(
                TransactionId.newId(),
                normalizeDescription(description),
                Money.ofCents(amountInCents),
                Objects.requireNonNull(category, "Category must not be null"),
                Objects.requireNonNull(source, "TransactionSource must not be null"),
                requireNonBlank(ownerUsername, "Owner username must not be blank"),
                requireNonBlank(requestId, "Request id must not be blank"),
                Instant.now());
    }

    /** Rebuilds a transaction that already exists (e.g. loaded from persistence), preserving its identity. */
    public static Transaction reconstruct(TransactionId id, String description, long amountInCents,
            Category category, TransactionSource source, String ownerUsername, String requestId, Instant createdAt) {
        return new Transaction(
                Objects.requireNonNull(id, "TransactionId must not be null"),
                normalizeDescription(description),
                Money.ofCents(amountInCents),
                Objects.requireNonNull(category, "Category must not be null"),
                Objects.requireNonNull(source, "TransactionSource must not be null"),
                requireNonBlank(ownerUsername, "Owner username must not be blank"),
                requireNonBlank(requestId, "Request id must not be blank"),
                Objects.requireNonNull(createdAt, "createdAt must not be null"));
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public TransactionId id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public Category category() {
        return category;
    }

    public TransactionSource source() {
        return source;
    }

    public String ownerUsername() {
        return ownerUsername;
    }

    public String requestId() {
        return requestId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaction other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
