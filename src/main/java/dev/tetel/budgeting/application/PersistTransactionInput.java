package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;

/**
 * Explicit input for {@link PersistTransactionUseCase}. {@code ownerUsername} and {@code requestId}
 * must always be supplied by trusted server code (the authenticated principal and a server-generated
 * request id), never by the REST client body or by the LLM.
 */
public record PersistTransactionInput(
        String description,
        long amountInCents,
        Category category,
        TransactionSource source,
        String ownerUsername,
        String requestId) {
}
