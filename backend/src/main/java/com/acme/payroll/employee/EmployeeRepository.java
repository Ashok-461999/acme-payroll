package com.acme.payroll.employee;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Serialises concurrent salary changes for one employee. This avoids two requests
     * both deciding they are writing the latest salary.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select e from Employee e
            where (:search = '' or lower(e.employeeCode) like concat('%', :search, '%')
                or lower(concat(e.firstName, ' ', e.lastName)) like concat('%', :search, '%')
                or lower(e.email) like concat('%', :search, '%'))
              and (:department is null or e.department = :department)
              and (:country = '' or e.country = :country)
              and (:currency = '' or e.currency = :currency)
            """)
    Page<Employee> search(@Param("search") String search,
                          @Param("department") Department department,
                          @Param("country") String country,
                          @Param("currency") String currency,
                          Pageable pageable);

    @Query("""
            select e.currency as currency,
                   count(e) as headcount,
                   sum(e.currentSalary) as totalSalary,
                   min(e.currentSalary) as minSalary,
                   max(e.currentSalary) as maxSalary
            from Employee e
            group by e.currency
            order by e.currency
            """)
    List<CurrencyAggregate> summarizeByCurrency();

    @Query("""
            select e.currency as currency,
                   e.department as department,
                   count(e) as headcount,
                   sum(e.currentSalary) as totalSalary
            from Employee e
            group by e.currency, e.department
            order by e.currency, e.department
            """)
    List<DepartmentAggregate> summarizeByCurrencyAndDepartment();

    @Query("""
            select e.currency as currency,
                   e.country as country,
                   count(e) as headcount,
                   sum(e.currentSalary) as totalSalary
            from Employee e
            group by e.currency, e.country
            order by e.currency, e.country
            """)
    List<CountryAggregate> summarizeByCurrencyAndCountry();

    interface CurrencyAggregate {
        String getCurrency();

        Long getHeadcount();

        BigDecimal getTotalSalary();

        BigDecimal getMinSalary();

        BigDecimal getMaxSalary();
    }

    interface DepartmentAggregate {
        String getCurrency();

        Department getDepartment();

        Long getHeadcount();

        BigDecimal getTotalSalary();
    }

    interface CountryAggregate {
        String getCurrency();

        String getCountry();

        Long getHeadcount();

        BigDecimal getTotalSalary();
    }
}
