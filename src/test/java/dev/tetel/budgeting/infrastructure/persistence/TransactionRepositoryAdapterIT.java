package dev.tetel.budgeting.infrastructure.persistence;

import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.Transaction;
import dev.tetel.budgeting.domain.TransactionSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Docker-dependent integration test: starts a real PostgreSQL container via Testcontainers,
 * runs the Flyway migrations against it, and proves JPA mapping, database constraints, and
 * idempotency at the persistence boundary. Requires a running Docker daemon.
 */
@Testcontainers
@SpringBootTest
class TransactionRepositoryAdapterIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @Autowired
    private TransactionRepositoryAdapter repositoryAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndReloadsATransactionPreservingAllFields() {
        Transaction transaction = Transaction.create(
                "Pharmacy visit", 4599L, Category.PHARMA, TransactionSource.VOICE, "alice", "req-jpa-1");

        repositoryAdapter.save(transaction);

        Optional<Transaction> reloaded = repositoryAdapter.findByRequestId("req-jpa-1");

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().id()).isEqualTo(transaction.id());
        assertThat(reloaded.get().description()).isEqualTo("Pharmacy visit");
        assertThat(reloaded.get().amount().cents()).isEqualTo(4599L);
        assertThat(reloaded.get().category()).isEqualTo(Category.PHARMA);
        assertThat(reloaded.get().source()).isEqualTo(TransactionSource.VOICE);
        assertThat(reloaded.get().ownerUsername()).isEqualTo("alice");
    }

    @Test
    void storesTheAmountAsIntegerCentsInTheDatabase() {
        Transaction transaction = Transaction.create(
                "Integer cents check", 12533L, Category.OTHER, TransactionSource.REST, "alice", "req-jpa-cents");

        repositoryAdapter.save(transaction);

        Long storedCents = jdbcTemplate.queryForObject(
                "SELECT amount_in_cents FROM transactions WHERE request_id = ?", Long.class, "req-jpa-cents");

        assertThat(storedCents).isEqualTo(12533L);
    }

    @Test
    void storesEnumsAsReadableStrings() {
        Transaction transaction = Transaction.create(
                "Enum as string check", 100L, Category.ENTERTAINMENT, TransactionSource.VOICE, "alice", "req-jpa-enum");

        repositoryAdapter.save(transaction);

        String category = jdbcTemplate.queryForObject(
                "SELECT category FROM transactions WHERE request_id = ?", String.class, "req-jpa-enum");
        String source = jdbcTemplate.queryForObject(
                "SELECT source FROM transactions WHERE request_id = ?", String.class, "req-jpa-enum");

        assertThat(category).isEqualTo("ENTERTAINMENT");
        assertThat(source).isEqualTo("VOICE");
    }

    @Test
    void databaseRejectsNonPositiveAmountsEvenIfApplicationLogicIsBypassed() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO transactions (id, description, amount_in_cents, category, source, owner_username, request_id, created_at) " +
                        "VALUES (?, 'bad row', 0, 'OTHER', 'REST', 'alice', 'req-bad-amount', now())",
                id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateRequestIdsEvenIfApplicationLogicIsBypassed() {
        jdbcTemplate.update(
                "INSERT INTO transactions (id, description, amount_in_cents, category, source, owner_username, request_id, created_at) " +
                        "VALUES (?, 'first', 100, 'OTHER', 'REST', 'alice', 'req-duplicate', now())",
                UUID.randomUUID());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO transactions (id, description, amount_in_cents, category, source, owner_username, request_id, created_at) " +
                        "VALUES (?, 'second', 200, 'OTHER', 'REST', 'alice', 'req-duplicate', now())",
                UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersByOwnerAndCategory() {
        repositoryAdapter.save(Transaction.create("A", 100L, Category.GROCERIES, TransactionSource.REST, "carol", "req-jpa-a"));
        repositoryAdapter.save(Transaction.create("B", 200L, Category.RESTAURANT, TransactionSource.REST, "carol", "req-jpa-b"));
        repositoryAdapter.save(Transaction.create("C", 300L, Category.GROCERIES, TransactionSource.REST, "dave", "req-jpa-c"));

        List<Transaction> result = repositoryAdapter.findByOwnerUsernameAndCategory("carol", Category.GROCERIES, 100);

        assertThat(result).extracting(Transaction::description).containsExactly("A");
    }

    @Test
    @Sql(statements = "DELETE FROM transactions", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void flywayMigratedSchemaMatchesJpaMetamodel() {
        // If the JPA metamodel did not match the Flyway-migrated schema, the Spring context
        // (with spring.jpa.hibernate.ddl-auto=validate) would have failed to start for this whole class.
        assertThat(repositoryAdapter.findByOwnerUsernameAndCategory("nobody", Category.OTHER, 10)).isEmpty();
    }
}
