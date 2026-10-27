package com.acme.payroll.salary.dto;

import com.acme.payroll.salary.SalaryReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryChangeRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotNull
        @PastOrPresent
        LocalDate effectiveDate,

        @NotNull
        SalaryReason reason,

        @Size(max = 255)
        String note
) {
}
