package dev.tetel.budgeting.infrastructure.ai;

import dev.tetel.budgeting.application.ListTransactionsByCategoryUseCase;
import dev.tetel.budgeting.application.PersistTransactionUseCase;
import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionSource;
import dev.tetel.budgeting.testsupport.FakeTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the only class the LLM's tool calls can reach. Every test here proves a
 * prompt-injection control from docs/security/prompt-injection.md: the model can supply
 * description/amount/category, but never identity or the request id (those come from
 * {@link ToolContext}, populated only by trusted server code), invalid arguments are rejected
 * without throwing back into the model, and duplicate calls with the same server-generated
 * request id never duplicate a transaction.
 */
class TransactionToolsTest {

    private FakeTransactionRepository repository;
    private TransactionTools tools;

    @BeforeEach
    void setUp() {
        repository = new FakeTransactionRepository();
        tools = new TransactionTools(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository));
    }

    private static ToolContext contextFor(String owner, String requestId) {
        return new ToolContext(Map.of("ownerUsername", owner, "requestId", requestId));
    }

    @Test
    void persistsATransactionUsingTrustedContextForIdentity() {
        String result = tools.persistTransaction("Coffee", 500L, "RESTAURANT", contextFor("alice", "req-tool-1"));

        assertThat(result).contains("Coffee");
        assertThat(repository.size()).isEqualTo(1);
        assertThat(repository.findByRequestId("req-tool-1")).isPresent();
        assertThat(repository.findByRequestId("req-tool-1").get().ownerUsername()).isEqualTo("alice");
        assertThat(repository.findByRequestId("req-tool-1").get().source()).isEqualTo(TransactionSource.VOICE);
    }

    @Test
    void duplicateToolCallsWithTheSameServerGeneratedRequestIdNeverDuplicateData() {
        tools.persistTransaction("Coffee", 500L, "RESTAURANT", contextFor("alice", "req-tool-dup"));
        tools.persistTransaction("Coffee", 500L, "RESTAURANT", contextFor("alice", "req-tool-dup"));

        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void rejectsAnUnknownCategoryWithoutThrowingBackIntoTheModel() {
        String result = tools.persistTransaction("Coffee", 500L, "NOT_A_REAL_CATEGORY", contextFor("alice", "req-tool-2"));

        assertThat(result).containsIgnoringCase("unknown category");
        assertThat(repository.size()).isZero();
    }

    @Test
    void rejectsAnInvalidAmountWithoutThrowingBackIntoTheModel() {
        String result = tools.persistTransaction("Coffee", -500L, "RESTAURANT", contextFor("alice", "req-tool-3"));

        assertThat(result).containsIgnoringCase("could not");
        assertThat(repository.size()).isZero();
    }

    @Test
    void rejectsABlankDescriptionWithoutThrowingBackIntoTheModel() {
        String result = tools.persistTransaction("   ", 500L, "RESTAURANT", contextFor("alice", "req-tool-blank"));

        assertThat(result).containsIgnoringCase("could not");
        assertThat(repository.size()).isZero();
    }

    @Test
    void listsOnlyTheAuthenticatedOwnersTransactions() {
        repository.save(Transaction.create("Bob's item", 100L, Category.OTHER, TransactionSource.REST, "bob", "r1"));
        repository.save(Transaction.create("Alice's item", 200L, Category.OTHER, TransactionSource.VOICE, "alice", "r2"));

        String result = tools.listTransactionsByCategory("OTHER", contextFor("alice", "req-tool-4"));

        assertThat(result).contains("Alice's item");
        assertThat(result).doesNotContain("Bob's item");
    }

    @Test
    void returnsASafeMessageWhenNoTransactionsMatch() {
        String result = tools.listTransactionsByCategory("PHARMA", contextFor("alice", "req-tool-5"));

        assertThat(result).containsIgnoringCase("no transactions");
    }

    @Test
    void rejectsAnUnknownCategoryWhenListing() {
        String result = tools.listTransactionsByCategory("NOT_A_REAL_CATEGORY", contextFor("alice", "req-tool-6"));

        assertThat(result).containsIgnoringCase("unknown category");
    }
}
