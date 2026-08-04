package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** Lists a single owner's transactions for one category, with a bounded result size. */
@Service
public class ListTransactionsByCategoryUseCase {

    /** Documented bound on result size for this educational project. */
    public static final int MAX_RESULTS = 100;

    private final TransactionRepository repository;

    public ListTransactionsByCategoryUseCase(TransactionRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<TransactionOutput> list(ListTransactionsByCategoryInput input) {
        return repository.findByOwnerUsernameAndCategory(input.ownerUsername(), input.category(), MAX_RESULTS)
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
