package com.acme.payroll.api;

import com.acme.payroll.employee.Department;
import com.acme.payroll.employee.Employee;
import com.acme.payroll.employee.EmployeeRepository;
import com.acme.payroll.employee.EmployeeService;
import com.acme.payroll.employee.dto.CreateEmployeeRequest;
import com.acme.payroll.salary.SalaryReason;
import com.acme.payroll.salary.SalaryRecord;
import com.acme.payroll.salary.SalaryRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @Test
    void runsEmployeeCreationSearchUpdateSalaryHistoryAndAnalyticsFlow() throws Exception {
        long employeeId = createEmployee(
                "EMP-1001", "asha@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("100000.00")
        );

        mockMvc.perform(get("/api/employees").param("search", "EMP-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Asha Patel"));

        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("EMP-1001"))
                .andExpect(jsonPath("$.currentSalary").value(100000.00));

        Map<String, Object> update = new HashMap<>();
        update.put("firstName", "Asha");
        update.put("lastName", "Patel");
        update.put("email", "asha.updated@example.com");
        update.put("department", "PRODUCT");
        update.put("jobTitle", "Senior Product Manager");
        update.put("country", "GB");
        mockMvc.perform(put("/api/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha.updated@example.com"))
                .andExpect(jsonPath("$.department").value("PRODUCT"))
                .andExpect(jsonPath("$.country").value("GB"))
                .andExpect(jsonPath("$.currentSalary").value(100000.00));

        LocalDate salaryDate = LocalDate.now();
        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                salaryBody(new BigDecimal("125000.00"), salaryDate, "PROMOTION")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(125000.00))
                .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(get("/api/employees/{id}/salaries", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(125000.00))
                .andExpect(jsonPath("$[0].reason").value("PROMOTION"))
                .andExpect(jsonPath("$[1].reason").value("INITIAL"));

        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSalary").value(125000.00))
                .andExpect(jsonPath("$.currentSalaryEffectiveDate").value(salaryDate.toString()));

        MvcResult analytics = mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(1))
                .andReturn();

        JsonNode usd = find(objectMapper.readTree(analytics.getResponse().getContentAsString())
                .get("currencies"), "currency", "USD");
        assertThat(usd.get("headcount").asLong()).isEqualTo(1);
        assertThat(usd.get("totalSalary").decimalValue()).isEqualByComparingTo("125000.00");
    }

    @Test
    void keepsCurrencyAggregatesSeparateAndCalculatesAverages() throws Exception {
        createEmployee("EMP-2001", "usd.one@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("100000.00"));
        createEmployee("EMP-2002", "usd.two@example.com", Department.SALES, "US", "USD",
                new BigDecimal("120000.50"));
        createEmployee("EMP-2003", "inr.one@example.com", Department.FINANCE, "IN", "INR",
                new BigDecimal("1000000.00"));

        MvcResult result = mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(3))
                .andExpect(jsonPath("$.currencies.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode usd = find(body.get("currencies"), "currency", "USD");
        assertThat(usd.get("headcount").asLong()).isEqualTo(2);
        assertThat(usd.get("totalSalary").decimalValue()).isEqualByComparingTo("220000.50");
        assertThat(usd.get("averageSalary").decimalValue()).isEqualByComparingTo("110000.25");
        assertThat(usd.get("minSalary").decimalValue()).isEqualByComparingTo("100000.00");
        assertThat(usd.get("maxSalary").decimalValue()).isEqualByComparingTo("120000.50");

        JsonNode inr = find(body.get("currencies"), "currency", "INR");
        assertThat(inr.get("headcount").asLong()).isEqualTo(1);
        assertThat(inr.get("totalSalary").decimalValue()).isEqualByComparingTo("1000000.00");

        JsonNode engineering = find(body.get("departments"), "department", "ENGINEERING");
        assertThat(engineering.get("currency").asText()).isEqualTo("USD");
        assertThat(engineering.get("totalSalary").decimalValue()).isEqualByComparingTo("100000.00");

        JsonNode finance = find(body.get("departments"), "department", "FINANCE");
        assertThat(finance.get("currency").asText()).isEqualTo("INR");
        assertThat(finance.get("totalSalary").decimalValue()).isEqualByComparingTo("1000000.00");

        JsonNode us = find(body.get("countries"), "country", "US");
        assertThat(us.get("headcount").asLong()).isEqualTo(2);
        JsonNode in = find(body.get("countries"), "country", "IN");
        assertThat(in.get("headcount").asLong()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateEmployeeCode() throws Exception {
        createEmployee("EMP-3001", "first@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeBody(
                                "EMP-3001", "second@example.com", Department.SALES, "US", "USD",
                                new BigDecimal("90000.00")
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee code already exists: EMP-3001"));
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() throws Exception {
        createEmployee("EMP-3101", "same@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeBody(
                                "EMP-3102", "SAME@EXAMPLE.COM", Department.SALES, "US", "USD",
                                new BigDecimal("90000.00")
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: same@example.com"));
    }

    @Test
    void rejectsInitialSalaryDateBeforeHireDate() throws Exception {
        Map<String, Object> body = employeeBody(
                "EMP-3201", "dates@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );
        body.put("initialSalaryEffectiveDate", LocalDate.now().minusYears(3).toString());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.initialSalaryEffectiveDate").exists());
    }

    @Test
    void rejectsSalaryThatDoesNotAdvanceTheHistory() throws Exception {
        long employeeId = createEmployee(
                "EMP-3301", "history@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );
        LocalDate firstIncrease = LocalDate.now().minusDays(10);
        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                salaryBody(new BigDecimal("95000.00"), firstIncrease, "ANNUAL_REVIEW")
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                salaryBody(new BigDecimal("96000.00"), firstIncrease, "ANNUAL_REVIEW")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.effectiveDate").exists());
    }

    @Test
    void rejectsFutureAndBeforeHireSalaryDates() throws Exception {
        long employeeId = createEmployee(
                "EMP-3401", "boundaries@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );

        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                salaryBody(new BigDecimal("95000.00"), LocalDate.now().plusDays(1), "OTHER")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.effectiveDate").exists());
    }

    @Test
    void rejectsSalaryBeforeHireDate() throws Exception {
        long employeeId = createEmployee(
                "EMP-3501", "beforehire@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );

        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                salaryBody(
                                        new BigDecimal("95000.00"),
                                        LocalDate.now().minusYears(3),
                                        "OTHER"
                                )
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.effectiveDate").exists());
    }

    @Test
    void returnsNotFoundForMissingEmployeeAndHistory() throws Exception {
        mockMvc.perform(get("/api/employees/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/employees/999999/salaries"))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginatesSearchesAndFiltersEmployees() throws Exception {
        for (int index = 1; index <= 25; index++) {
            Department department = index % 2 == 0 ? Department.SALES : Department.ENGINEERING;
            String country = index % 2 == 0 ? "IN" : "US";
            String currency = index % 2 == 0 ? "INR" : "USD";
            createEmployee(
                    String.format("EMP-%04d", 4000 + index),
                    "person" + index + "@example.com",
                    department,
                    country,
                    currency,
                    new BigDecimal("80000.00").add(BigDecimal.valueOf(index))
            );
        }

        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "employeeCode")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-4001"));

        mockMvc.perform(get("/api/employees").param("search", "person25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("person25@example.com"));

        mockMvc.perform(get("/api/employees")
                        .param("department", "SALES")
                        .param("country", "IN")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12));

        mockMvc.perform(get("/api/employees").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").exists());

        mockMvc.perform(get("/api/employees").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void databaseRejectsDuplicateSalaryDateForSameEmployee() throws Exception {
        long employeeId = createEmployee(
                "EMP-5001", "constraint@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        SalaryRecord duplicateDate = new SalaryRecord(
                employee,
                new BigDecimal("91000.00"),
                employee.getCurrentSalaryEffectiveDate(),
                SalaryReason.OTHER,
                null
        );

        assertThatThrownBy(() -> salaryRecordRepository.saveAndFlush(duplicateDate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void replaysIdenticalWritesWithoutCreatingDuplicates() throws Exception {
        Map<String, Object> employeeBody = employeeBody(
                "EMP-6001", "idempotent@example.com", Department.ENGINEERING, "US", "USD",
                new BigDecimal("90000.00")
        );
        String employeeJson = objectMapper.writeValueAsString(employeeBody);

        MvcResult firstCreate = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isCreated())
                .andReturn();
        long employeeId = objectMapper.readTree(firstCreate.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(employeeId));

        mockMvc.perform(get("/api/employees").param("search", "EMP-6001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        String salaryJson = objectMapper.writeValueAsString(
                salaryBody(new BigDecimal("95000.00"), LocalDate.now(), "ANNUAL_REVIEW")
        );
        MvcResult firstSalary = mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salaryJson))
                .andExpect(status().isCreated())
                .andReturn();
        long salaryId = objectMapper.readTree(firstSalary.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/employees/{id}/salaries", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salaryJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(salaryId));

        mockMvc.perform(get("/api/employees/{id}/salaries", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private long createEmployee(String code,
                                String email,
                                Department department,
                                String country,
                                String currency,
                                BigDecimal salary) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeBody(code, email, department, country, currency, salary)
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Map<String, Object> employeeBody(String code,
                                             String email,
                                             Department department,
                                             String country,
                                             String currency,
                                             BigDecimal salary) {
        LocalDate hireDate = LocalDate.now().minusYears(2);
        Map<String, Object> body = new HashMap<>();
        body.put("employeeCode", code);
        body.put("firstName", "Asha");
        body.put("lastName", "Patel");
        body.put("email", email);
        body.put("department", department.name());
        body.put("jobTitle", "Engineer");
        body.put("country", country);
        body.put("currency", currency);
        body.put("hireDate", hireDate.toString());
        body.put("initialSalary", salary);
        body.put("initialSalaryEffectiveDate", hireDate.toString());
        return body;
    }

    private Map<String, Object> salaryBody(BigDecimal amount, LocalDate effectiveDate, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("effectiveDate", effectiveDate.toString());
        body.put("reason", reason);
        body.put("note", "Test change");
        return body;
    }

    private JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode node : array) {
            if (value.equals(node.get(field).asText())) {
                return node;
            }
        }
        throw new AssertionError("No element with " + field + "=" + value + " in " + array);
    }
}
