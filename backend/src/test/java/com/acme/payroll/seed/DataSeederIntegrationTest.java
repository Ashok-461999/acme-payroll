package com.acme.payroll.seed;

import com.acme.payroll.employee.Employee;
import com.acme.payroll.employee.EmployeeRepository;
import com.acme.payroll.salary.SalaryRecord;
import com.acme.payroll.salary.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "app.seed.count=50",
        "spring.datasource.url=jdbc:h2:mem:seedtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DataSeederIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void seedsExactlyTheConfiguredNumberOfConsistentEmployees() {
        List<Employee> employees = employeeRepository.findAll();

        assertThat(employees).hasSize(50);
        assertThat(employees).extracting(Employee::getEmployeeCode)
                .contains("EMP-00001", "EMP-00050")
                .doesNotHaveDuplicates();
        assertThat(new HashSet<>(employees.stream().map(Employee::getEmail).toList())).hasSize(50);

        Set<String> currencies = new HashSet<>();
        for (Employee employee : employees) {
            SalaryRecord latest = salaryRecordRepository
                    .findTopByEmployeeIdOrderByEffectiveDateDesc(employee.getId())
                    .orElseThrow();
            assertThat(employee.getCurrentSalary()).isEqualByComparingTo(latest.getAmount());
            assertThat(employee.getCurrentSalaryEffectiveDate()).isEqualTo(latest.getEffectiveDate());
            assertThat(employee.getHireDate()).isBeforeOrEqualTo(java.time.LocalDate.now());
            currencies.add(employee.getCurrency());
        }
        assertThat(currencies).hasSize(10);
        assertThat(salaryRecordRepository.count()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void doesNotDuplicateDataWhenRunAgain() throws Exception {
        long before = employeeRepository.count();
        dataSeeder.run(new DefaultApplicationArguments(new String[0]));
        assertThat(employeeRepository.count()).isEqualTo(before);
    }
}
