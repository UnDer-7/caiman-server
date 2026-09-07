---
name: end-to-end-testing
description: Use when asked to test, validate, or QA the Caiman backend against a real running instance rather than the unit/integration test suite — manual E2E testing, exercising live endpoints, or checking scheduler/DB behavior locally.
---

# End-to-End Testing

## Overview

Manual E2E testing runs the real app (`just dev-run`), drives it through its own REST API, lets its scheduled jobs (Odin, Huginn) act on the data, and verifies outcomes by querying SQLite directly and reading the app log — no mocks, no test suite. Good for finding real integration bugs (wiring gaps between bounded contexts) that unit tests can't see.

## When to use

- Asked to test/validate/QA a business flow "as it exists today".
- Need to see runtime behavior across module boundaries (e.g. debtor → billing → notification) that only connects at runtime via events/gateways.
- Hunting for real bugs, not verifying already-covered logic.

Not for: `just test-unit` / `just test-integration-jvm` coverage, or static code review of a diff.

## Setup

1. **Check `sqlite3` is installed before anything else** — it's how you'll verify every result later:
   ```bash
   command -v sqlite3
   ```
   Not found → **stop the test now** and tell the user `sqlite3` isn't installed, don't try to work around it (no app boot, no curl calls).
2. **Read before touching anything**: `AGENT.md`, then the relevant `docs/*.md` (`BUSINESS_RULES.md` is the primary reference; also check `HEXAGONAL_ARCHITECTURE.md`, `INVOICE_GENERATION_CYCLE.md`, `MODULE_FLOW.md`, `VALIDATION_STRATEGY.md` depending on the flow). Don't guess request JSON shape — read the `*RequestDto` records under `*/entrypoint/.../payload/request/`, or hit Swagger UI (served at `/` — `CAIMAN_SERVER_OPEN_API_SWAGGER_UI_ENABLE` in `.env`) / OpenAPI docs (`/api-docs`).
3. **Always delete the SQLite database before starting a test run** — it's a disposable local file, and starting from a stale/dirty DB is a common source of confusing, wrong results (leftover rows from a previous run masking or faking the outcome you're checking):
   ```bash
   rm -f ./sqlite_database/database.db
   ```
4. **Start the app in background**, never blocking the session on it:
   ```bash
   just dev-run > /path/to/scratch/dev-run.log 2>&1 &
   ```
   Wait on the log instead of a fixed sleep:
   ```bash
   until grep -q "Started CaimanApplication\|APPLICATION FAILED\|Error starting" dev-run.log 2>/dev/null; do sleep 3; done
   ```
   Port and base path come from `.env` (`CAIMAN_SERVER_PORT`, default `8080`; `CAIMAN_SERVER_ENDPOINTS_PREFIX`, default `/api`) — change either there if needed, it's dev-only and safe to edit.

## Calling the API

Every request needs two headers, checked by a servlet filter *before* any controller — miss them and you get a 400 with header-only errors, easy to mistake for something else:

```bash
curl -s -X POST http://localhost:8080/api/v1/debtors \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: $(uuidgen)" \
  -H "X-Channel: CLAUDE_TEST_AGENT" \
  -d '{ ... }'
```

Chain requests by extracting IDs from responses (`jq -r .id`, or `python3 -c "import sys,json;print(json.load(sys.stdin)['id'])"`).

Date-sensitive fields matter: e.g. `ChargePlan.cycleAnchorDate` must land on a tick that includes **today** (UTC) for Odin to generate anything today — check the actual current date/time (`date -u`) rather than assuming, and see `docs/INVOICE_GENERATION_CYCLE.md` for the ruler math if a generation-timing flow is involved.

## Waiting on the scheduled jobs

Scheduled jobs in this project use JobRunr's `@Recurring`, not Spring's `@Scheduled` — find them with:

```bash
grep -rn "@Recurring" --include=*.java . 2>/dev/null || find . -name "*.java" -not -path "*/build/*" | xargs grep -l "@Recurring"
```

A JobRunr dashboard runs at `http://localhost:8000/dashboard` if you want to watch job runs visually instead of grepping logs.

**If the default cron makes the job too slow to wait on, change it** — e.g. `@Recurring(id = "odin_job", cron = "*/30 * * * * *")` → tighten the interval, restart the app, test, then revert. It's local dev only, freely reversible (`git diff`/`git checkout` after). Still don't block on a fixed `sleep` — poll for the actual condition:

```bash
until sqlite3 sqlite_database/database.db "SELECT COUNT(*) FROM invoice;" | grep -qv '^0$'; do sleep 3; done
```

## Verifying results

Query SQLite directly — it's a plain file, no server round-trip:

```bash
sqlite3 -header -column sqlite_database/database.db "SELECT id, status, amount_due FROM invoice;"
```

Check every table the flow touches, not just the last one in the chain — an upstream wiring bug (e.g. a domain event never published) shows up as a *missing row downstream*, not an error. Also tail the app log for stack traces the API response won't show you (server errors return a fixed generic body by design):

```bash
grep -n "ERROR\|Exception" dev-run.log
```

## Shutting down

`just dev-run` launches a Gradle wrapper process whose actual JVM runs as a **different, child PID** — killing the wrapper alone leaves the app running. Find and kill the real one:

```bash
pgrep -af CaimanApplication
kill <that pid>
```

## Common mistakes

| Symptom | Cause / fix |
|---|---|
| `sqlite3: command not found` | Stop the test, tell the user it's not installed — don't work around it |
| 400 with only header-shaped errors | Missing `X-Correlation-ID` / `X-Channel` |
| Scheduled job never fires on the data you created | `cycleAnchorDate`/similar isn't on today's tick, or a `starts_at`-style floor is in the future |
| Waited a fixed `sleep 30`, DB still empty | Use a poll loop keyed to the actual DB/log condition, not a guessed delay |
| App "still running" after `kill` | You killed the gradle-wrapper PID, not the JVM child — `pgrep -af CaimanApplication` |
| Guessing request JSON shape and getting 400s | Read the `*RequestDto` record source, or use Swagger UI at `/` |
