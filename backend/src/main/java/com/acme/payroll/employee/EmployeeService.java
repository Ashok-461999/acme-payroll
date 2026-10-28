package com.acme.payroll.employee;

import com.acme.payroll.common.IsoReferenceData;
import com.acme.payroll.common.MoneyRules;
import com.acme.payroll.common.dto.PageResponse;
import com.acme.payroll.common.exception.BusinessValidationException;
import com.acme.payroll.common.exception.ConflictException;
import com.acme.payroll.common.exception.ResourceNotFoundException;
import com.acme.payroll.employee.dto.CreateEmployeeRequest;
import com.acme.payroll.employee.dto.EmployeeCreationResult;
import com.acme.payroll.employee.dto.EmployeeResponse;
import com.acme.payroll.employee.dto.UpdateEmployeeRequest;
import com.acme.payroll.salary.SalaryReason;
import com.acme.payroll.salary.SalaryRecord;
import com.acme.payroll.salary.SalaryRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "employeeCode", "firstName", "lastName", "department",
            "country", "currency", "hireDate", "currentSalary"
    );

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Transactional
    public EmployeeCreationResult create(CreateEmployeeRequest request) {
        String employeeCode = request.employeeCode().trim().toUpperCase(Locale.ROOT);
        String email = normaliseEmail(request.email());
        String country = IsoReferenceData.requireCountry(request.country());
        String currency = IsoReferenceData.requireCurrency(request.currency());
        BigDecimal initialSalary = MoneyRules.requireValidAmount(request.initialSalary(), "initialSalary");

        LocalDate today = LocalDate.now();
        if (request.hireDate().isAfter(today)) {
            throw new BusinessValidationException("hireDate", "must not be in the future");
        }
        if (request.initialSalaryEffectiveDate().isBefore(request.hireDate())) {
            throw new BusinessValidationException(
                    "initialSalaryEffectiveDate",
                    "must be on or after the hire date"
            );
        }
        if (request.initialSalaryEffectiveDate().isAfter(today)) {
            throw new BusinessValidationException(
                    "initialSalaryEffectiveDate",
                    "must not be in the future"
            );
        }

        Optional<Employee> existingByCode = employeeRepository.findByEmployeeCode(employeeCode);
        if (existingByCode.isPresent()) {
            Employee existing = existingByCode.get();
            if (isIdenticalCreateRequest(
                    existing, request, employeeCode, email, country, currency, initialSalary
            )) {
                // Employee code is the natural idempotency key. Replaying an identical
                // create returns the original record instead of duplicating the employee.
                return new EmployeeCreationResult(EmployeeResponse.from(existing), false);
            }
            throw new ConflictException("Employee code already exists: " + employeeCode);
        }
        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }

        Employee employee = new Employee(
                employeeCode,
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.department(),
                request.jobTitle().trim(),
                country,
                currency,
                request.hireDate(),
                initialSalary,
                request.initialSalaryEffectiveDate()
        );
        employeeRepository.save(employee);

        SalaryRecord initialRecord = new SalaryRecord(
                employee,
                initialSalary,
                request.initialSalaryEffectiveDate(),
                SalaryReason.INITIAL,
                null
        );
        salaryRecordRepository.save(initialRecord);

        return new EmployeeCreationResult(EmployeeResponse.from(employee), true);
    }

    @Transactional
    public EmployeeResponse update(Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        String email = normaliseEmail(request.email());
        String country = IsoReferenceData.requireCountry(request.country());

        if (employeeRepository.existsByEmailAndIdNot(email, employeeId)) {
            throw new ConflictException("Email already exists: " + email);
        }

        employee.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.department(),
                request.jobTitle().trim(),
                country
        );
        employeeRepository.save(employee);
        return EmployeeResponse.from(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return EmployeeResponse.from(employee);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String search,
                                                 Department department,
                                                 String country,
                                                 String currency,
                                                 int page,
                                                 int size,
                                                 String sortBy,
                                                 String direction) {
        validatePaging(page, size);
        Sort sort = buildSort(sortBy, direction);

        String normalisedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String normalisedCountry = country == null || country.isBlank()
                ? ""
                : country.trim().toUpperCase(Locale.ROOT);
        String normalisedCurrency = currency == null || currency.isBlank()
                ? ""
                : currency.trim().toUpperCase(Locale.ROOT);

        Page<Employee> result = employeeRepository.search(
                normalisedSearch,
                department,
                normalisedCountry,
                normalisedCurrency,
                PageRequest.of(page, size, sort)
        );

        List<EmployeeResponse> content = result.getContent().stream()
                .map(EmployeeResponse::from)
                .toList();
        return PageResponse.from(result, content);
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new BusinessValidationException("page", "must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessValidationException("size", "must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private Sort buildSort(String sortBy, String direction) {
        String requestedField = sortBy == null || sortBy.isBlank() ? "lastName" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(requestedField)) {
            throw new BusinessValidationException("sortBy", "unsupported sort field: " + requestedField);
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new BusinessValidationException("direction", "must be 'asc' or 'desc'");
        }

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        // The id tie-breaker keeps pagination stable when the sort field has duplicates.
        return Sort.by(sortDirection, requestedField).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isIdenticalCreateRequest(Employee existing,
                                             CreateEmployeeRequest request,
                                             String employeeCode,
                                             String email,
                                             String country,
                                             String currency,
                                             BigDecimal initialSalary) {
        return existing.getEmployeeCode().equals(employeeCode)
                && existing.getFirstName().equals(request.firstName().trim())
                && existing.getLastName().equals(request.lastName().trim())
                && existing.getEmail().equals(email)
                && existing.getDepartment() == request.department()
                && existing.getJobTitle().equals(request.jobTitle().trim())
                && existing.getCountry().equals(country)
                && existing.getCurrency().equals(currency)
                && existing.getHireDate().equals(request.hireDate())
                && existing.getCurrentSalary().compareTo(initialSalary) == 0
                && existing.getCurrentSalaryEffectiveDate().equals(request.initialSalaryEffectiveDate());
    }
}
