# Incubyte Submission Checklist

This maps the assessment deliverables to evidence in this repository. Update the status
and URL columns after deployment and recording.

| Requirement | Status | Evidence / next action |
| --- | --- | --- |
| Fully functional software | Local: complete. AWS: pending deployment. | Spring Boot + PostgreSQL + Angular app; 10,000-employee seed; runtime checks recorded in [verification.md](verification.md). |
| Public repository link | Pending | No git remote is configured yet. Create a public GitHub repository, add it as `origin`, and push the commit history. |
| Incremental commits | Complete | Eight commits cover scope/docs, backend APIs, seed, retry safety, frontend, submission evidence, and deployment assets. |
| Publicly deployed application | Pending | AWS Elastic Beanstalk + RDS deployment is automated in [../infra/aws-elastic-beanstalk.yaml](../infra/aws-elastic-beanstalk.yaml) and [../scripts/deploy-aws.ps1](../scripts/deploy-aws.ps1); requires AWS credentials and budget approval. |
| Demo video | Pending | Script: [demo-script.md](demo-script.md). Record after the public URL is live. |
| One-page requirements document | Complete | [one-page-requirements.md](one-page-requirements.md). |
| Architecture / design notes | Complete | [architecture.md](architecture.md), including data model, API, transaction boundaries, and trade-offs. |
| AI prompts / workflow notes | Complete | [ai-workflow.md](ai-workflow.md). |
| Trade-off explanations | Complete | [architecture.md](architecture.md#key-decisions-and-trade-offs) and [requirements.md](requirements.md#deliberately-out-of-scope). |
| Performance considerations | Complete; production timing pending | [architecture.md](architecture.md#performance-approach), [verification.md](verification.md), and the repeatable API timing check in [../README.md](../README.md#local-performance-check). |
| Screenshots | Partial | UI verified locally; capture dashboard, employee list, detail/history, salary dialog, and form at desktop and mobile widths after deployment. |
| README / setup instructions | Complete | [../README.md](../README.md); frontend-specific notes in [../frontend/README.md](../frontend/README.md). |
| Tests | Complete | 52 backend tests and 15 frontend specs; commands and evidence in [verification.md](verification.md). |
| Production build | Complete | Single jar includes the Angular production bundle and SPA deep-link forwarding. |

## Final submission sequence

1. Rebuild the production jar and rerun both test suites.
2. Commit the remaining work in reviewable slices.
3. Create the public GitHub repository and push.
4. Deploy with `scripts/deploy-aws.ps1` and verify the health/API/SPA routes.
5. Capture screenshots and API timings against the deployed URL.
6. Record the demo video using [demo-script.md](demo-script.md).
7. Add the public URL, video link, and repository link to the submission message.
