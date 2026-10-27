package com.acme.payroll.salary;

import com.acme.payroll.salary.dto.SalaryChangeRequest;
import com.acme.payroll.salary.dto.SalaryRecordResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{employeeId}/salaries")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping
    public List<SalaryRecordResponse> history(@PathVariable("employeeId") Long employeeId) {
        return salaryService.history(employeeId);
    }

    @PostMapping
    public ResponseEntity<SalaryRecordResponse> changeSalary(
            @PathVariable("employeeId") Long employeeId,
            @Valid @RequestBody SalaryChangeRequest request) {
        SalaryRecordResponse created = salaryService.changeSalary(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
