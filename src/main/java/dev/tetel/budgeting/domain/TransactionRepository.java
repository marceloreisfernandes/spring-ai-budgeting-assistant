package dev.tetel.budgeting.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for {@link Transaction}, owned by the domain and implemented in infrastructure.
 * Exposes only the operations the current use cases need; no Spring Data types leak through here.
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findByRequestId(String requestId);

    List<Transaction> findByOwnerUsernameAndCategory(String ownerUsername, Category category, int maxResults);
}
