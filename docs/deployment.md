# Deployment

The application is one Spring Boot jar containing the Angular production bundle. The
same jar is used for local production runs, Docker, and Render. The submitted demo runs
on **Render Free** with a **Neon Free PostgreSQL 17** database.

## Live deployment

| Item | Value |
| --- | --- |
| Application URL | <https://acme-payroll-a2d1.onrender.com> |
| Repository | <https://github.com/Ashok-461999/acme-payroll> |
| Render service | `acme-payroll`, Singapore, Docker, Free plan, auto-deploy enabled |
| Render service id | `srv-dbh1fopsrm7s73f0gr60` |
| Database | Neon project `acme-payroll`, PostgreSQL 17, `aws-ap-southeast-1` |
| Database name | `employee_salary` |
| TLS | Required (`sslmode=require`) |

Render Free sleeps after 15 minutes without traffic. The first request can take about a
minute; warm the URL before a demo or review.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` / `PORT` | `8080` | HTTP port. Render supplies `PORT`. |
| `DB_URL` | local JDBC URL | Full JDBC URL; overrides `DB_HOST`, `DB_PORT`, and `DB_NAME`. |
| `DB_HOST` | `localhost` | PostgreSQL host when `DB_URL` is not set. |
| `DB_PORT` | `5432` | PostgreSQL port when `DB_URL` is not set. |
| `DB_NAME` | `employee_salary` | Database name when `DB_URL` is not set. |
| `DB_USERNAME` | `postgres` | Database user. |
| `DB_PASSWORD` | `postgres` | Database password. |
| `APP_SEED_ENABLED` | `true` | Seed demo employees when the table is empty. |
| `APP_SEED_COUNT` | `10000` | Number of demo employees. |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Only needed when the frontend is served from a different origin. |

The live service uses:

```text
DB_URL=jdbc:postgresql://<neon-host>:5432/employee_salary?sslmode=require
DB_USERNAME=<neon-user>
DB_PASSWORD=<neon-password>
APP_SEED_ENABLED=true
APP_SEED_COUNT=10000
```

Credentials are stored only in Render's environment settings and Neon. They are not in
the repository, screenshots, or documentation.

## Local production jar

```powershell
.\scripts\build-all.ps1
java -jar .\backend\target\payroll-service-0.0.1-SNAPSHOT.jar
```

If `ng serve` is running, `npm ci` can fail with an esbuild `EPERM` error. Stop the dev
server, or build from the already-installed dependencies:

```powershell
.\scripts\build-all.ps1 -SkipInstall
```

Open `http://localhost:8080`. The app applies Flyway migrations and seeds 10,000
employees on the first run. Deep links are handled by `SpaForwardFilter`.

## Docker

```powershell
docker compose up --build
```

Open `http://localhost:8080`. Compose starts PostgreSQL 17 and the application, with
`DB_HOST=db`. The database is stored in the `payroll-db-data` volume.

## Render + Neon

### Reproduce the deployment

1. Create a Neon project and database named `employee_salary`.
2. Copy the direct (non-pooled) connection details and keep TLS enabled.
3. In Render, create a Blueprint from this repository. `render.yaml` defines the Docker
   web service on the Free plan.
4. When Render prompts for the `sync: false` variables, set `DB_URL`, `DB_USERNAME`, and
   `DB_PASSWORD`. Keep `APP_SEED_ENABLED=true` and `APP_SEED_COUNT=10000`.
5. Deploy. The first boot applies Flyway and seeds 10,000 employees; the initial Docker
   build and seed can take several minutes.

`render.yaml` intentionally does not create a Render database. Render's free PostgreSQL
expires after 30 days; Neon is used instead. No credentials are committed to the
Blueprint.

### Verify

```powershell
$base = 'https://acme-payroll-a2d1.onrender.com'
Invoke-RestMethod "$base/api/analytics/summary" | Select-Object totalEmployees
(Invoke-WebRequest "$base/employees/1" -UseBasicParsing).StatusCode
```

Expected: 10,000 employees and HTTP 200 for the deep link.

## AWS (optional, not used for the submitted demo)

The repository retains an optional AWS path for an always-on deployment:
`infra/aws-elastic-beanstalk.yaml` and `scripts/deploy-aws.ps1`. It provisions an
Elastic Beanstalk single-instance Java environment and a private RDS PostgreSQL 17
database in the account's default VPC. Approximate cost is $22-27/month; see the script
and template for prerequisites and cleanup. Do not run it unless you intend to create
billable AWS resources.

## Security notes

- Neon requires TLS; the live JDBC URL uses `sslmode=require`.
- Database credentials live in Render's secret environment variables and Neon. Do not
  commit `.env` files or the generated local deployment password file.
- Render terminates HTTPS for the public URL.
- This is a demo with no authentication. Do not enter real personal or payroll data.
- The optional AWS path uses a private RDS instance; its Beanstalk endpoint is HTTP for
  cost reasons. That path is not provisioned for this submission.
