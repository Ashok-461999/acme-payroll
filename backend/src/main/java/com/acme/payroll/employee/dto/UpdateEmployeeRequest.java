package com.acme.payroll.employee.dto;

import com.acme.payroll.employee.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
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
        String country
) {
}
