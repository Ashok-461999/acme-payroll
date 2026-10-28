# ACME Employee Salary Management

A full-stack salary management application for one HR manager and 10,000 employees.
The backend is a Spring Boot modular monolith on PostgreSQL; the frontend is an Angular
single-page application. A deterministic seeder provides demonstration data.

## Features

- Server-side employee search, filtering, sorting, and pagination.
- Employee creation with an initial salary and append-only salary history.
- Salary changes with validation, current-salary read model, and safe retries.
- Per-currency analytics: headcount, payroll total, average, minimum, and maximum.
- Responsive Angular Material UI with URL-addressable list filters and deep links.
- Flyway-owned schema, database constraints, 10,000-employee seed, and automated tests.
- One production jar can serve both the API and the Angular application.
- Live Render Free deployment backed by Neon PostgreSQL; Docker Compose and optional AWS artifacts.

## Live demo

- Application: <https://acme-payroll-a2d1.onrender.com>
- Repository: <https://github.com/Ashok-461999/acme-payroll>

The Render Free service sleeps after 15 minutes without traffic. The first request can
take about a minute while it wakes; subsequent requests are fast.

Screenshots are in [docs/screenshots](docs/screenshots):
[dashboard](docs/screenshots/dashboard-desktop.png),
[employee directory](docs/screenshots/employee-list-desktop.png),
[employee detail](docs/screenshots/employee-detail-desktop.png), and
[mobile dashboard](docs/screenshots/dashboard-mobile.png).

## Repository layout

```text
backend/    Spring Boot API, tests, Flyway migrations
frontend/   Angular application and Karma/Jasmine tests
docs/       Requirements, architecture, deployment, demo, and AI workflow
infra/      Optional AWS Elastic Beanstalk + RDS CloudFormation template
scripts/    Build and optional AWS deployment scripts
```

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 17 (database `employee_salary`; defaults: `postgres` / `postgres`)

Create the database once:

```powershell
psql -U postgres -c "CREATE DATABASE employee_salary;"
```

## Run in development

Run the backend and frontend in separate terminals.

### Terminal A: backend

```powershell
cd C:\Users\ashok\IncubyteProject\backend
mvn spring-boot:run
```

Flyway applies the schema on startup. On the first run, the seeder inserts 10,000
employees. The API is available at `http://localhost:8080/api`.

Check readiness (the first seed can take a few seconds):

```powershell
Invoke-RestMethod http://localhost:8080/api/analytics/summary | Select-Object totalEmployees
```

### Terminal B: frontend

```powershell
cd C:\Users\ashok\IncubyteProject\frontend
npm ci
npm start
```

Open `http://localhost:4200`. The Angular dev server proxies `/api` to port 8080.

## Build one production jar

From the repository root:

```powershell
cd C:\Users\ashok\IncubyteProject
.\scripts\build-all.ps1
```

The script runs `npm ci`, builds the Angular production bundle, copies it into
`backend/src/main/resources/static`, and packages the Spring Boot jar. Run it with:

```powershell
java -jar .\backend\target\payroll-service-0.0.1-SNAPSHOT.jar
```

Open `http://localhost:8080`. Deep links such as `/employees/42` are forwarded to the
Angular router by `SpaForwardFilter`; `/api` paths always return API responses.

## Deployment

The submitted demo runs on **Render Free** with a **Neon Free PostgreSQL 17** database.
`render.yaml` defines the Docker web service and expects `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` to be supplied as secret environment variables. No database credential is
stored in this repository.

- **Docker:** `docker compose up --build` starts PostgreSQL 17 and the app.
- **Render + Neon:** create a Neon project, create a Render Blueprint from this repository,
  and set the three database secrets when prompted. The live service was smoke-tested at
  <https://acme-payroll-a2d1.onrender.com>.
- **AWS (optional):** `.\scripts\deploy-aws.ps1 -Region ap-south-1` provisions an
  Elastic Beanstalk single-instance environment and a private RDS PostgreSQL database
  through `infra/aws-elastic-beanstalk.yaml`. It is not used for the submitted demo.
  See [docs/deployment.md](docs/deployment.md) for details and cleanup.

