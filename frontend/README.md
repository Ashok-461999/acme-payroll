# ACME People Frontend

Angular 20 standalone application for the ACME salary management project. It talks to
the Spring Boot API through the `/api` proxy during development.

## Development

Start the backend from `../backend` (`mvn spring-boot:run`), then:

```powershell
npm ci
npm start
```

Open `http://localhost:4200`. `proxy.conf.json` forwards `/api` to `http://localhost:8080`.

## Tests

```powershell
$env:CHROME_BIN = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm test -- --no-watch --browsers=ChromeHeadless
```

The current suite has 15 specs covering the app shell, employee form, salary dialog,
services, and currency formatting.

## Production build

```powershell
npm run build
```

The production bundle is written to `dist/frontend/browser`. The root
`scripts/build-all.ps1` copies it into the backend static resources and packages the
single deployable jar.

## Structure

```text
src/app/core/           API models/services, reference data, money pipe
src/app/features/       dashboard, employee list/form/detail, salary dialog
src/app/app.*           shell, navigation, global accessibility affordances
src/styles.scss         shared spacing tokens and responsive rules
```

See the root [README](../README.md) and [deployment guide](../docs/deployment.md) for the
full project setup and delivery instructions.
