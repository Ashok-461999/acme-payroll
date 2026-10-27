package com.acme.payroll.analytics.dto;

import java.util.List;

public record AnalyticsSummaryResponse(
        long totalEmployees,
        List<CurrencyPayrollSummary> currencies,
        List<DepartmentPayrollSummary> departments,
        List<CountryPayrollSummary> countries
) {
}
