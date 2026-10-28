# Verification Evidence

Date: 2026-10-28. Environment: Windows 11, Java 21.0.6, Maven 3.9.9, Node 22.22.0,
Angular CLI 20.3.41, PostgreSQL 17.4.

## Automated tests

| Suite | Command | Result |
| --- | --- | --- |
| Backend | `cd backend; mvn -q test` | **52 tests, 0 failures, 0 errors, 0 skipped** across 7 Surefire suites |
| Frontend | `cd frontend; npm test -- --no-watch --browsers=ChromeHeadless` | **15/15 specs passed** |

The frontend suite includes create-form, salary-dialog, service, app-shell, and the
currency-formatting pipe tests.

## Production build

| Check | Result |
| --- | --- |
| Angular production build | Passed; initial bundle **435.98 kB raw / 118.45 kB transfer**, no budget warning |
| Lazy routes | Dashboard, employee list, employee form, and employee detail emitted as lazy chunks |
| Single jar | `payroll-service-0.0.1-SNAPSHOT.jar`, 55,200,739 bytes |
| Jar contents | `BOOT-INF/classes/static/index.html`, `main-3IHB5AXH.js`, and `styles-SEWK2RJN.css` present |

## PostgreSQL and seed

Verified against PostgreSQL 17.4 on 2026-10-28:

- `GET /api/analytics/summary` returned `totalEmployees = 10000`.
- The seed is deterministic and skipped when employees already exist.
- Ten currencies; per-currency headcount sums to 10,000.
- Dashboard payroll totals and averages agree: for INR, `1,798,590,544 / 1,000 =
  1,798,590.544`, displayed rounded as `INR 1,798,591`.
- Currency and department/country breakdowns are computed by grouped SQL, not in the
  browser.

The local runtime check was performed before the final currency-formatting-only change.
The AWS deployment smoke test below must be rerun against the deployed jar.

## Production smoke test (after AWS deployment)

```powershell
$url = aws cloudformation describe-stacks --stack-name acme-payroll-prod `
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text
Invoke-RestMethod "$url/api/analytics/summary" | Select-Object totalEmployees
Invoke-WebRequest "$url/employees" -UseBasicParsing | Select-Object StatusCode
Invoke-WebRequest "$url/employees/1" -UseBasicParsing | Select-Object StatusCode
```

Expected: 10,000 employees; both deep links return HTTP 200 with the Angular shell.

## Performance checks

- Employee list: paginated SQL, maximum page size 100; browser never loads 10,000 rows.
- Analytics: three grouped queries per dashboard request.
- Seed: JDBC batches; no per-row JPA flush.
- Composite indexes: `(last_name, first_name, id)`, `(currency, department)`, and
  `(currency, country)`.
- Bundle: one shared initial chunk plus four lazy feature chunks.

Run the repeatable API timing block in the root README against the deployed URL and
record the median of three warm runs in the submission notes.

## Screenshots to capture

1. Dashboard at 1440×900, including INR selected and department/country breakdowns.
2. Employee list with filters and sort visible.
3. Employee detail with salary history.
4. Salary-change dialog.
5. Create/edit form, including the current-salary panel.
6. Dashboard at 390×844 for mobile responsiveness.

## Known test limitation

Automated backend integration tests use H2 in PostgreSQL compatibility mode for speed.
The application itself is verified against PostgreSQL 17.4, including Flyway and the
10,000-employee seed.
