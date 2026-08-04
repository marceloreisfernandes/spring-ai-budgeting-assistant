package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Persists a transaction coming from either the REST API or the voice assistant tool adapter.
 * Idempotent by {@code requestId}: replaying the same request id never creates a duplicate row.
 */
@Service
public class PersistTransactionUseCase {

    private final TransactionRepository repository;

    public PersistTransactionUseCase(TransactionRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public TransactionOutput persist(PersistTransactionInput input) {
        return repository.findByRequestId(input.requestId())
                .map(TransactionOutput::from)
                .orElseGet(() -> {
                    Transaction transaction = Transaction.create(
                            input.description(),
                            input.amountInCents(),
                            input.category(),
                            input.source(),
                            input.ownerUsername(),
                            input.requestId());
                    return TransactionOutput.from(repository.save(transaction));
                });
    }
}
