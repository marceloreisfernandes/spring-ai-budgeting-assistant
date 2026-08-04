package dev.tetel.budgeting.testsupport;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link TransactionRepository} test double, used so application-layer unit tests run
 * without Spring, JPA, Docker, or PostgreSQL.
 */
public class FakeTransactionRepository implements TransactionRepository {

    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findByRequestId(String requestId) {
        return transactions.stream()
                .filter(t -> t.requestId().equals(requestId))
                .findFirst();
    }

    @Override
    public List<Transaction> findByOwnerUsernameAndCategory(String ownerUsername, Category category, int maxResults) {
        return transactions.stream()
                .filter(t -> t.ownerUsername().equals(ownerUsername))
                .filter(t -> t.category() == category)
                .limit(maxResults)
                .toList();
    }

    public int size() {
        return transactions.size();
    }
}
