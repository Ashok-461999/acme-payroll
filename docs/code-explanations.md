# Code Walkthrough

This document explains where the important behaviour lives and why the implementation is
structured this way.

## Request flow

```text
Angular service -> /api proxy -> Spring MVC controller -> DTO validation
                -> service transaction -> repositories -> PostgreSQL
                -> response DTO -> Angular signal/state -> Material UI
```

Controllers only translate HTTP. Business rules, transaction boundaries, and locking live
in services. Entities are never returned directly; response records define the API shape.

## Backend

### Employee creation

`EmployeeService.create` (`backend/src/main/java/com/acme/payroll/employee/EmployeeService.java`)
is the most important write path:

1. Normalise the employee code and email, and validate ISO country/currency.
2. Validate the hire date and initial salary date.
3. Look up the employee by normalised code.
4. If it exists and the submitted representation still matches, return
   `EmployeeCreationResult(..., false)` so the controller responds `200` with
   `Idempotent-Replay: true`.
5. If it exists with different data, throw `ConflictException` (`409`).
6. Check email uniqueness, create the employee and initial salary record, and return
   `created = true` (`201`).

`EmployeeCreationResult` is a service-level result, not an entity. It keeps the
"created vs replayed" decision out of the controller and out of the persisted model.

### Salary change

`SalaryService.changeSalary`:

1. Loads the employee with `findByIdForUpdate`, taking a PostgreSQL row lock.
2. Rejects future dates and dates before the hire date.
3. If the date and amount match the current salary state, loads the latest history row.
   If date, amount, reason, and normalised note also match, returns the existing record
   with `created = false`.
4. Rejects a second change on the same effective date with a clear message.
5. Rejects dates that do not advance the history.
6. Inserts the history row and updates `currentSalary` plus
   `currentSalaryEffectiveDate` in the same transaction.

The lock prevents two concurrent requests from both believing they are writing the next
salary. The unique `(employee_id, effective_date)` constraint is the database backstop.
`SalaryChangeResult` carries the created/replayed flag to the controller.

### Denormalised current salary

`Employee.currentSalary` and `currentSalaryEffectiveDate` are a read model. List and
analytics queries can order and aggregate without joining the latest history row for
every employee. The write path is centralised in `SalaryService`, so the history row and
read model cannot diverge through normal application use. The seeder and integration
tests assert consistency.

### Error handling

`GlobalExceptionHandler` maps domain and HTTP exceptions to one `ApiError` shape without
leaking stack traces. It covers validation, malformed JSON, conflicts, missing resources,
unknown API paths, unsupported methods/media types, and unexpected failures. Database
constraint violations become `409` because they represent a conflict with stored data.

### Production serving

`SpaForwardFilter` forwards non-API `GET`/`HEAD` routes without a file extension to
`index.html`. Angular then handles routes such as `/employees/42`. `/api` paths are
excluded, so unknown API endpoints still receive the JSON `404` from the exception
handler instead of an HTML page.

### Schema and seed

`V1__create_employee_and_salary_tables.sql` creates the tables, unique constraints,
positive-value checks, foreign key, and base indexes.
`V2__add_integrity_and_aggregate_indexes.sql` adds the salary-after-hire check and
composite indexes for name ordering and dashboard aggregates.

`DataSeeder` uses JDBC batches and a fixed random seed. It skips when employees already
exist, which prevents duplicate demo data across restarts.

## Frontend

- `core/employee.service.ts` and `core/analytics.service.ts` are the only HTTP boundary.
- `app.routes.ts` lazy-loads each feature, keeping the initial bundle small.
- `employee-list` treats URL query parameters as the source of truth for search, filters,
  sorting, and pagination. Refreshing or sharing a URL restores the same view.
- `employee-form` handles create and edit with different guarantees: currency is locked
  on edit because salary history is currency-specific.
- `employee-detail` renders the salary timeline and compares each record with the prior
  one, while the salary dialog enforces the same date rules as the backend.
- Material datepickers use `provideNativeDateAdapter()` to avoid a custom date module.

## Tests

- `EmployeeServiceTest` and `SalaryServiceTest` cover normalisation, validation, history
  rules, idempotent replay, conflicts, and locking interactions with mocks.
- `ApiControllerTest` verifies HTTP status codes, error shape, validation, idempotent
  replay responses, and content-type handling.
- `EmployeeApiIntegrationTest` runs real HTTP/service/repository flow against H2 in
  PostgreSQL compatibility mode, including Flyway, salary history, analytics, and
  idempotency.
- `DataSeederIntegrationTest` verifies deterministic counts, consistency, and skip-on-rerun
  behaviour.
- Angular specs cover the shell, HTTP services, employee form, and salary dialog.

H2 is a fast feedback tool, not a replacement for the PostgreSQL verification documented
in the README and architecture notes.
