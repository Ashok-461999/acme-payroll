package com.acme.payroll.employee.dto;

import com.acme.payroll.employee.Department;
import com.acme.payroll.employee.Employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        Department department,
        String jobTitle,
        String country,
        String currency,
        LocalDate hireDate,
        BigDecimal currentSalary,
        LocalDate currentSalaryEffectiveDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getCountry(),
                employee.getCurrency(),
                employee.getHireDate(),
                employee.getCurrentSalary(),
                employee.getCurrentSalaryEffectiveDate(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
