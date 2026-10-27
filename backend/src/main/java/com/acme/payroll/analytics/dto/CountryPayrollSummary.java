package com.acme.payroll.analytics.dto;

import java.math.BigDecimal;

public record CountryPayrollSummary(
        String currency,
        String country,
        long headcount,
        BigDecimal totalSalary,
        BigDecimal averageSalary
) {
}
