# Demo Video Script

Target length: 3–5 minutes. Record at 1440×900 with browser zoom at 100%. Use the
deployed URL, not localhost. Keep the cursor deliberate and pause briefly between
sections.

## 1. Introduction (20 seconds)

> "This is ACME People, a salary management application for one HR manager and 10,000
> employees across 10 countries. It uses Spring Boot, PostgreSQL, and Angular, and the
> production build runs as one application."

Show the dashboard.

## 2. Payroll overview (45 seconds)

- Show the KPI cards: 10,000 employees, 10 currencies, and the selected currency's total
  payroll, average, minimum, and maximum.
- Point out that currency amounts are grouped and never added across currencies.
- Select **INR** and show the department and country breakdowns updating.
- Click **View INR employees** to demonstrate the URL-addressable cross-link.

## 3. Employee directory (60 seconds)

- Search for `EMP-00001` or a name.
- Filter by department and country; show the result count and pagination.
- Sort by salary and then by last name.
- Show that filters, sort, and page are reflected in the URL and survive a refresh.

## 4. Employee profile and salary history (45 seconds)

- Open an employee.
- Show current salary, effective date, profile facts, and history.
- Click **Add salary change**.
- Enter a later date, a higher amount, reason `Promotion`, and a short note.
- Save and show the new current salary plus the new history row.
- Reopen the dialog and repeat the same request to show the idempotent replay response
  (no duplicate record).

## 5. Create/edit employee (45 seconds)

- Open **Add employee**.
- Show currency being derived from country and the initial-salary validation.
- Create a demo employee.
- Open **Edit profile**, change the job title, and save.
- Explain that code, hire date, and currency are immutable so salary history stays valid.

## 6. Engineering evidence (30 seconds)

Show the repository in the editor or terminal:

- `docs/one-page-requirements.md`, `docs/architecture.md`, and `docs/ai-workflow.md`.
- `mvn test` result and `npm test` result.
- The Flyway migrations and 10,000-employee seed.
- The AWS deployment resources in `infra/` and `scripts/deploy-aws.ps1`.

## 7. Close (15 seconds)

> "The app is deployed at the URL in the submission, the repository is public, and the
> README documents local, Docker, Render, and AWS deployment. Thank you."

## Pre-record checklist

- Seed is complete and the dashboard shows 10,000 employees.
- Browser console has no errors.
- No localhost URLs or terminal secrets are visible.
- Use a fresh browser profile or hide unrelated tabs/bookmarks.
- Verify the recording includes the public URL in the address bar.
