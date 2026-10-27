package com.acme.payroll.employee;

import com.acme.payroll.common.dto.PageResponse;
import com.acme.payroll.employee.dto.CreateEmployeeRequest;
import com.acme.payroll.employee.dto.EmployeeResponse;
import com.acme.payroll.employee.dto.UpdateEmployeeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return employeeService.search(search, department, country, currency, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable("id") Long id) {
        return employeeService.getById(id);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        return ResponseEntity
                .created(URI.create("/api/employees/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable("id") Long id,
                                   @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(id, request);
    }
}
