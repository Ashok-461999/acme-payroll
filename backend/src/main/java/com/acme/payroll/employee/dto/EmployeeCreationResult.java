package com.acme.payroll.employee.dto;

public record EmployeeCreationResult(EmployeeResponse employee, boolean created) {
}
