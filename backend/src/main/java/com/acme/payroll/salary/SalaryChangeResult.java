package com.acme.payroll.salary;

import com.acme.payroll.salary.dto.SalaryRecordResponse;

public record SalaryChangeResult(SalaryRecordResponse record, boolean created) {
}
