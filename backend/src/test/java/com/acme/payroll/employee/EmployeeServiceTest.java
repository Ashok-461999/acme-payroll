package com.acme.payroll.employee;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createsEmployeeAndInitialSalaryRecord() {
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEmployeeRequest request = validCreateRequest(
                " emp-1001 ",
                " Asha@Example.COM ",
                "in",
                "inr"
        );

        EmployeeResponse response = employeeService.create(request).employee();

        assertThat(response.employeeCode()).isEqualTo("EMP-1001");
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.country()).isEqualTo("IN");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.firstName()).isEqualTo("Asha");
        assertThat(response.currentSalary()).isEqualByComparingTo("100000.00");
        assertThat(response.currentSalaryEffectiveDate()).isEqualTo(request.initialSalaryEffectiveDate());

        ArgumentCaptor<SalaryRecord> salaryCaptor = ArgumentCaptor.forClass(SalaryRecord.class);
        verify(salaryRecordRepository).save(salaryCaptor.capture());
        SalaryRecord savedSalary = salaryCaptor.getValue();
        assertThat(savedSalary.getReason()).isEqualTo(SalaryReason.INITIAL);
        assertThat(savedSalary.getAmount()).isEqualByComparingTo("100000.00");
        assertThat(savedSalary.getEffectiveDate()).isEqualTo(request.initialSalaryEffectiveDate());
    }

    @Test
    void rejectsDuplicateEmployeeCode() {
        when(employeeRepository.findByEmployeeCode("EMP-1001"))
                .thenReturn(Optional.of(employee()));

        assertThatThrownBy(() -> employeeService.create(validCreateRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EMP-1001");

        verify(employeeRepository, never()).save(any());
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void replaysAnIdenticalCreateRequestWithoutDuplicating() {
        LocalDate hireDate = LocalDate.now().minusYears(1);
        Employee existing = new Employee(
                "EMP-1001",
                "Asha",
                "Patel",
                "asha@example.com",
                Department.ENGINEERING,
                "Engineer",
                "IN",
                "INR",
                hireDate,
                new BigDecimal("100000.00"),
                hireDate
        );
        when(employeeRepository.findByEmployeeCode("EMP-1001"))
                .thenReturn(Optional.of(existing));

        EmployeeCreationResult result = employeeService.create(validCreateRequest());

        assertThat(result.created()).isFalse();
        assertThat(result.employee().employeeCode()).isEqualTo("EMP-1001");
        verify(employeeRepository, never()).save(any());
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateEmailAfterNormalisation() {
        when(employeeRepository.existsByEmail("asha@example.com")).thenReturn(true);

        CreateEmployeeRequest request = validCreateRequest(
                "EMP-1002",
                "ASHA@EXAMPLE.COM",
                "IN",
                "INR"
        );

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("asha@example.com");
    }

    @Test
    void rejectsInitialEffectiveDateBeforeHireDate() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-1003",
                "Asha",
                "Patel",
                "asha@example.com",
                Department.ENGINEERING,
                "Engineer",
                "IN",
                "INR",
                LocalDate.now().minusYears(1),
                new BigDecimal("100000.00"),
                LocalDate.now().minusYears(2)
        );

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessValidationException.class)
                .extracting("field")
                .isEqualTo("initialSalaryEffectiveDate");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidCountryAndCurrencyCodes() {
        CreateEmployeeRequest invalidCountry = validCreateRequest("EMP-1004", "asha@example.com", "ZZ", "INR");
        assertThatThrownBy(() -> employeeService.create(invalidCountry))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("country");

        CreateEmployeeRequest invalidCurrency = validCreateRequest("EMP-1005", "asha@example.com", "IN", "ZZZ");
        assertThatThrownBy(() -> employeeService.create(invalidCurrency))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsNonPositiveAndExcessivelyPreciseSalaries() {
        CreateEmployeeRequest zeroSalary = new CreateEmployeeRequest(
                "EMP-1006", "Asha", "Patel", "asha@example.com", Department.ENGINEERING,
                "Engineer", "IN", "INR", LocalDate.now().minusYears(1),
                BigDecimal.ZERO, LocalDate.now().minusYears(1)
        );
        assertThatThrownBy(() -> employeeService.create(zeroSalary))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("greater than zero");

        CreateEmployeeRequest threeDecimals = new CreateEmployeeRequest(
                "EMP-1007", "Asha", "Patel", "asha@example.com", Department.ENGINEERING,
                "Engineer", "IN", "INR", LocalDate.now().minusYears(1),
                new BigDecimal("100.999"), LocalDate.now().minusYears(1)
        );
        assertThatThrownBy(() -> employeeService.create(threeDecimals))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("two decimal places");
    }

    @Test
    void rejectsFutureHireDate() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-1008", "Asha", "Patel", "asha@example.com", Department.ENGINEERING,
                "Engineer", "IN", "INR", LocalDate.now().plusDays(1),
                new BigDecimal("100000.00"), LocalDate.now().plusDays(1)
        );

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void updatesMutableEmployeeFieldsAndChecksEmailOwnership() {
        Employee employee = employee();
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("new@example.com", 7L)).thenReturn(false);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Maya", "Nair", "New@Example.com", Department.PRODUCT, "Product Manager", "gb"
        );

        EmployeeResponse response = employeeService.update(7L, request);

        assertThat(response.firstName()).isEqualTo("Maya");
        assertThat(response.lastName()).isEqualTo("Nair");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.department()).isEqualTo(Department.PRODUCT);
        assertThat(response.country()).isEqualTo("GB");
        assertThat(employee.getCurrentSalary()).isEqualByComparingTo("100000.00");
        verify(employeeRepository).save(employee);
    }

    @Test
    void rejectsDuplicateEmailWhenUpdatingAnotherEmployee() {
        Employee employee = employee();
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("taken@example.com", 7L)).thenReturn(true);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Maya", "Nair", "taken@example.com", Department.PRODUCT, "Product Manager", "GB"
        );

        assertThatThrownBy(() -> employeeService.update(7L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    void returnsPagedSearchResultsWithStableSort() {
        Employee employee = employee();
        Pageable pageable = PageRequest.of(0, 20);
        when(employeeRepository.search(eq(""), isNull(), eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        PageResponse<EmployeeResponse> response = employeeService.search(
                null, null, null, null, 0, 20, "lastName", "asc"
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.first()).isTrue();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepository).search(
                eq(""), isNull(), eq(""), eq(""), pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("lastName")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("lastName").isAscending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void rejectsInvalidPagingAndSortParameters() {
        assertThatThrownBy(() -> employeeService.search(null, null, null, null, -1, 20, "lastName", "asc"))
                .isInstanceOf(BusinessValidationException.class)
                .extracting("field")
                .isEqualTo("page");

        assertThatThrownBy(() -> employeeService.search(null, null, null, null, 0, 101, "lastName", "asc"))
                .isInstanceOf(BusinessValidationException.class)
                .extracting("field")
                .isEqualTo("size");

        assertThatThrownBy(() -> employeeService.search(null, null, null, null, 0, 20, "dropTable", "asc"))
                .isInstanceOf(BusinessValidationException.class)
                .extracting("field")
                .isEqualTo("sortBy");

        assertThatThrownBy(() -> employeeService.search(null, null, null, null, 0, 20, "lastName", "sideways"))
                .isInstanceOf(BusinessValidationException.class)
                .extracting("field")
                .isEqualTo("direction");
    }

    @Test
    void rejectsUnknownEmployeeOnRead() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    private CreateEmployeeRequest validCreateRequest() {
        return validCreateRequest("EMP-1001", "asha@example.com", "IN", "INR");
    }

    private CreateEmployeeRequest validCreateRequest(String employeeCode,
                                                    String email,
                                                    String country,
                                                    String currency) {
        LocalDate hireDate = LocalDate.now().minusYears(1);
        return new CreateEmployeeRequest(
                employeeCode,
                " Asha ",
                " Patel ",
                email,
                Department.ENGINEERING,
                " Engineer ",
                country,
                currency,
                hireDate,
                new BigDecimal("100000.00"),
                hireDate
        );
    }

    private Employee employee() {
        LocalDate hireDate = LocalDate.now().minusYears(2);
        return new Employee(
                "EMP-1001",
                "Asha",
                "Patel",
                "asha@example.com",
                Department.ENGINEERING,
                "Engineer",
                "IN",
                "USD",
                hireDate,
                new BigDecimal("100000.00"),
                hireDate.plusYears(1)
        );
    }
}
