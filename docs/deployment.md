# Deployment

The application is one Spring Boot jar containing the Angular production bundle. The
same jar is used for local production runs, Docker, Render, and AWS.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` / `PORT` | `8080` | HTTP port. Elastic Beanstalk's nginx forwards to `5000`, so AWS sets `SERVER_PORT=5000`. |
| `DB_URL` | local JDBC URL | Full JDBC URL; overrides the individual DB variables. |
| `DB_HOST` | `localhost` | PostgreSQL host. |
| `DB_PORT` | `5432` | PostgreSQL port. |
| `DB_NAME` | `employee_salary` | Database name. |
| `DB_USERNAME` | `postgres` | Database user. |
| `DB_PASSWORD` | `postgres` | Database password. |
| `APP_SEED_ENABLED` | `true` | Seed demo employees when the table is empty. |
| `APP_SEED_COUNT` | `10000` | Number of demo employees. |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Only needed when the frontend is served from a different origin. |

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

## Render

`render.yaml` defines a Docker web service plus a free PostgreSQL 17 database. Create a
Blueprint in Render, connect the repository, and apply the Blueprint. The service sleeps
on the free plan, so the first request after idle time is slow.

## AWS

### Chosen architecture

**Elastic Beanstalk single-instance environment + RDS PostgreSQL** in the account's
default VPC.

- Elastic Beanstalk runs the Corretto 21 Java SE platform and nginx on one `t3.micro`
  instance.
- RDS runs PostgreSQL 17 on a single-AZ `db.t4g.micro` instance in private-facing mode;
  its security group accepts `5432` only from the Beanstalk instance security group.
- The jar is uploaded to a private S3 bucket; CloudFormation creates the Beanstalk
  application/version/environment, security groups, RDS subnet group, and RDS instance.
- The Beanstalk HTTP endpoint is the demo URL. A single-instance environment has no load
  balancer, so HTTPS needs an ALB, CloudFront distribution, or a reverse proxy plus a
  certificate/domain. That is intentionally not provisioned to keep the demo cost low.

AWS App Runner is not used: AWS has closed App Runner to new customers and is directing
new workloads to ECS. Elastic Beanstalk remains the lowest-complexity way to run this
Java jar without building or publishing a container.

### Cost

Approximate monthly cost in `ap-south-1`, excluding free-tier credits:

| Resource | Approximate cost |
| --- | --- |
| `t3.micro` Beanstalk instance + EBS | $8–10 |
| `db.t4g.micro` RDS + 20 GB gp3 | $13–16 |
| S3 / data transfer | under $1 |
| **Total** | **about $22–27/month** |

Confirm the price in the AWS console for your region/account before deploying. Delete the
CloudFormation stack after the assessment if the environment is no longer needed.

### Prerequisites

- AWS CLI v2 authenticated with permission to manage CloudFormation, EC2, RDS, S3,
  IAM, and Elastic Beanstalk. `aws sts get-caller-identity` must succeed.
- A region with Elastic Beanstalk Corretto 21 and RDS PostgreSQL 17 available.
- A built jar (the script builds it unless `-SkipBuild` is supplied).

If your AWS CLI is not logged in, use your usual SSO/profile flow:

```powershell
aws sso login
# or: aws configure
aws sts get-caller-identity
```

### Deploy

```powershell
cd C:\Users\ashok\IncubyteProject
.\scripts\deploy-aws.ps1 -Region ap-south-1 -StackName acme-payroll-prod
```

If `ng serve` is running, add `-SkipInstall` so the script reuses `frontend/node_modules`
while still rebuilding the production bundle.

The script:

1. builds the single jar (unless `-SkipBuild`);
2. finds the latest Elastic Beanstalk Corretto 21 platform and a current RDS PostgreSQL
   17 engine version;
3. creates a private S3 source bucket and uploads the jar;
4. generates a database password and writes it to `.aws-deploy.local.json` (git-ignored);
5. deploys `infra/aws-elastic-beanstalk.yaml`;
6. prints the Beanstalk URL, RDS endpoint, and smoke-test commands.

The environment is ready when the stack reaches `CREATE_COMPLETE` and the application
returns `200` from `/api/analytics/summary`. The first boot applies Flyway and seeds
10,000 employees, which can take several minutes on a micro instance.

### Smoke test

```powershell
$url = (aws cloudformation describe-stacks --stack-name acme-payroll-prod `
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text)
Invoke-RestMethod "$url/api/analytics/summary" | Select-Object totalEmployees
Invoke-WebRequest "$url/employees" -UseBasicParsing | Select-Object StatusCode
```

`totalEmployees` must be `10000`, and the deep link must return `200` with the Angular
shell.

### Delete

```powershell
aws cloudformation delete-stack --stack-name acme-payroll-prod
aws s3 rb "s3://acme-payroll-src-$((aws sts get-caller-identity --query Account --output text))" --force
```

Deleting the stack deletes the Beanstalk environment and the RDS instance. Export any
data you need first. The generated password file is local only; delete it when finished.

## Security notes

- The database is not publicly accessible; only the Beanstalk security group can reach
  port 5432.
- The database password is generated locally and stored in `.aws-deploy.local.json`, an
  environment variable on the Beanstalk environment, and RDS. This is adequate for a
  demo but should move to AWS Secrets Manager for a real deployment.
- The Beanstalk endpoint is HTTP. Do not enter real personal or payroll data.
- The S3 source bucket blocks public access.
