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
import java.util.Objects;
import java.util.Optional;

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
    public SalaryChangeResult changeSalary(Long employeeId, SalaryChangeRequest request) {
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

        if (effectiveDate.equals(employee.getCurrentSalaryEffectiveDate())
                && amount.compareTo(employee.getCurrentSalary()) == 0) {
            Optional<SalaryRecord> latest = salaryRecordRepository
                    .findTopByEmployeeIdOrderByEffectiveDateDesc(employeeId);
            if (latest.isPresent() && isIdenticalChange(latest.get(), request, amount, effectiveDate)) {
                // (employee, effective date, amount, reason, note) is the natural key
                // for a salary change. Replaying the same request returns the record
                // that the first attempt created instead of failing the retry.
                return new SalaryChangeResult(
                        SalaryRecordResponse.from(latest.get(), employee.getCurrency()),
                        false
                );
            }
        }

        if (effectiveDate.equals(employee.getCurrentSalaryEffectiveDate())) {
            throw new BusinessValidationException(
                    "effectiveDate",
                    "already has a salary change effective on " + effectiveDate
                            + "; use a later effective date"
            );
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

        return new SalaryChangeResult(
                SalaryRecordResponse.from(record, employee.getCurrency()),
                true
        );
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

    private boolean isIdenticalChange(SalaryRecord record,
                                      SalaryChangeRequest request,
                                      BigDecimal amount,
                                      LocalDate effectiveDate) {
        return record.getAmount().compareTo(amount) == 0
                && record.getEffectiveDate().equals(effectiveDate)
                && record.getReason() == request.reason()
                && Objects.equals(normaliseNote(record.getNote()), normaliseNote(request.note()));
    }
}
