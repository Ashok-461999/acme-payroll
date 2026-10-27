package com.acme.payroll.common;

import com.acme.payroll.common.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyRulesTest {

    @Test
    void acceptsPositiveAmountsAndScalesToTwoDecimals() {
        assertThat(MoneyRules.requireValidAmount(new BigDecimal("0.01"), "amount"))
                .isEqualByComparingTo("0.01");
        assertThat(MoneyRules.requireValidAmount(new BigDecimal("123456.7"), "amount"))
                .isEqualByComparingTo("123456.70");
        assertThat(MoneyRules.requireValidAmount(new BigDecimal("9999999999999.99"), "amount"))
                .isEqualByComparingTo("9999999999999.99");
    }

    @Test
    void rejectsMissingZeroAndNegativeAmounts() {
        assertThatThrownBy(() -> MoneyRules.requireValidAmount(null, "amount"))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> MoneyRules.requireValidAmount(BigDecimal.ZERO, "amount"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> MoneyRules.requireValidAmount(new BigDecimal("-0.01"), "amount"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsExcessPrecisionAndOversizedAmounts() {
        assertThatThrownBy(() -> MoneyRules.requireValidAmount(new BigDecimal("10.001"), "amount"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("two decimal places");
        assertThatThrownBy(() -> MoneyRules.requireValidAmount(new BigDecimal("10000000000000.00"), "amount"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("too large");
    }
}
