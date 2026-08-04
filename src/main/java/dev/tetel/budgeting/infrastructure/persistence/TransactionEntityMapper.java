package dev.tetel.budgeting.infrastructure.persistence;

import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionId;

/** Explicit, boring mapping between the domain {@link Transaction} and its {@link TransactionEntity}. */
final class TransactionEntityMapper {

    private TransactionEntityMapper() {
    }

    static TransactionEntity toEntity(Transaction transaction) {
        return new TransactionEntity(
                transaction.id().value(),
                transaction.description(),
                transaction.amount().cents(),
                transaction.category(),
                transaction.source(),
                transaction.ownerUsername(),
                transaction.requestId(),
                transaction.createdAt());
    }

    static Transaction toDomain(TransactionEntity entity) {
        return Transaction.reconstruct(
                TransactionId.of(entity.getId()),
                entity.getDescription(),
                entity.getAmountInCents(),
                entity.getCategory(),
                entity.getSource(),
                entity.getOwnerUsername(),
                entity.getRequestId(),
                entity.getCreatedAt());
    }
}
