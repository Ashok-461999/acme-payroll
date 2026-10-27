package com.acme.payroll.seed;

import com.acme.payroll.employee.Department;
import com.acme.payroll.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Creates deterministic demonstration data. The process only runs when the employee
 * table is empty, so restarting the application never duplicates data.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final long RANDOM_SEED = 20261028L;
    private static final int MAX_EMPLOYEES = 100_000;
    private static final LocalDate EARLIEST_HIRE_DATE = LocalDate.of(2016, 1, 1);

    private static final String[] FIRST_NAMES = {
            "Asha", "Rohan", "Maya", "Arjun", "Priya", "Daniel", "Sofia", "Liam",
            "Noah", "Emma", "Olivia", "Lucas", "Mia", "Ethan", "Ava", "Mason",
            "Isabella", "James", "Amelia", "Benjamin", "Zara", "Omar", "Hana",
            "Kenji", "Ana", "Mateo", "Lena", "Tariq", "Nina", "Felix", "Grace",
            "Ivan", "Leila", "Sam", "Mei", "Diego", "Aisha", "Neil", "Clara", "Victor"
    };

    private static final String[] LAST_NAMES = {
            "Patel", "Sharma", "Chen", "Kumar", "Nair", "Smith", "Garcia", "Johnson",
            "Brown", "Wilson", "Martin", "Lee", "Walker", "Hall", "Young", "King",
            "Wright", "Scott", "Green", "Baker", "Adams", "Nelson", "Hill", "Ramirez",
            "Campbell", "Mitchell", "Roberts", "Carter", "Phillips", "Evans", "Turner",
            "Torres", "Parker", "Collins", "Edwards", "Stewart", "Morris", "Murphy", "Cook", "Rogers"
    };

    private static final List<CountrySeed> COUNTRIES = List.of(
            new CountrySeed("US", "USD", new BigDecimal("95000")),
            new CountrySeed("IN", "INR", new BigDecimal("1800000")),
            new CountrySeed("GB", "GBP", new BigDecimal("70000")),
            new CountrySeed("DE", "EUR", new BigDecimal("75000")),
            new CountrySeed("CA", "CAD", new BigDecimal("90000")),
            new CountrySeed("AU", "AUD", new BigDecimal("95000")),
            new CountrySeed("SG", "SGD", new BigDecimal("110000")),
            new CountrySeed("BR", "BRL", new BigDecimal("240000")),
            new CountrySeed("JP", "JPY", new BigDecimal("8000000")),
            new CountrySeed("ZA", "ZAR", new BigDecimal("900000"))
    );

    private static final Map<Department, String[]> JOB_TITLES = Map.ofEntries(
            Map.entry(Department.ENGINEERING, new String[]{"Software Engineer", "Senior Engineer", "Engineering Manager", "QA Engineer"}),
            Map.entry(Department.PRODUCT, new String[]{"Product Manager", "Associate Product Manager", "Product Owner"}),
            Map.entry(Department.DESIGN, new String[]{"Product Designer", "UX Researcher", "Design Lead"}),
            Map.entry(Department.SALES, new String[]{"Account Executive", "Sales Manager", "Sales Development Representative"}),
            Map.entry(Department.MARKETING, new String[]{"Marketing Manager", "Content Strategist", "Growth Marketer"}),
            Map.entry(Department.FINANCE, new String[]{"Financial Analyst", "Payroll Specialist", "Finance Manager"}),
            Map.entry(Department.HUMAN_RESOURCES, new String[]{"HR Business Partner", "Recruiter", "People Operations Specialist"}),
            Map.entry(Department.OPERATIONS, new String[]{"Operations Analyst", "Operations Manager", "Workplace Coordinator"}),
            Map.entry(Department.CUSTOMER_SUCCESS, new String[]{"Customer Success Manager", "Support Specialist", "Onboarding Specialist"}),
            Map.entry(Department.LEGAL, new String[]{"Legal Counsel", "Compliance Analyst", "Contracts Manager"})
    );

    private static final Map<Department, BigDecimal> DEPARTMENT_FACTORS = Map.ofEntries(
            Map.entry(Department.ENGINEERING, new BigDecimal("1.15")),
            Map.entry(Department.PRODUCT, new BigDecimal("1.10")),
            Map.entry(Department.DESIGN, new BigDecimal("0.95")),
            Map.entry(Department.SALES, new BigDecimal("1.00")),
            Map.entry(Department.MARKETING, new BigDecimal("0.90")),
            Map.entry(Department.FINANCE, new BigDecimal("1.05")),
            Map.entry(Department.HUMAN_RESOURCES, new BigDecimal("0.85")),
            Map.entry(Department.OPERATIONS, new BigDecimal("0.85")),
            Map.entry(Department.CUSTOMER_SUCCESS, new BigDecimal("0.80")),
            Map.entry(Department.LEGAL, new BigDecimal("1.10"))
    );

    private static final String INSERT_EMPLOYEE = """
            INSERT INTO employee (
                employee_code, first_name, last_name, email, department, job_title,
                country, currency, hire_date, current_salary, current_salary_effective_date,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_SALARY = """
            INSERT INTO salary_record (
                employee_id, amount, effective_date, reason, note, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final SeedProperties properties;
    private final EmployeeRepository employeeRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(SeedProperties properties,
                      EmployeeRepository employeeRepository,
                      JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.employeeRepository = employeeRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("Seed data is disabled");
            return;
        }
        if (properties.count() < 1 || properties.count() > MAX_EMPLOYEES) {
            throw new IllegalStateException("app.seed.count must be between 1 and " + MAX_EMPLOYEES);
        }
        long existing = employeeRepository.count();
        if (existing > 0) {
            log.info("Skipping seed because {} employees already exist", existing);
            return;
        }

        long startedAt = System.currentTimeMillis();
        seed(properties.count());
        log.info("Seeded {} employees and their salary history in {} ms",
                properties.count(), System.currentTimeMillis() - startedAt);
    }

    private void seed(int count) {
        Random random = new Random(RANDOM_SEED);
        LocalDate today = LocalDate.now();
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        List<SeedEmployee> employees = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            employees.add(createEmployee(index, random, today));
        }

        List<Object[]> employeeRows = new ArrayList<>(count);
        for (SeedEmployee employee : employees) {
            employeeRows.add(new Object[]{
                    employee.employeeCode(),
                    employee.firstName(),
                    employee.lastName(),
                    employee.email(),
                    employee.department().name(),
                    employee.jobTitle(),
                    employee.country(),
                    employee.currency(),
                    java.sql.Date.valueOf(employee.hireDate()),
                    employee.currentSalary(),
                    java.sql.Date.valueOf(employee.currentSalaryEffectiveDate()),
                    createdAt,
                    createdAt
            });
        }
        jdbcTemplate.batchUpdate(INSERT_EMPLOYEE, employeeRows);

        Map<String, Long> idsByCode = new HashMap<>();
        jdbcTemplate.query("SELECT employee_code, id FROM employee", resultSet -> {
            idsByCode.put(resultSet.getString("employee_code"), resultSet.getLong("id"));
        });

        List<Object[]> salaryRows = new ArrayList<>(count + count / 2);
        for (SeedEmployee employee : employees) {
            Long employeeId = idsByCode.get(employee.employeeCode());
            if (employeeId == null) {
                throw new IllegalStateException("Seed employee id not found for " + employee.employeeCode());
            }
            for (SeedSalary salary : employee.salaries()) {
                salaryRows.add(new Object[]{
                        employeeId,
                        salary.amount(),
                        java.sql.Date.valueOf(salary.effectiveDate()),
                        salary.reason().name(),
                        salary.note(),
                        createdAt
                });
            }
        }
        jdbcTemplate.batchUpdate(INSERT_SALARY, salaryRows);
    }

    private SeedEmployee createEmployee(int index, Random random, LocalDate today) {
        CountrySeed country = COUNTRIES.get(index % COUNTRIES.size());
        Department department = Department.values()[random.nextInt(Department.values().length)];
        String[] titles = JOB_TITLES.get(department);
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String employeeCode = String.format("EMP-%05d", index + 1);
        String email = (firstName + "." + lastName + String.format("%05d", index + 1))
                .toLowerCase()
                + "@acme.example";

        int hireWindowDays = (int) ChronoUnit.DAYS.between(EARLIEST_HIRE_DATE, today.minusDays(30));
        LocalDate hireDate = EARLIEST_HIRE_DATE.plusDays(random.nextInt(hireWindowDays + 1));

        BigDecimal initialSalary = country.baseSalary()
                .multiply(DEPARTMENT_FACTORS.get(department))
                .multiply(BigDecimal.valueOf(0.90 + random.nextDouble() * 0.20))
                .setScale(2, RoundingMode.HALF_UP);

        List<SeedSalary> salaries = new ArrayList<>(3);
        salaries.add(new SeedSalary(hireDate, initialSalary, com.acme.payroll.salary.SalaryReason.INITIAL, null));

        if (hireDate.isBefore(today.minusDays(365)) && index % 3 == 0) {
            LocalDate reviewDate = hireDate.plusDays(365 + random.nextInt(180));
            if (reviewDate.isAfter(today)) {
                reviewDate = today;
            }
            BigDecimal reviewedSalary = increase(initialSalary, 1.03 + random.nextDouble() * 0.07);
            salaries.add(new SeedSalary(
                    reviewDate,
                    reviewedSalary,
                    com.acme.payroll.salary.SalaryReason.ANNUAL_REVIEW,
                    "Annual review"
            ));

            if (index % 11 == 0 && reviewDate.isBefore(today.minusDays(200))) {
                LocalDate promotionDate = reviewDate.plusDays(200 + random.nextInt(300));
                if (promotionDate.isAfter(today)) {
                    promotionDate = today;
                }
                if (promotionDate.isAfter(reviewDate)) {
                    salaries.add(new SeedSalary(
                            promotionDate,
                            increase(reviewedSalary, 1.05 + random.nextDouble() * 0.08),
                            com.acme.payroll.salary.SalaryReason.PROMOTION,
                            "Promotion"
                    ));
                }
            }
        }

        SeedSalary latest = salaries.get(salaries.size() - 1);
        return new SeedEmployee(
                employeeCode,
                firstName,
                lastName,
                email,
                department,
                titles[random.nextInt(titles.length)],
                country.code(),
                country.currency(),
                hireDate,
                latest.amount(),
                latest.effectiveDate(),
                salaries
        );
    }

    private BigDecimal increase(BigDecimal current, double factor) {
        return current.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    private record CountrySeed(String code, String currency, BigDecimal baseSalary) {
    }

    private record SeedEmployee(
            String employeeCode,
            String firstName,
            String lastName,
            String email,
            Department department,
            String jobTitle,
            String country,
            String currency,
            LocalDate hireDate,
            BigDecimal currentSalary,
            LocalDate currentSalaryEffectiveDate,
            List<SeedSalary> salaries
    ) {
    }

    private record SeedSalary(
            LocalDate effectiveDate,
            BigDecimal amount,
            com.acme.payroll.salary.SalaryReason reason,
            String note
    ) {
    }
}
