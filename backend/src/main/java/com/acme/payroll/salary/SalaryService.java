package com.acme.payroll.salary;

import com.acme.payroll.common.MoneyRules;
import com.acme.payroll.common.exception.BusinessValidationException;
import com.acme.payroll.common.exception.ResourceNotFoundException;
import com.acme.payroll.employee.Employee;
import com.acme.payroll.employee.EmployeeRepository;
import com.acme.payroll.salary.dto.SalaryChangeRequest;
import com.acme.payroll.salary.dto.SalaryRecordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalaryService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public SalaryService(EmployeeRepository employeeRepository,
                         SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Transactional
    public SalaryRecordResponse changeSalary(Long employeeId, SalaryChangeRequest request) {
        // The lock serialises concurrent changes for this employee so the latest
        // effective date and the current salary cannot diverge.
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        BigDecimal amount = MoneyRules.requireValidAmount(request.amount(), "amount");
        LocalDate effectiveDate = request.effectiveDate();
        LocalDate today = LocalDate.now();

        if (effectiveDate.isAfter(today)) {
            throw new BusinessValidationException("effectiveDate", "must not be in the future");
        }
        if (effectiveDate.isBefore(employee.getHireDate())) {
            throw new BusinessValidationException("effectiveDate", "must be on or after the hire date");
        }
        if (!effectiveDate.isAfter(employee.getCurrentSalaryEffectiveDate())) {
            throw new BusinessValidationException(
                    "effectiveDate",
                    "must be after the current salary effective date "
                            + employee.getCurrentSalaryEffectiveDate()
            );
        }

        SalaryRecord record = new SalaryRecord(
                employee,
                amount,
                effectiveDate,
                request.reason(),
                normaliseNote(request.note())
        );
        salaryRecordRepository.save(record);

        employee.updateCurrentSalary(amount, effectiveDate);
        employeeRepository.save(employee);

        return SalaryRecordResponse.from(record, employee.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<SalaryRecordResponse> history(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        return salaryRecordRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream()
                .map(record -> SalaryRecordResponse.from(record, employee.getCurrency()))
                .toList();
    }

    private String normaliseNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }
}
