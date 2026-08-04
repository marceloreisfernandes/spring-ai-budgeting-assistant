package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionSource;

import java.math.BigDecimal;
import java.time.Instant;

/** Safe, minimal projection of a {@link Transaction}. Never exposes the JPA entity or domain object directly. */
public record TransactionOutput(
        String id,
        String description,
        BigDecimal amount,
        Category category,
        TransactionSource source,
        Instant createdAt) {

    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.id().value().toString(),
                transaction.description(),
                transaction.amount().toDecimal(),
                transaction.category(),
                transaction.source(),
                transaction.createdAt());
    }
}
