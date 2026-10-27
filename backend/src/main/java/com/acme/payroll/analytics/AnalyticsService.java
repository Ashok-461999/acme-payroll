package com.acme.payroll.analytics;

import com.acme.payroll.analytics.dto.AnalyticsSummaryResponse;
import com.acme.payroll.analytics.dto.CountryPayrollSummary;
import com.acme.payroll.analytics.dto.CurrencyPayrollSummary;
import com.acme.payroll.analytics.dto.DepartmentPayrollSummary;
import com.acme.payroll.employee.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalyticsService {

    private final EmployeeRepository employeeRepository;

    public AnalyticsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary() {
        List<CurrencyPayrollSummary> currencies = employeeRepository.summarizeByCurrency().stream()
                .map(row -> new CurrencyPayrollSummary(
                        row.getCurrency(),
                        row.getHeadcount(),
                        row.getTotalSalary(),
                        average(row.getTotalSalary(), row.getHeadcount()),
                        row.getMinSalary(),
                        row.getMaxSalary()
                ))
                .toList();

        List<DepartmentPayrollSummary> departments = employeeRepository
                .summarizeByCurrencyAndDepartment().stream()
                .map(row -> new DepartmentPayrollSummary(
                        row.getCurrency(),
                        row.getDepartment(),
                        row.getHeadcount(),
                        row.getTotalSalary(),
                        average(row.getTotalSalary(), row.getHeadcount())
                ))
                .toList();

        List<CountryPayrollSummary> countries = employeeRepository
                .summarizeByCurrencyAndCountry().stream()
                .map(row -> new CountryPayrollSummary(
                        row.getCurrency(),
                        row.getCountry(),
                        row.getHeadcount(),
                        row.getTotalSalary(),
                        average(row.getTotalSalary(), row.getHeadcount())
                ))
                .toList();

        return new AnalyticsSummaryResponse(
                employeeRepository.count(),
                currencies,
                departments,
                countries
        );
    }

    private BigDecimal average(BigDecimal total, long headcount) {
        return total.divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP);
    }
}
