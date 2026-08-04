package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionSource;
import dev.tetel.budgeting.testsupport.FakeTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListTransactionsByCategoryUseCaseTest {

    private FakeTransactionRepository repository;
    private ListTransactionsByCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeTransactionRepository();
        useCase = new ListTransactionsByCategoryUseCase(repository);
    }

    @Test
    void returnsOnlyTransactionsMatchingOwnerAndCategory() {
        repository.save(Transaction.create("Groceries", 1000L, Category.GROCERIES, TransactionSource.REST, "alice", "r1"));
        repository.save(Transaction.create("Movie", 2000L, Category.ENTERTAINMENT, TransactionSource.REST, "alice", "r2"));

        List<TransactionOutput> result = useCase.list(new ListTransactionsByCategoryInput("alice", Category.GROCERIES));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("Groceries");
    }

    @Test
    void returnsEmptyListWhenNoTransactionMatches() {
        List<TransactionOutput> result = useCase.list(new ListTransactionsByCategoryInput("alice", Category.PHARMA));

        assertThat(result).isEmpty();
    }

    @Test
    void neverReturnsAnotherOwnersTransactions() {
        repository.save(Transaction.create("Bob's secret", 1000L, Category.OTHER, TransactionSource.REST, "bob", "r3"));

        List<TransactionOutput> result = useCase.list(new ListTransactionsByCategoryInput("alice", Category.OTHER));

        assertThat(result).isEmpty();
    }

    @Test
    void resultSizeIsBounded() {
        for (int i = 0; i < ListTransactionsByCategoryUseCase.MAX_RESULTS + 10; i++) {
            repository.save(Transaction.create("item " + i, 100L, Category.OTHER, TransactionSource.REST, "alice", "r" + i));
        }

        List<TransactionOutput> result = useCase.list(new ListTransactionsByCategoryInput("alice", Category.OTHER));

        assertThat(result).hasSize(ListTransactionsByCategoryUseCase.MAX_RESULTS);
    }
}
