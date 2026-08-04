package dev.tetel.budgeting.infrastructure.persistence;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA representation of a transaction. Kept separate from the domain {@code Transaction} so that
 * persistence concerns never leak into domain or application code, and so this entity is never
 * accidentally serialized over HTTP or handed to Spring AI.
 */
@Entity
@Table(name = "transactions")
class TransactionEntity {

    @Id
    private UUID id;

    private String description;

    private long amountInCents;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private TransactionSource source;

    private String ownerUsername;

    private String requestId;

    private Instant createdAt;

    protected TransactionEntity() {
        // required by JPA
    }

    TransactionEntity(UUID id, String description, long amountInCents, Category category,
            TransactionSource source, String ownerUsername, String requestId, Instant createdAt) {
        this.id = id;
        this.description = description;
        this.amountInCents = amountInCents;
        this.category = category;
        this.source = source;
        this.ownerUsername = ownerUsername;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getDescription() {
        return description;
    }

    long getAmountInCents() {
        return amountInCents;
    }

    Category getCategory() {
        return category;
    }

    TransactionSource getSource() {
        return source;
    }

    String getOwnerUsername() {
        return ownerUsername;
    }

    String getRequestId() {
        return requestId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
