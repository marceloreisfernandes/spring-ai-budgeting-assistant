package dev.tetel.budgeting.infrastructure.persistence;

import dev.tetel.budgeting.domain.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA contract. Package-private: only {@link TransactionRepositoryAdapter} may use it. */
interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByRequestId(String requestId);

    List<TransactionEntity> findByOwnerUsernameAndCategory(String ownerUsername, Category category, Pageable pageable);
}
