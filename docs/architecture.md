# Architecture

## Shape of the system

A modular monolith: one Spring Boot application, one PostgreSQL database, and one Angular
single-page application. This is intentionally the smallest architecture that satisfies
the requirements and remains easy to explain and operate.

```mermaid
flowchart LR
    U[HR Manager] -->|HTTPS| A[Angular SPA]
    A -->|JSON /api| C[Spring MVC controllers]
    C --> V[Bean validation]
    C --> S[Services / transactions]
    S --> R[Spring Data JPA repositories]
    R --> P[(PostgreSQL 17)]
    F[Flyway migrations] --> P
    D[Deterministic seeder] --> P
```

In production mode the Angular build is copied into the Spring Boot jar and served as
static files. In development the Angular CLI dev server proxies `/api` to the backend.

## Backend packages

```text
com.acme.payroll
├── employee     employee aggregate, directory queries, employee service/controller
├── salary       salary history entity, repository, change/history service and DTOs
├── analytics    read-only aggregate queries and dashboard endpoint
├── common       API errors, exception handling, CORS, reference-data validation
└── seed         deterministic 10,000-employee demonstration data
```

Each feature keeps its controller, DTOs, service, repository, and entity together.
Controllers do HTTP work, services own business rules and transaction boundaries, and
repositories own persistence queries. Entities are never serialised by the API.

## Database model

```mermaid
erDiagram
    EMPLOYEE ||--o{ SALARY_RECORD : has
    EMPLOYEE {
        bigint id PK
        varchar employee_code UK
        varchar email UK
        varchar first_name
        varchar last_name
        varchar department
        varchar job_title
        varchar country
        varchar currency
        date hire_date
        numeric current_salary
        date current_salary_effective_date
        timestamptz created_at
        timestamptz updated_at
    }
    SALARY_RECORD {
        bigint id PK
        bigint employee_id FK
        numeric amount
        date effective_date
        varchar reason
        varchar note
        timestamptz created_at
    }
```

`SALARY_RECORD` is the append-only history. `EMPLOYEE.current_salary` and
`current_salary_effective_date` are a deliberate read model so the employee list and
analytics do not need a "latest salary per employee" join on every request. Both are
updated in the same transaction as a new history row, and tests verify they agree.

### Indexes

- Unique indexes on employee code and email enforce identity rules.
- Unique `(employee_id, effective_date)` on salary records prevents duplicate dates and
  makes overlap impossible under the increasing-date rule.
- Indexes on `department`, `country`, `currency`, and `(employee_id, effective_date)`
  support filters, grouping, and history reads.
- Composite indexes on `(last_name, first_name, id)`, `(currency, department)`, and
  `(currency, country)` support stable name ordering and per-currency dashboard
  aggregates without adding indexes for every filter combination.
- Name/email search uses `LIKE '%term%'`; at 10,000 rows a sequential scan is a few
  milliseconds. A trigram index would add operational cost without a measured need.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/employees` | Paginated search and filters |
| POST | `/api/employees` | Create employee + initial salary |
| GET | `/api/employees/{id}` | Employee profile |
| PUT | `/api/employees/{id}` | Edit mutable employee fields |
| GET | `/api/employees/{id}/salaries` | Salary history, newest first |
| POST | `/api/employees/{id}/salaries` | Record a salary change |
| GET | `/api/analytics/summary` | Headcount and payroll grouped by currency, department, country |

List query parameters: `search`, `department`, `country`, `currency`, `page`, `size`,
`sortBy`, `direction`. Pagination is zero-based. `size` is capped at 100.

## Key decisions and trade-offs

### Denormalised current salary

The alternative was to derive current salary from history with a correlated subquery or
a window function on every list and analytics request. That is more "normalised" but
makes the most common read paths harder to read and paginate. Since salary writes are
centralised in one service method with a row lock and one transaction, the denormalised
fields cannot drift through normal application use. The seeder and an integration test
also assert consistency. If salary history became editable, this choice would be
revisited.

### Strictly increasing effective dates instead of interval editing

A salary record is a point-in-time change. Each record's period runs until the next
record's date. Requiring each new date to be strictly after the latest date makes
overlaps impossible and keeps the write path simple. A late-recorded raise may use a past
date as long as it is still after the latest existing record. Inserting a change between
existing records, editing history, and future scheduling remain out of scope because
they require an explicit correction and audit workflow.

### Idempotency and concurrency

The write paths use business natural keys rather than a separate idempotency-key table:

- Employee creation is keyed by normalised employee code. A retry whose fields, initial
  salary, and effective date still match the stored employee returns the original
  employee with `200 OK` and `Idempotent-Replay: true`. A same-code request with
  different details is a real conflict (`409`), so a stale retry cannot silently
  overwrite or ignore newer data.
- Salary changes are keyed by employee, effective date, amount, reason, and normalised
  note. The exact retry is recognised after the employee row is locked and returns the
  existing salary record with `200 OK` and `Idempotent-Replay: true`. The same date with
  a different payload is rejected, preserving the one-record-per-date rule.

A database unique constraint is the final guard against duplicate identities and salary
dates. In a simultaneous employee-create race, one transaction wins and the other gets a
clear `409`; no duplicate is written. Salary changes additionally take a pessimistic row
lock so two concurrent changes cannot both choose the same next effective date or
overwrite `current_salary` independently.

### No currency conversion

No exchange-rate source or accounting policy exists. Reports therefore group by
currency and show per-currency totals. Adding a conversion layer later would require a
rate source, effective dates for rates, and an audit policy.

### Server-side pagination and filtering

The directory never loads 10,000 rows into the browser. The API applies filters,
ordering, and pagination in PostgreSQL through Spring Data JPA. The UI keeps filter
state in the URL so a page can be bookmarked or refreshed.

### H2 for automated integration tests

Repository/API integration tests run against H2 in PostgreSQL compatibility mode so the
suite is fast and does not require Docker or a local database. The real application is
verified separately against PostgreSQL 17, including Flyway migrations and the 10k seed.
This trade-off is documented rather than hidden; a CI pipeline could add a PostgreSQL
service container for a second integration pass.

### Single deployable jar

For the assessment demo, the Angular production build is copied into
`backend/src/main/resources/static` and served by Spring Boot. This avoids CORS and a
separate web server in the deployed artifact. `SpaForwardFilter` forwards non-API GET
routes such as `/employees/42` to `index.html` so browser refreshes work; `/api`
requests never fall through to the SPA. During development, `ng serve` uses a proxy to
keep hot reload.

## Performance approach

- Seed inserts use JDBC batches, not 10,000 individual JPA flushes.
- The list query is paginated and backed by simple indexes.
- Analytics perform three grouped SQL queries, not one query per employee.
- The UI debounces text search and requests only one page at a time.
- The README includes a repeatable timing check for the local environment.

No cache, message broker, or distributed component is needed at this scale.
