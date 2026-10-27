package com.acme.payroll.common;

import com.acme.payroll.common.exception.BusinessValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralises the monetary rules so employee creation and salary changes cannot
 * accidentally accept different amounts.
 */
public final class MoneyRules {

    private static final int MAX_INTEGER_DIGITS = 13;
    private static final int SCALE = 2;

    private MoneyRules() {
    }

    public static BigDecimal requireValidAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new BusinessValidationException(fieldName, "must not be null");
        }
        if (amount.signum() <= 0) {
            throw new BusinessValidationException(fieldName, "must be greater than zero");
        }
        if (amount.scale() > SCALE) {
            throw new BusinessValidationException(fieldName, "must have at most two decimal places");
        }
        if (amount.precision() - amount.scale() > MAX_INTEGER_DIGITS) {
            throw new BusinessValidationException(fieldName, "is too large");
        }
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
