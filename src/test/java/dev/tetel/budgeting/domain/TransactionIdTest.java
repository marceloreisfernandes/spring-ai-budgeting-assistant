package dev.tetel.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionIdTest {

    @Test
    void generatesUniqueIdentifiers() {
        TransactionId first = TransactionId.newId();
        TransactionId second = TransactionId.newId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void wrapsAnExistingUuid() {
        UUID uuid = UUID.randomUUID();

        TransactionId id = TransactionId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullUuid() {
        assertThatThrownBy(() -> TransactionId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
