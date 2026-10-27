package com.acme.payroll.employee.dto;

import com.acme.payroll.employee.Department;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must contain only letters, digits and hyphens")
        String employeeCode,

        @NotBlank
        @Size(max = 80)
        String firstName,

        @NotBlank
        @Size(max = 80)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotNull
        Department department,

        @NotBlank
        @Size(max = 100)
        String jobTitle,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter ISO country code")
        String country,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three-letter ISO currency code")
        String currency,

        @NotNull
        @PastOrPresent
        LocalDate hireDate,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal initialSalary,

        @NotNull
        @PastOrPresent
        LocalDate initialSalaryEffectiveDate
) {
}
