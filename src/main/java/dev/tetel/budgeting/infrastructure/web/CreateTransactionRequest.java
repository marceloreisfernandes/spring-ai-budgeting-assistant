package dev.tetel.budgeting.infrastructure.web;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * REST request body for creating a transaction. Intentionally has no {@code source},
 * {@code ownerUsername}, or {@code requestId} field: those are always trusted, server-generated
 * values (see {@link TransactionController}), never bound from client input.
 */
record CreateTransactionRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = Transaction.MAX_DESCRIPTION_LENGTH, message = "must be at most " + Transaction.MAX_DESCRIPTION_LENGTH + " characters")
        String description,

        @Positive(message = "must be greater than zero")
        long amountInCents,

        @NotNull(message = "must not be null")
        Category category) {
}
