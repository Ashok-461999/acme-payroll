package com.acme.payroll.salary.dto;

import com.acme.payroll.salary.SalaryReason;
import com.acme.payroll.salary.SalaryRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryRecordResponse(
        Long id,
        BigDecimal amount,
        String currency,
        LocalDate effectiveDate,
        SalaryReason reason,
        String note,
        Instant createdAt
) {
    public static SalaryRecordResponse from(SalaryRecord record, String currency) {
        return new SalaryRecordResponse(
                record.getId(),
                record.getAmount(),
                currency,
                record.getEffectiveDate(),
                record.getReason(),
                record.getNote(),
                record.getCreatedAt()
        );
    }
}
