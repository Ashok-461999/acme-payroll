# Employee Salary Management — Requirements

## Goal

Replace ACME's spreadsheet-based salary tracking with a web application that lets one HR
manager maintain salary data for 10,000 employees across multiple countries and answer
questions about how the organisation pays people.

## Primary user

HR Manager. This is an internal back-office tool, so the product is optimised for fast,
reliable data entry and clear reporting rather than employee self-service.

## In scope

### Employee directory

- View employees with server-side pagination (20 per page by default).
- Search by employee code, name, or email.
- Filter by department, country, and salary currency.
- Sort by code, name, department, country, currency, hire date, or current salary.
- Create an employee with their first salary.
- Edit non-financial employee details: name, email, department, job title, and country.
- View an employee profile and their complete salary history.

### Salary management

- Record an initial salary when an employee is created.
- Record salary changes with an amount, effective date, reason, and optional note.
- Keep every prior salary record; salary history is append-only through the application.
- Show the current salary and the effective date of that salary.

### Analytics

- Headcount and payroll totals by currency.
- Average, minimum, and maximum salary by currency.
- Department and country breakdowns for a selected currency.
- All monetary aggregates are grouped by ISO 4217 currency. Amounts in different
  currencies are never added together because the application has no FX conversion data.

### Data and delivery

- A deterministic seed process creates exactly 10,000 employees for demonstration.
- PostgreSQL stores all data; Flyway owns the schema.
- The Angular application and Spring Boot API can be built and run locally.
- The production build serves the Angular application and API from one Spring Boot jar.
- Deployment artifacts cover Docker Compose and Render + Neon; AWS Elastic Beanstalk +
  RDS remains an optional, unprovisioned fallback.
- Automated backend and frontend tests cover the rules described below.

## Domain rules

### Employee identity

- `employeeCode` and `email` are unique. Both are normalised (code upper-case, email
  lower-case) so duplicates that differ only by case are rejected.
- Employee code, hire date, and salary currency are immutable after creation. Changing
  any of them would invalidate existing salary history, so those corrections are out of
  scope for this version.

### Money and currency

- Salary amounts represent annual gross pay in the employee's salary currency.
- Salary amounts use `BigDecimal` and `NUMERIC(15,2)` database columns.
- Amounts must be positive, with at most two decimal places.
- Every employee has one explicit ISO 4217 salary currency. All of their salary records
  use that currency.
- Reports group by currency; there is no cross-currency total.

### Salary effective dates

- An initial salary's effective date must be on or after the hire date and not in the
  future.
- A salary change must be effective strictly after the current salary's effective date
  and not in the future.
- Two salary records for the same employee cannot share an effective date. This is
  enforced by a unique database constraint as well as the service rule.
- Because dates strictly increase, salary periods cannot overlap. A change may be
  recorded late with an effective date in the past, provided it remains after the
  current record and is not earlier than the hire date. Existing records are never
  edited, deleted, or reordered, and future-dated changes are not supported.

### Integrity

- Primary keys, foreign keys, uniqueness, positive-amount checks, and required columns
  are enforced in PostgreSQL.
- Salary changes lock the employee row and write the history row plus current-salary
  fields in one transaction, preventing lost updates and inconsistent current values.
- Exact retries are safe. Employee creation uses the employee code as its natural key:
  an identical create returns the original employee with `200` and an
  `Idempotent-Replay: true` header; a differing payload with the same code is `409`.
  Salary changes use `(employee, effective date, amount, reason, note)` as their natural
  key: an exact replay returns the original record with `200` and
  `Idempotent-Replay: true`, while a different change on the same effective date is
  rejected.
- API errors are returned in a consistent JSON shape with field-level validation details.

## Deliberately out of scope

| Item | Why |
| --- | --- |
| Authentication and roles | The assessment describes one HR-manager persona. Adding auth without real identity requirements would add ceremony, not business value. |
| Currency conversion | No exchange-rate source or accounting policy was provided. Mixing currencies would be incorrect, so reports are grouped by currency. |
| Payroll processing, tax, benefits, deductions | This tool manages salary data, not the payroll run. |
| Employee deletion / offboarding | Deletion would destroy salary history. A lifecycle workflow should be designed separately. |
| Approval workflows | Not requested and would complicate every salary write. |
| Bulk import/export | Useful later, but the seed process proves 10k-scale operation. |
| Scheduled or future-dated raises | Requires a scheduler and a policy for unapproved future records. |
| Microservices, Kafka, Redis, CQRS | A modular monolith easily handles 10k employees and keeps the system explainable. |
| Public cloud credentials | Deployment templates/scripts and local production-mode instructions are included; applying them requires account access outside the repository. |

## Acceptance criteria

1. Clean database + application start creates exactly 10,000 employees with consistent
   current-salary values and history.
2. An HR user can create an employee, search/filter the directory, update a salary, and
   see the change in history and analytics.
3. Duplicate codes/emails, invalid salaries, invalid dates, missing employees, and
   malformed requests produce clear 4xx responses.
4. Payroll totals for different currencies remain separate.
5. Retrying an identical employee create or salary change does not duplicate data; it
   returns the original resource with `Idempotent-Replay: true`.
6. Backend tests and frontend tests run from documented commands and pass.
7. The production build serves the Angular application and API from one Spring Boot jar.