If PowerShell blocks local scripts:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-all.ps1
```

## Tests

Backend (unit, controller, H2 integration, and seeder tests):

```powershell
cd C:\Users\ashok\IncubyteProject\backend
mvn test
```

Frontend (headless Chrome):

```powershell
cd C:\Users\ashok\IncubyteProject\frontend
$env:CHROME_BIN = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm test -- --no-watch --browsers=ChromeHeadless
```

Frontend production build only:

```powershell
npm run build
```

## Seed data

The seed is deterministic (fixed random seed), creates exactly 10,000 employees across
10 countries and 10 currencies, and is skipped when employees already exist. Configure
it with `APP_SEED_ENABLED` and `APP_SEED_COUNT`.

To recreate the demo data locally:

```powershell
psql -U postgres -d employee_salary -c "TRUNCATE salary_record, employee RESTART IDENTITY CASCADE;"
```

Then restart the backend. This deletes all local employee data.

Verify consistency:

```powershell
$summary = Invoke-RestMethod http://localhost:8080/api/analytics/summary
$summary.totalEmployees
($summary.currencies | Measure-Object -Property headcount -Sum).Sum
```

Both values should be `10000`.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/employees` | Paginated list |
| POST | `/api/employees` | Create employee + initial salary |
| GET | `/api/employees/{id}` | Employee profile |
| PUT | `/api/employees/{id}` | Update mutable profile fields |
| GET | `/api/employees/{id}/salaries` | Salary history, newest first |
| POST | `/api/employees/{id}/salaries` | Record a salary change |
| GET | `/api/analytics/summary` | Per-currency payroll analytics |

List parameters: `search`, `department`, `country`, `currency`, `page` (zero-based),
`size` (1–100), `sortBy`, `direction` (`asc`/`desc`).

### Retry behavior

- Employee creation uses the normalised employee code as its natural idempotency key.
  An identical retry returns the existing employee with `200 OK` and
  `Idempotent-Replay: true`. Reusing the code with different details returns `409`.
- Salary changes use employee, effective date, amount, reason, and note as their natural
  key. An exact retry returns the existing salary record with `200 OK` and
  `Idempotent-Replay: true`. A different change on the same date is rejected.
- Salary writes lock the employee row so concurrent changes cannot create lost updates.

## Design notes

- Money is `BigDecimal` / `NUMERIC(15,2)` and represents annual gross pay. Every employee
  has one immutable salary currency.
- Salary history is append-only. The employee table keeps a denormalised current salary
  for fast list and analytics reads, updated in the same transaction as history.
- Effective dates must increase. A late-recorded change can use a past date only if it is
  after the latest record; inserting between old records and future scheduling are out
  of scope.
- Reports never add different currencies together because no exchange-rate policy exists.
- The directory is paginated in SQL; the browser never loads all 10,000 rows.
- Search uses `LIKE '%term%'`; at this scale it is fast, and a trigram index can be added
  if the dataset grows.

See [docs/one-page-requirements.md](docs/one-page-requirements.md),
[docs/requirements.md](docs/requirements.md), [docs/architecture.md](docs/architecture.md),
[docs/code-explanations.md](docs/code-explanations.md), and
[docs/submission-checklist.md](docs/submission-checklist.md) for details.

## Local performance check

After starting the backend, warm up once and then run:

```powershell
1..3 | ForEach-Object {
  Measure-Command {
    Invoke-RestMethod 'http://localhost:8080/api/employees?page=0&size=20' | Out-Null
  } | Select-Object TotalMilliseconds
}
```

Repeat with `/api/analytics/summary` to measure the dashboard endpoint.

## Deployed smoke test

The live deployment was verified with:

```powershell
$base = 'https://acme-payroll-a2d1.onrender.com'
$summary = Invoke-RestMethod "$base/api/analytics/summary"
$summary.totalEmployees
$summary.currencies.Count
(Invoke-WebRequest "$base/employees/1" -UseBasicParsing).StatusCode
```

Expected: `10000`, `10`, and `200`. Three warm runs measured the employee list at
183-494 ms and analytics at 390-499 ms.

## Known limitations

- No authentication or role management; the assessment assumes one trusted HR user.
- No currency conversion, payroll processing, tax, approvals, or deletion.
- Automated integration tests use H2 in PostgreSQL mode; PostgreSQL 17 is used for the
  running application, including the deployed Neon database.
- Render Free sleeps after 15 minutes without traffic, so the first request can take
  about a minute. Neon Free also has compute, storage, and data-transfer limits.
- Optional AWS artifacts exist for an always-on demo but were not provisioned.
- The public URL and repository are live; the demo video remains the final submission
  step tracked in [docs/submission-checklist.md](docs/submission-checklist.md).
