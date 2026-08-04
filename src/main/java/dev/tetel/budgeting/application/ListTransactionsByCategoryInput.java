package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Category;

/** Explicit input for {@link ListTransactionsByCategoryUseCase}. {@code ownerUsername} always comes from the authenticated principal. */
public record ListTransactionsByCategoryInput(String ownerUsername, Category category) {
}
