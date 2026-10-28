package com.acme.payroll.api;

import com.acme.payroll.common.exception.ConflictException;
import com.acme.payroll.common.exception.ResourceNotFoundException;
import com.acme.payroll.employee.Department;
import com.acme.payroll.employee.EmployeeController;
import com.acme.payroll.employee.EmployeeService;
import com.acme.payroll.employee.dto.EmployeeCreationResult;
import com.acme.payroll.employee.dto.EmployeeResponse;
import com.acme.payroll.salary.SalaryChangeResult;
import com.acme.payroll.salary.SalaryController;
import com.acme.payroll.salary.SalaryReason;
import com.acme.payroll.salary.SalaryService;
import com.acme.payroll.salary.dto.SalaryRecordResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeController.class, SalaryController.class})
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private SalaryService salaryService;

    @Test
    void createsEmployeeAndReturnsLocation() throws Exception {
        EmployeeResponse created = new EmployeeResponse(
                1L, "EMP-1001", "Asha", "Patel", "Asha Patel", "asha@example.com",
                Department.ENGINEERING, "Engineer", "IN", "INR",
                LocalDate.now().minusYears(1), new BigDecimal("100000.00"),
                LocalDate.now().minusYears(1), Instant.now(), Instant.now()
        );
        when(employeeService.create(any())).thenReturn(new EmployeeCreationResult(created, true));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeBody())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/employees/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeCode").value("EMP-1001"))
                .andExpect(jsonPath("$.email").value("asha@example.com"))
                .andExpect(jsonPath("$.currentSalary").value(100000.00));
    }

    @Test
    void returnsOkForIdempotentCreateReplay() throws Exception {
        EmployeeResponse existing = new EmployeeResponse(
                1L, "EMP-1001", "Asha", "Patel", "Asha Patel", "asha@example.com",
                Department.ENGINEERING, "Engineer", "IN", "INR",
                LocalDate.now().minusYears(1), new BigDecimal("100000.00"),
                LocalDate.now().minusYears(1), Instant.now(), Instant.now()
        );
        when(employeeService.create(any()))
                .thenReturn(new EmployeeCreationResult(existing, false));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeBody())))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void returnsNotFoundForUnknownApiPath() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void rejectsInvalidEmployeeRequestWithFieldErrors() throws Exception {
        Map<String, Object> body = employeeBody();
        body.put("employeeCode", "");
        body.put("email", "not-an-email");
        body.put("initialSalary", new BigDecimal("0.00"));
        body.put("hireDate", LocalDate.now().plusDays(1).toString());
        body.put("initialSalaryEffectiveDate", LocalDate.now().plusDays(1).toString());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.employeeCode").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.initialSalary").exists())
                .andExpect(jsonPath("$.fieldErrors.hireDate").exists())
                .andExpect(jsonPath("$.fieldErrors.initialSalaryEffectiveDate").exists());

        verifyNoInteractions(employeeService);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Request body is missing, malformed, or contains an unsupported value"
                ));
    }

    @Test
    void returnsConflictForDuplicateIdentity() throws Exception {
        when(employeeService.create(any()))
                .thenThrow(new ConflictException(
                        "employeeCode", "Employee code already exists: EMP-1001"
                ));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Employee code already exists: EMP-1001"))
                .andExpect(jsonPath("$.fieldErrors.employeeCode")
                        .value("Employee code already exists: EMP-1001"));
    }

    @Test
    void returnsNotFoundForMissingEmployee() throws Exception {
        when(employeeService.getById(99L)).thenThrow(new ResourceNotFoundException("Employee not found: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/employees/99"));
    }

    @Test
    void recordsSalaryChange() throws Exception {
        SalaryRecordResponse response = new SalaryRecordResponse(
                10L, new BigDecimal("120000.00"), "INR", LocalDate.now(),
                SalaryReason.PROMOTION, "Promotion", Instant.now()
        );
        when(salaryService.changeSalary(eq(1L), any()))
                .thenReturn(new SalaryChangeResult(response, true));

        mockMvc.perform(post("/api/employees/1/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(120000.00))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.reason").value("PROMOTION"));
    }

    @Test
    void rejectsInvalidSalaryRequest() throws Exception {
        Map<String, Object> body = salaryBody();
        body.put("amount", new BigDecimal("-10.00"));
        body.put("effectiveDate", LocalDate.now().plusDays(1).toString());
        body.remove("reason");

        mockMvc.perform(post("/api/employees/1/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.effectiveDate").exists())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());

        verifyNoInteractions(salaryService);
    }

    @Test
    void returnsNotFoundWhenChangingSalaryForMissingEmployee() throws Exception {
        when(salaryService.changeSalary(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Employee not found: 99"));

        mockMvc.perform(post("/api/employees/99/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: 99"));
    }

    private Map<String, Object> employeeBody() {
        LocalDate hireDate = LocalDate.now().minusYears(1);
        Map<String, Object> body = new HashMap<>();
        body.put("employeeCode", "EMP-1001");
        body.put("firstName", "Asha");
        body.put("lastName", "Patel");
        body.put("email", "asha@example.com");
        body.put("department", "ENGINEERING");
        body.put("jobTitle", "Engineer");
        body.put("country", "IN");
        body.put("currency", "INR");
        body.put("hireDate", hireDate.toString());
        body.put("initialSalary", new BigDecimal("100000.00"));
        body.put("initialSalaryEffectiveDate", hireDate.toString());
        return body;
    }

    private Map<String, Object> salaryBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", new BigDecimal("120000.00"));
        body.put("effectiveDate", LocalDate.now().toString());
        body.put("reason", "PROMOTION");
        body.put("note", "Promotion");
        return body;
    }
}
