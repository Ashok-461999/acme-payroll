# Verification Evidence

Date: 2026-10-28. Environment: Windows 11, Java 21.0.6, Maven 3.9.9, Node 22.22.0,
Angular CLI 20.3.41, PostgreSQL 17.4, Neon PostgreSQL 17.

## Automated tests

| Suite | Command | Result |
| --- | --- | --- |
| Backend | `cd backend; mvn -q test` | **52 tests, 0 failures, 0 errors, 0 skipped** across 7 Surefire suites |
| Frontend | `cd frontend; npm test -- --no-watch --browsers=ChromeHeadless` | **16/16 specs passed** |

The frontend suite includes create-form, salary-dialog, service, app-shell, and the
currency-formatting pipe tests.

## Production build

| Check | Result |
| --- | --- |
| Angular production build | Passed; initial bundle **436.48 kB raw / 118.51 kB transfer**, no budget warning |
| Lazy routes | Dashboard, employee list, employee form, and employee detail emitted as lazy chunks |
| Single jar | `payroll-service-0.0.1-SNAPSHOT.jar`, 55,201,167 bytes |
| Jar contents | `BOOT-INF/classes/static/index.html`, `main-74AO6IBO.js`, and `styles-ZEMRGUKQ.css` present |

## PostgreSQL and seed

Verified against local PostgreSQL 17.4 and the deployed Neon PostgreSQL 17 database:

- `GET /api/analytics/summary` returned `totalEmployees = 10000` on the live deployment.
- The live response contained 10 currencies and 10 countries.
- The seed is deterministic and skipped when employees already exist.
- Ten currencies; per-currency headcount sums to 10,000.
- Dashboard payroll totals and averages agree: for INR, `1,798,590,544 / 1,000 =
  1,798,590.544`, displayed rounded as `INR 1,798,591`.
- Currency and department/country breakdowns are computed by grouped SQL, not in the
  browser.

## Deployed smoke test

Deployment: Render Free service `acme-payroll` in Singapore, built from the repository
Dockerfile and connected to Neon PostgreSQL 17.

| Check | Result |
| --- | --- |
| Dashboard `/` | HTTP 200, 28,487-byte Angular shell |
| Deep link `/employees/1` | HTTP 200; SPA fallback served the Angular shell |
| `/api/analytics/summary` | `totalEmployees = 10000`; 10 currencies; 10 countries |
| Dashboard content | 10,000 employees; USD payroll `95,761,303`; average `95,761` |
| Browser console | No application errors observed during smoke tests |

```powershell
$base = 'https://acme-payroll-a2d1.onrender.com'
$summary = Invoke-RestMethod "$base/api/analytics/summary"
$summary.totalEmployees
$summary.currencies.Count
(Invoke-WebRequest "$base/employees/1" -UseBasicParsing).StatusCode
```

Expected: `10000`, `10`, and `200`.

## Performance checks

- Live employee-list API: 183 ms, 260 ms, and 494 ms across three warm runs.
- Live analytics API: 390 ms, 407 ms, and 499 ms across three warm runs.
- Employee list: paginated SQL, maximum page size 100; browser never loads 10,000 rows.
- Analytics: three grouped queries per dashboard request.
- Seed: JDBC batches; no per-row JPA flush.
- Composite indexes: `(last_name, first_name, id)`, `(currency, department)`, and
  `(currency, country)`.
- Bundle: one shared initial chunk plus four lazy feature chunks.
- Render Free cold start: the service sleeps after 15 minutes idle; warm up the live URL
  before recording the demo.

## Screenshots

Captured from the deployed application and visually verified:

1. [Dashboard at 1440x900](screenshots/dashboard-desktop.png), including USD selected and
   department/country breakdowns.
2. [Employee directory at 1440x900](screenshots/employee-list-desktop.png), with filters
   and sort controls visible.
3. [Employee detail at 1440x900](screenshots/employee-detail-desktop.png), with salary
   history.
4. [Dashboard at 390x844](screenshots/dashboard-mobile.png), verified with no horizontal
   overflow (`document.documentElement.scrollWidth = 390`).

## Known test limitation

Automated backend integration tests use H2 in PostgreSQL compatibility mode for speed.
The application itself is verified against PostgreSQL 17.4 locally and Neon PostgreSQL 17
in the live deployment, including Flyway and the 10,000-employee seed.
