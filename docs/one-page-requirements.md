# ACME Salary Management — One-Page Requirements

## Goal

Replace ACME's spreadsheet-based salary tracking with a web application that lets one HR
manager maintain records for 10,000 employees across countries and understand how the
organisation pays people.

## Primary user

An HR manager working with the entire employee population daily. This is an internal
back-office tool, not employee self-service.

## In scope

- **Employee directory:** server-side pagination, search by code/name/email, filter by
  department, country, and currency, and sort by the visible columns.
- **Employee records:** create an employee with an initial salary; edit mutable profile
  fields; view profile, current salary, and complete salary history.
- **Salary changes:** append an amount, effective date, reason, and optional note. The
  current salary and effective date update in the same transaction.
- **Analytics:** headcount, total payroll, average, minimum, and maximum salary per
  currency; department and country breakdowns for a selected currency.
- **Demo data:** a deterministic 10,000-employee seed with realistic names, departments,
  countries, currencies, hire dates, and salary history.
- **Delivery:** PostgreSQL + Flyway, automated tests, a single production jar, Docker
  Compose, a live Render + Neon deployment, and the documentation listed in the
  submission checklist.

## Rules

- Annual gross salary is stored as `NUMERIC(15,2)` / `BigDecimal`, never a float.
- Employee code and email are unique and normalised before comparison.
- Employee code, hire date, and salary currency are immutable after creation.
- Salary effective dates must be on/after the hire date, strictly after the current
  salary's effective date, and not in the future.
- Salary history is append-only; one record per employee per effective date.
- Reports never add different currencies together because no FX policy exists.
- Identical create/change retries return the existing resource with `200` and
  `Idempotent-Replay: true`; conflicting retries return `409`.
- Errors use one JSON shape with field-level validation details.

## Out of scope

Authentication and roles, payroll processing/tax/deductions, currency conversion,
employee deletion, approval workflows, bulk import/export, and future-dated raises.
Each is excluded because the brief provides one trusted HR persona and no policy for
those concerns; see [requirements.md](requirements.md) for the reasoning.

## Acceptance

1. A clean start creates exactly 10,000 employees with consistent current salaries and
   history.
2. The HR manager can search/filter/sort, create an employee, edit their profile, add a
   salary change, and see it reflected in history and analytics.
3. Invalid identities, amounts, dates, requests, and missing employees return clear 4xx
   responses without corrupting data.
4. Exact retries do not duplicate employees or salary records.
5. Backend and frontend tests pass; the production build serves the SPA and API from one
   Spring Boot application.
