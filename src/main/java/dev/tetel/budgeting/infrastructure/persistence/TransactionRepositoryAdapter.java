package dev.tetel.budgeting.infrastructure.persistence;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements the domain's {@link TransactionRepository} boundary using Spring Data JPA.
 * This is the only class allowed to see both the domain model and the JPA entity.
 */
@Repository
class TransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository jpaRepository;

    TransactionRepositoryAdapter(SpringDataTransactionRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = jpaRepository.save(TransactionEntityMapper.toEntity(transaction));
        return TransactionEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findByRequestId(String requestId) {
        return jpaRepository.findByRequestId(requestId).map(TransactionEntityMapper::toDomain);
    }

    @Override
    public List<Transaction> findByOwnerUsernameAndCategory(String ownerUsername, Category category, int maxResults) {
        PageRequest page = PageRequest.of(0, maxResults, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByOwnerUsernameAndCategory(ownerUsername, category, page).stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }
}
