package com.acme.payroll.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Department department;

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "current_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentSalary;

    @Column(name = "current_salary_effective_date", nullable = false)
    private LocalDate currentSalaryEffectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {
        // Required by JPA.
    }

    public Employee(String employeeCode,
                    String firstName,
                    String lastName,
                    String email,
                    Department department,
                    String jobTitle,
                    String country,
                    String currency,
                    LocalDate hireDate,
                    BigDecimal currentSalary,
                    LocalDate currentSalaryEffectiveDate) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.country = country;
        this.currency = currency;
        this.hireDate = hireDate;
        this.currentSalary = currentSalary;
        this.currentSalaryEffectiveDate = currentSalaryEffectiveDate;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Employee code, hire date and currency are intentionally not editable.
     * Changing them would invalidate the existing salary history.
     */
    public void updateProfile(String firstName,
                              String lastName,
                              String email,
                              Department department,
                              String jobTitle,
                              String country) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.country = country;
    }

    public void updateCurrentSalary(BigDecimal amount, LocalDate effectiveDate) {
        this.currentSalary = amount;
        this.currentSalaryEffectiveDate = effectiveDate;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Department getDepartment() {
        return department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public BigDecimal getCurrentSalary() {
        return currentSalary;
    }

    public LocalDate getCurrentSalaryEffectiveDate() {
        return currentSalaryEffectiveDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
