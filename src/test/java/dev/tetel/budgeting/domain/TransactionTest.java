package dev.tetel.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final String OWNER = "alice";
    private static final String REQUEST_ID = "req-1";

    @Test
    void createsATransactionWithTrustedGeneratedIdentityAndTimestamp() {
        Transaction transaction = Transaction.create(
                "Parking payment", 6000L, Category.AUTO, TransactionSource.REST, OWNER, REQUEST_ID);

        assertThat(transaction.id()).isNotNull();
        assertThat(transaction.description()).isEqualTo("Parking payment");
        assertThat(transaction.amount().cents()).isEqualTo(6000L);
        assertThat(transaction.category()).isEqualTo(Category.AUTO);
        assertThat(transaction.source()).isEqualTo(TransactionSource.REST);
        assertThat(transaction.ownerUsername()).isEqualTo(OWNER);
        assertThat(transaction.requestId()).isEqualTo(REQUEST_ID);
        assertThat(transaction.createdAt()).isNotNull();
    }

    @Test
    void trimsDescriptionWhitespace() {
        Transaction transaction = Transaction.create(
                "  Grocery run  ", 1000L, Category.GROCERIES, TransactionSource.REST, OWNER, REQUEST_ID);

        assertThat(transaction.description()).isEqualTo("Grocery run");
    }

    @Test
    void rejectsNullDescription() {
        assertThatThrownBy(() -> Transaction.create(
                null, 1000L, Category.GROCERIES, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> Transaction.create(
                "   ", 1000L, Category.GROCERIES, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDescriptionAboveMaximumLength() {
        String tooLong = "a".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> Transaction.create(
                tooLong, 1000L, Category.GROCERIES, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsDescriptionAtMaximumLength() {
        String maxLength = "a".repeat(Transaction.MAX_DESCRIPTION_LENGTH);

        Transaction transaction = Transaction.create(
                maxLength, 1000L, Category.GROCERIES, TransactionSource.REST, OWNER, REQUEST_ID);

        assertThat(transaction.description()).hasSize(Transaction.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", 0L, Category.RESTAURANT, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", -500L, Category.RESTAURANT, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAmountAboveDomainLimit() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", Money.MAX_AMOUNT_CENTS + 1, Category.RESTAURANT, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", 500L, null, TransactionSource.REST, OWNER, REQUEST_ID))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSource() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", 500L, Category.RESTAURANT, null, OWNER, REQUEST_ID))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankOwnerUsername() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", 500L, Category.RESTAURANT, TransactionSource.REST, "  ", REQUEST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankRequestId() {
        assertThatThrownBy(() -> Transaction.create(
                "Coffee", 500L, Category.RESTAURANT, TransactionSource.REST, OWNER, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstructsAnExistingTransactionWithoutRegeneratingIdentity() {
        TransactionId id = TransactionId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        Transaction transaction = Transaction.reconstruct(
                id, "Old purchase", 2500L, Category.OTHER, TransactionSource.VOICE, OWNER, REQUEST_ID, createdAt);

        assertThat(transaction.id()).isEqualTo(id);
        assertThat(transaction.createdAt()).isEqualTo(createdAt);
    }
}
