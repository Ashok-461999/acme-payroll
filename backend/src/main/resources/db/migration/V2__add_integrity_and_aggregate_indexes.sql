ALTER TABLE employee
    ADD CONSTRAINT chk_employee_salary_after_hire
        CHECK (current_salary_effective_date >= hire_date);

CREATE INDEX idx_employee_name_sort
    ON employee (last_name, first_name, id);

CREATE INDEX idx_employee_currency_department
    ON employee (currency, department);

CREATE INDEX idx_employee_currency_country
    ON employee (currency, country);
