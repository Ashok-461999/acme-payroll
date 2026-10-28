package com.acme.payroll.salary;

import com.acme.payroll.common.exception.BusinessValidationException;
import com.acme.payroll.common.exception.ResourceNotFoundException;
import com.acme.payroll.employee.Department;
import com.acme.payroll.employee.Employee;
import com.acme.payroll.employee.EmployeeRepository;
import com.acme.payroll.salary.dto.SalaryChangeRequest;
import com.acme.payroll.salary.dto.SalaryRecordResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private SalaryService salaryService;

    @Test
    void recordsSalaryChangeAndUpdatesCurrentSalary() {
        Employee employee = employee();
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate effectiveDate = LocalDate.now().minusDays(1);
        SalaryChangeRequest request = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                effectiveDate,
                SalaryReason.PROMOTION,
                "  Promotion to senior engineer  "
        );

        SalaryChangeResult result = salaryService.changeSalary(1L, request);
        SalaryRecordResponse response = result.record();

        assertThat(result.created()).isTrue();
        assertThat(response.amount()).isEqualByComparingTo("120000.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.effectiveDate()).isEqualTo(effectiveDate);
        assertThat(response.reason()).isEqualTo(SalaryReason.PROMOTION);
        assertThat(response.note()).isEqualTo("Promotion to senior engineer");
        assertThat(employee.getCurrentSalary()).isEqualByComparingTo("120000.00");
        assertThat(employee.getCurrentSalaryEffectiveDate()).isEqualTo(effectiveDate);

        ArgumentCaptor<SalaryRecord> recordCaptor = ArgumentCaptor.forClass(SalaryRecord.class);
        verify(salaryRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAmount()).isEqualByComparingTo("120000.00");
        assertThat(recordCaptor.getValue().getEmployee()).isSameAs(employee);
    }

    @Test
    void rejectsFutureEffectiveDate() {
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee()));

        SalaryChangeRequest request = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                LocalDate.now().plusDays(1),
                SalaryReason.ANNUAL_REVIEW,
                null
        );

        assertThatThrownBy(() -> salaryService.changeSalary(1L, request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("future");
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void rejectsEffectiveDateBeforeHireDate() {
        Employee employee = employee();
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));

        SalaryChangeRequest request = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                employee.getHireDate().minusDays(1),
                SalaryReason.OTHER,
                null
        );

        assertThatThrownBy(() -> salaryService.changeSalary(1L, request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("hire date");
    }

    @Test
    void rejectsEffectiveDateThatDoesNotAdvanceHistory() {
        Employee employee = employee();
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));

        SalaryChangeRequest sameDate = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                employee.getCurrentSalaryEffectiveDate(),
                SalaryReason.ANNUAL_REVIEW,
                null
        );
        assertThatThrownBy(() -> salaryService.changeSalary(1L, sameDate))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already has a salary change effective on");

        SalaryChangeRequest earlierDate = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                employee.getCurrentSalaryEffectiveDate().minusDays(1),
                SalaryReason.ANNUAL_REVIEW,
                null
        );
        assertThatThrownBy(() -> salaryService.changeSalary(1L, earlierDate))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("after the current salary effective date");
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void replaysAnIdenticalSalaryChangeWithoutWritingAgain() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee(
                "EMP-1002",
                "Asha",
                "Patel",
                "asha@example.com",
                Department.ENGINEERING,
                "Engineer",
                "US",
                "USD",
                today.minusYears(2),
                new BigDecimal("120000.00"),
                today
        );
        SalaryRecord latest = new SalaryRecord(
                employee,
                new BigDecimal("120000.00"),
                today,
                SalaryReason.PROMOTION,
                "Promotion"
        );
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(1L))
                .thenReturn(Optional.of(latest));

        SalaryChangeRequest request = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                today,
                SalaryReason.PROMOTION,
                "  Promotion  "
        );

        SalaryChangeResult result = salaryService.changeSalary(1L, request);

        assertThat(result.created()).isFalse();
        assertThat(result.record().amount()).isEqualByComparingTo("120000.00");
        assertThat(result.record().reason()).isEqualTo(SalaryReason.PROMOTION);

        SalaryChangeRequest differentReason = new SalaryChangeRequest(
                new BigDecimal("120000.00"),
                today,
                SalaryReason.ANNUAL_REVIEW,
                "Promotion"
        );
        assertThatThrownBy(() -> salaryService.changeSalary(1L, differentReason))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already has a salary change effective on");

        verify(salaryRecordRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidSalaryAmounts() {
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee()));

        SalaryChangeRequest negative = new SalaryChangeRequest(
                new BigDecimal("-1.00"), LocalDate.now(), SalaryReason.OTHER, null
        );
        assertThatThrownBy(() -> salaryService.changeSalary(1L, negative))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("greater than zero");

        SalaryChangeRequest tooPrecise = new SalaryChangeRequest(
                new BigDecimal("100.001"), LocalDate.now(), SalaryReason.OTHER, null
        );
        assertThatThrownBy(() -> salaryService.changeSalary(1L, tooPrecise))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("two decimal places");

        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void rejectsMissingEmployee() {
        when(employeeRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        SalaryChangeRequest request = new SalaryChangeRequest(
                new BigDecimal("120000.00"), LocalDate.now(), SalaryReason.OTHER, null
        );

        assertThatThrownBy(() -> salaryService.changeSalary(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void returnsHistoryNewestFirstWithEmployeeCurrency() {
        Employee employee = employee();
        SalaryRecord older = new SalaryRecord(
                employee,
                new BigDecimal("100000.00"),
                LocalDate.now().minusYears(2),
                SalaryReason.INITIAL,
                null
        );
        SalaryRecord newer = new SalaryRecord(
                employee,
                new BigDecimal("110000.00"),
                LocalDate.now().minusYears(1),
                SalaryReason.ANNUAL_REVIEW,
                "Annual review"
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployeeIdOrderByEffectiveDateDesc(1L))
                .thenReturn(List.of(newer, older));

        List<SalaryRecordResponse> history = salaryService.history(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).amount()).isEqualByComparingTo("110000.00");
        assertThat(history.get(0).currency()).isEqualTo("USD");
        assertThat(history.get(1).reason()).isEqualTo(SalaryReason.INITIAL);
    }

    @Test
    void rejectsHistoryForMissingEmployee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salaryService.history(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
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
                "US",
                "USD",
                hireDate,
                new BigDecimal("100000.00"),
                LocalDate.now().minusDays(30)
        );
    }
}
