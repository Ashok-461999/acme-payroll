package com.acme.payroll.analytics.dto;

import java.math.BigDecimal;

public record CurrencyPayrollSummary(
        String currency,
        long headcount,
        BigDecimal totalSalary,
        BigDecimal averageSalary,
        BigDecimal minSalary,
        BigDecimal maxSalary
) {
}
