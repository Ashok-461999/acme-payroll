# AI Workflow

## How AI was used

AI was used as a pair-programming accelerator, not as an unreviewed code generator:

1. **Requirement decomposition.** The brief was turned into explicit domain rules,
   acceptance criteria, and deliberate exclusions before implementation.
2. **Architecture first.** The modular-monolith shape, data model, transaction boundaries,
   and read-model trade-off were agreed before scaffolding feature code.
3. **Incremental implementation.** Backend slices were built and tested before the
   Angular features that consume them. Commits stayed small enough to review.
4. **Adversarial review.** Edge cases were challenged: duplicate identities, concurrent
   salary changes, same-day changes, currency mixing, pagination stability, malformed
   requests, retries, and SPA deep links.
5. **Verification over assertion.** Claims were checked with tests, builds, the running
   API, and PostgreSQL seed counts rather than assumed from generated code.

## Working rules followed

- Keep the architecture as small as the requirements allow.
- Put business rules in services, not controllers or Angular components.
- Enforce critical invariants in the database as well as the application.
- Prefer natural keys and idempotent retries over hidden duplicate writes.
- Keep write transactions short and lock only the row being changed.
- Make failure states explicit with consistent HTTP status codes and error payloads.
- Test the rules that could cause financial or historical corruption first.
- Document limitations instead of presenting unverified work as complete.

## Verification evidence

The following was exercised locally during implementation:

- Backend Maven suite passing: 52 tests, 0 failures/errors/skips across 7 suites,
  including service, controller, H2 integration, and seeder tests.
- Angular production build passing with lazy-loaded routes and no bundle-budget warning.
- Frontend Karma suite passing headlessly: 15/15 specs.
- PostgreSQL 17 startup with Flyway and the deterministic 10,000-employee seed.
- API checks: total employees 10,000; 10 currencies; headcount sum 10,000; search by
  `EMP-00001`; engineering filter 1,034; GBP filter 1,000.
- Single-jar packaging with the Angular assets and the SPA forward filter.
- Public GitHub repository and live Render Free deployment backed by Neon PostgreSQL 17.
- Live smoke test: 10,000 employees; 10 currencies; dashboard and SPA deep links HTTP 200.
- Live warm timings: employee list 183-494 ms; analytics 390-499 ms.
- Desktop and mobile screenshots captured and visually reviewed.
- Docker, Render + Neon, GitHub Actions, and optional AWS Elastic Beanstalk + RDS
  artifacts added; AWS was not provisioned for the submitted demo.

See [verification.md](verification.md) for commands, bundle sizes, and the deployed
smoke-test evidence.

## Known gaps / honest limitations

- The automated integration suite uses H2 in PostgreSQL compatibility mode. The real
  application was verified against PostgreSQL, but CI does not yet provision PostgreSQL.
- The public repository and live Render URL are complete. The demo video is the
  remaining submission step in [submission-checklist.md](submission-checklist.md).
- Render Free sleeps after 15 minutes idle, so the first review request may be slow.
- Authentication, FX conversion, payroll processing, approvals, historical correction,
  and future-dated raises are intentionally out of scope.
- The idempotency design handles retries by natural key. A simultaneous employee-create
  race is resolved by the unique constraint with one request receiving `409`; it does not
  wait and replay the winner's response.

## Why this approach is appropriate for the assessment

The result is deliberately explainable: a reviewer can follow one request from Angular
through the controller, service, transaction, and SQL, then see the same rule asserted in
a focused test. The system is small enough to operate as one jar and strong enough to
protect salary history and currency correctness under retries and concurrent writes.
