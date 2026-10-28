# Incubyte Submission Checklist

This maps the assessment deliverables to evidence in this repository.

- **Live application:** <https://acme-payroll-a2d1.onrender.com>
- **Public repository:** <https://github.com/Ashok-461999/acme-payroll>

| Requirement | Status | Evidence / next action |
| --- | --- | --- |
| Fully functional software | Complete | Deployed Spring Boot + Neon PostgreSQL + Angular app; 10,000-employee seed; live checks recorded in [verification.md](verification.md). |
| Public repository link | Complete | <https://github.com/Ashok-461999/acme-payroll> (public, incremental history). |
| Incremental commits | Complete | Commit history covers scope/docs, backend APIs, seed, retry safety, frontend, submission evidence, and deployment. |
| Publicly deployed application | Complete | Render Free: <https://acme-payroll-a2d1.onrender.com>. Database: Neon Free PostgreSQL 17. The free instance sleeps after 15 minutes idle. |
| Demo video | Pending | Script: [demo-script.md](demo-script.md). Record against the live URL. |
| One-page requirements document | Complete | [one-page-requirements.md](one-page-requirements.md). |
| Architecture / design notes | Complete | [architecture.md](architecture.md), including data model, API, transaction boundaries, and trade-offs. |
| AI prompts / workflow notes | Complete | [ai-workflow.md](ai-workflow.md). |
| Trade-off explanations | Complete | [architecture.md](architecture.md#key-decisions-and-trade-offs) and [requirements.md](requirements.md#deliberately-out-of-scope). |
| Performance considerations | Complete | Live warm timings: employee list 183-494 ms; analytics 390-499 ms. See [verification.md](verification.md). |
| Screenshots | Complete | [Dashboard](screenshots/dashboard-desktop.png), [employee list](screenshots/employee-list-desktop.png), [employee detail](screenshots/employee-detail-desktop.png), and [mobile dashboard](screenshots/dashboard-mobile.png). |
| README / setup instructions | Complete | [../README.md](../README.md); frontend-specific notes in [../frontend/README.md](../frontend/README.md). |
| Tests | Complete | 52 backend tests and 15 frontend specs; commands and evidence in [verification.md](verification.md). |
| Production build | Complete | Single jar contains the Angular production bundle; SPA deep links return the app shell. |
| Deployment configuration | Complete | [../render.yaml](../render.yaml) defines the Render Docker service and Neon-backed secret variables; no credentials are committed. |

## Final submission sequence

1. Open the live URL once and confirm the dashboard shows 10,000 employees.
2. Record the demo video using [demo-script.md](demo-script.md).
3. Reply to the Incubyte email with the public repository URL, live URL, and video link.
4. Optionally attach the screenshots and one-page requirements document.
