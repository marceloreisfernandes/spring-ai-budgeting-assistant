package dev.tetel.budgeting.application;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;
import dev.tetel.budgeting.testsupport.FakeTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistTransactionUseCaseTest {

    private FakeTransactionRepository repository;
    private PersistTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeTransactionRepository();
        useCase = new PersistTransactionUseCase(repository);
    }

    @Test
    void persistsANewTransactionAndReturnsASafeOutput() {
        PersistTransactionInput input = new PersistTransactionInput(
                "Parking payment", 6000L, Category.AUTO, TransactionSource.REST, "alice", "req-1");

        TransactionOutput output = useCase.persist(input);

        assertThat(output.description()).isEqualTo("Parking payment");
        assertThat(output.amount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(output.category()).isEqualTo(Category.AUTO);
        assertThat(output.source()).isEqualTo(TransactionSource.REST);
        assertThat(output.id()).isNotBlank();
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void doesNotCreateADuplicateWhenTheSameRequestIdIsPersistedTwice() {
        PersistTransactionInput input = new PersistTransactionInput(
                "Parking payment", 6000L, Category.AUTO, TransactionSource.VOICE, "alice", "req-idempotent");

        TransactionOutput first = useCase.persist(input);
        TransactionOutput second = useCase.persist(input);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidAmountsProducedByUntrustedCallers() {
        PersistTransactionInput input = new PersistTransactionInput(
                "Suspicious", -100L, Category.OTHER, TransactionSource.VOICE, "alice", "req-2");

        assertThatThrownBy(() -> useCase.persist(input))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.size()).isZero();
    }

    @Test
    void rejectsBlankDescriptionProducedByUntrustedCallers() {
        PersistTransactionInput input = new PersistTransactionInput(
                "   ", 500L, Category.OTHER, TransactionSource.VOICE, "alice", "req-3");

        assertThatThrownBy(() -> useCase.persist(input))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.size()).isZero();
    }
}
