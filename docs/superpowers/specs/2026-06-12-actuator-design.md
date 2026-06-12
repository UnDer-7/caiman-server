# Design: Spring Boot Actuator — caiman-server

**Date:** 2026-06-12

## Summary

Configure `spring-boot-starter-actuator` in `caiman-app`. Expose only the `/manage/health` endpoint. Health check must validate the database (auto-configured via `DataSourceHealthIndicator`). Actuator paths must bypass the `RequiredHeaderFilterConfig`. Endpoints must appear in Swagger UI.

---

## Scope

- One endpoint exposed: `GET /manage/health`
- DB health auto-configured (no custom code)
- Filter exclusion via `@Value` injection (no new dependency in `caiman-web-support`)
- Swagger via `springdoc.show-actuator=true`
- Integration test validates response structure
- Runtime CURL validation after tests pass

---

## Configuration

### `management.endpoints.web.base-path`
`/manage` — avoids collision with API prefix (`caiman-server.server.endpoints-prefix`).

### `management.endpoints.web.exposure.include`
`health` only.

### `management.endpoint.health.show-details`
`always` — this is a single-admin personal tool with no public access, so full DB details are acceptable.

### `springdoc.show-actuator`
`true` — SpringDoc auto-adds an "actuator" group to Swagger UI.

---

## Filter Exclusion

`RequiredHeaderFilterConfig` (in `caiman-web-support`) builds its `ignoredPaths` at construction time. Add:
- `managementBasePath` (e.g. `/manage`)
- `managementBasePath + "/**"` (e.g. `/manage/**`)

Injected via `@Value("${management.endpoints.web.base-path:/manage}")` as constructor parameter. No new compile-time dependency on `spring-boot-actuator` in `caiman-web-support`.

---

## Database Health

`DataSourceHealthIndicator` is auto-configured by Spring Boot when `DataSource` is present on the classpath (already satisfied by `spring-boot-starter-data-jpa`). No custom `HealthIndicator` needed.

Health response when DB is healthy:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "PostgreSQL", "validationQuery": "isValid()" }
    }
  }
}
```

Health returns `"status": "DOWN"` if DB is unreachable → HTTP 503.

---

## Swagger Integration

`springdoc.show-actuator=true` adds a dedicated group in Swagger UI listing `/manage/health`. Management base-path must match `management.endpoints.web.base-path`.

---

## Files

| Action | File | Change |
|--------|------|--------|
| modify | `caiman-app/build.gradle.kts` | add `spring-boot-starter-actuator` |
| modify | `caiman-shared/web-support/.../RequiredHeaderFilterConfig.java` | inject `@Value` management base path, add to `ignoredPaths` |
| modify | `caiman-app/src/main/resources/application.yaml` | add `management.*` config block |
| modify | `caiman-app/src/test/resources/application.yaml` | add `management.*` config block (same values) |
| create | `caiman-app/src/test/java/.../actuator/ActuatorIT.java` | integration tests for `/manage/health` |

---

## Integration Tests

Class: `ActuatorIT extends IntegrationTestController` — inherits `@IntegrationTest`, PostgreSQL Testcontainer, `WebTestClient`.

Tests:
1. `GET /manage/health` without required headers → `200 OK`
2. Response body `status == "UP"`
3. Response body `components.db.status == "UP"`

---

## Out of Scope

- `info` endpoint (not enough value without additional `InfoContributor` config)
- Separate management port
- Security (`when_authorized`) — single-admin tool, `always` is acceptable
- Liveness/readiness probes (no Kubernetes)
