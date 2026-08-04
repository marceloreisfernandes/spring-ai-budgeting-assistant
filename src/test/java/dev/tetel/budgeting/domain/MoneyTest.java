package dev.tetel.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({
            "8000, 80.00",
            "12533, 125.33",
            "1, 0.01"
    })
    void convertsCentsToDecimalWithTwoDecimalPlaces(long cents, String expectedDecimal) {
        Money money = Money.ofCents(cents);

        assertThat(money.toDecimal()).isEqualByComparingTo(new BigDecimal(expectedDecimal));
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Money.ofCents(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.ofCents(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAmountAboveDomainLimit() {
        assertThatThrownBy(() -> Money.ofCents(Money.MAX_AMOUNT_CENTS + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAmountAtDomainLimit() {
        Money money = Money.ofCents(Money.MAX_AMOUNT_CENTS);

        assertThat(money.cents()).isEqualTo(Money.MAX_AMOUNT_CENTS);
    }
}
