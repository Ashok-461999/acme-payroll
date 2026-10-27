package com.acme.payroll.salary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    List<SalaryRecord> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);

    Optional<SalaryRecord> findTopByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
}
