package com.acme.payroll.analytics.dto;

import com.acme.payroll.employee.Department;

import java.math.BigDecimal;

public record DepartmentPayrollSummary(
        String currency,
        Department department,
        long headcount,
        BigDecimal totalSalary,
        BigDecimal averageSalary
) {
}
