package com.acme.payroll.salary;

import com.acme.payroll.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "salary_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_salary_employee_effective_date",
                columnNames = {"employee_id", "effective_date"}
        )
)
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalaryReason reason;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SalaryRecord() {
        // Required by JPA.
    }

    public SalaryRecord(Employee employee,
                        BigDecimal amount,
                        LocalDate effectiveDate,
                        SalaryReason reason,
                        String note) {
        this.employee = employee;
        this.amount = amount;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.note = note;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public SalaryReason getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
