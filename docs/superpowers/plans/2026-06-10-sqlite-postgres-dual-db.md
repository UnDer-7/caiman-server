# SQLite + PostgreSQL Dual-DB Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow the app to run with either PostgreSQL or SQLite, selected at runtime via `CAIMAN_SERVER_DATABASE_TYPE` env var with no default — user must always declare a choice.

**Architecture:** Exclude `DataSourceAutoConfiguration` and provide a single `DataSourceConfig @Configuration` that reads `CAIMAN_SERVER_DATABASE_TYPE` and creates either a `HikariDataSource` (Postgres) or `SQLiteDataSource` (SQLite) via plain Java `if/else` — no `@ConditionalOnProperty`, GraalVM-safe. A custom `JpaVendorAdapter` bean sets the Hibernate dialect accordingly. Liquibase detects the database type from the `DataSource` connection and applies type-appropriate DDL automatically; no migration changes needed.

**Tech Stack:** Spring Boot 4, Hibernate 7 (`hibernate-community-dialects` for `SQLiteDialect`), `org.xerial:sqlite-jdbc`, `HikariCP` (already in Spring Boot BOM), Liquibase, JUnit 5, Testcontainers.

---

## File Map

| Action | File |
|--------|------|
| Modify | `gradle/libs.versions.toml` |
| Modify | `caiman-app/build.gradle.kts` |
| **Create** | `caiman-app/src/main/java/com/caimanproject/app/config/DatabaseType.java` |
| **Create** | `caiman-app/src/main/java/com/caimanproject/app/config/DataSourceConfig.java` |
| Modify | `caiman-app/src/main/java/com/caimanproject/app/CaimanApplication.java` |
| Modify | `caiman-app/src/main/java/com/caimanproject/app/property/CaimanServerPropsConfig.java` |
| Modify | `caiman-app/src/main/resources/application.yaml` |
| Modify | `caiman-app/src/test/resources/application.yaml` |
| Modify | `caiman-app/src/test/java/com/caimanproject/app/test/IntegrationTestController.java` |
| Delete | `caiman-app/src/test/java/com/caimanproject/app/test/config/TestcontainersConfig.java` |
| **Create** | `caiman-app/src/test/java/com/caimanproject/app/integration/CreateDebtorPostgresIT.java` |
| **Create** | `caiman-app/src/test/java/com/caimanproject/app/integration/CreateDebtorSQLiteIT.java` |

---

### Task 1: Add SQLite dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `caiman-app/build.gradle.kts`

- [ ] **Step 1: Add sqlite-jdbc version to version catalog**

Open `gradle/libs.versions.toml`. After the last entry in `[versions]`, add:

```toml
sqlite-jdbc = "3.47.1.0"
```

Then in `[libraries]`, add after the last library entry:

```toml
sqlite-jdbc = { module = "org.xerial:sqlite-jdbc", version.ref = "sqlite-jdbc" }
```

> **Note:** If `3.47.1.0` is unavailable, check https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc for the latest stable release and update the version.

- [ ] **Step 2: Add runtime dependencies in `caiman-app/build.gradle.kts`**

In the `dependencies {}` block, after the `implementation("org.springframework.boot:spring-boot-starter-liquibase")` line, add:

```kotlin
implementation(libs.sqlite.jdbc)
implementation("org.hibernate.orm:hibernate-community-dialects")
```

`hibernate-community-dialects` has no version because Spring Boot's BOM manages all `org.hibernate.orm:*` artifacts.

- [ ] **Step 3: Verify Gradle can resolve both artifacts**

```bash
cd /home/under7/Workspace/caiman/caiman-server
./gradlew :caiman-app:dependencies --configuration compileClasspath 2>&1 | grep -E "sqlite|hibernate-community"
```

Expected output includes both:
```
+--- org.xerial:sqlite-jdbc:3.47.1.0
+--- org.hibernate.orm:hibernate-community-dialects:x.x.x
```

---

### Task 2: Create `DatabaseType` enum

**Files:**
- Create: `caiman-app/src/main/java/com/caimanproject/app/config/DatabaseType.java`

- [ ] **Step 1: Create the enum**

```java
package com.caimanproject.app.config;

public enum DatabaseType {
    POSTGRES,
    SQLITE
}
```

No tests needed — Spring Boot binds `"POSTGRES"` / `"SQLITE"` strings from env vars to this enum type automatically via `@ConfigurationProperties`.

---

### Task 3: Update `CaimanServerPropsConfig`

**Files:**
- Modify: `caiman-app/src/main/java/com/caimanproject/app/property/CaimanServerPropsConfig.java`

Current state: `DatabasePropImpl` has `@NotBlank url`, `@NotBlank username`, `@NotBlank password`.

Target state: add `type` (required), `sqliteFile` (optional), make the Postgres fields nullable, add cross-field `@AssertTrue` validators, make `DatabasePropImpl` `public` (required so `DataSourceConfig` in a different package can call its methods).

- [ ] **Step 1: Replace the entire `DatabasePropImpl` record**

The imports to add at the top of the file (after existing imports):

```java
import com.caimanproject.app.config.DatabaseType;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.lang.Nullable;
```

Replace the existing `DatabasePropImpl` record (currently lines ~22–27) with:

```java
public record DatabasePropImpl(
    @NotNull DatabaseType type,
    @Nullable String url,
    @Nullable String username,
    @Nullable String password,
    @Nullable String sqliteFile
) implements CaimanServerProps.DatabaseProp {

    DatabasePropImpl {
        url = (url != null && url.isBlank()) ? null : url;
        username = (username != null && username.isBlank()) ? null : username;
        password = (password != null && password.isBlank()) ? null : password;
        sqliteFile = (sqliteFile != null && sqliteFile.isBlank()) ? null : sqliteFile;
    }

    @AssertTrue(message = "DATABASE_TYPE=POSTGRES requires DATABASE_JDBC_URL, DATABASE_USERNAME, DATABASE_PASSWORD")
    boolean isPostgresConfigValid() {
        return type != DatabaseType.POSTGRES
            || (url != null && username != null && password != null);
    }

    @AssertTrue(message = "DATABASE_TYPE=SQLITE requires DATABASE_SQLITE_FILE")
    boolean isSqliteConfigValid() {
        return type != DatabaseType.SQLITE
            || sqliteFile != null;
    }
}
```

The compact constructor (`DatabasePropImpl { ... }`) normalizes empty strings (from YAML default `:`) to `null`. This avoids `@NotBlank` validation while still treating blank values as absent.

- [ ] **Step 2: Verify the class still compiles**

```bash
./gradlew :caiman-app:compileJava 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

### Task 4: Update `application.yaml` (main)

**Files:**
- Modify: `caiman-app/src/main/resources/application.yaml`

- [ ] **Step 1: Replace the `caiman-server.database` section**

Find and replace the existing database block:
```yaml
  database:
    url: ${CAIMAN_SERVER_DATABASE_JDBC_URL}
    username: ${CAIMAN_SERVER_DATABASE_USERNAME}
    password: ${CAIMAN_SERVER_DATABASE_PASSWORD}
```

With:
```yaml
  database:
    type: ${CAIMAN_SERVER_DATABASE_TYPE}
    url: ${CAIMAN_SERVER_DATABASE_JDBC_URL:}
    username: ${CAIMAN_SERVER_DATABASE_USERNAME:}
    password: ${CAIMAN_SERVER_DATABASE_PASSWORD:}
    sqlite-file: ${CAIMAN_SERVER_DATABASE_SQLITE_FILE:}
```

`${VAR}` (no colon) = required, startup fails if not set.
`${VAR:}` (colon, empty default) = optional; binds to empty string → compact constructor normalizes to `null`.

- [ ] **Step 2: Remove the `spring.datasource` section**

Remove these lines from the `spring:` block:
```yaml
  datasource:
    url: ${caiman-server.database.url}
    username: ${caiman-server.database.username}
    password: ${caiman-server.database.password}
```

We configure the `DataSource` bean manually. Spring Boot's datasource auto-config is excluded so these properties are unused.

- [ ] **Step 3: Verify YAML is valid**

```bash
./gradlew :caiman-app:processResources 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

---

### Task 5: Update `application.yaml` (test)

**Files:**
- Modify: `caiman-app/src/test/resources/application.yaml`

- [ ] **Step 1: Remove the entire `database` subsection**

Remove these lines from the test YAML:
```yaml
  database:
    url: 'not-informed'
    username: 'not-informed'
    password: 'not-informed'
```

Rationale: These hardcoded values are stale — the fields are now nullable. Removing them is cleanup. Note: `@DynamicPropertySource` (Task 8) sets `caiman-server.database.*` **directly** (not via env var names), so it would override the test YAML values regardless. But removing them keeps the test YAML honest.

---

### Task 6: Create `DataSourceConfig`

**Files:**
- Create: `caiman-app/src/main/java/com/caimanproject/app/config/DataSourceConfig.java`

- [ ] **Step 1: Create the configuration class**

```java
package com.caimanproject.app.config;

import com.caimanproject.app.property.CaimanServerPropsConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.JpaVendorAdapter;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(CaimanServerPropsConfig props) {
        return switch (props.database().type()) {
            case POSTGRES -> buildPostgresDataSource(
                props.database().url(),
                props.database().username(),
                props.database().password()
            );
            case SQLITE -> buildSqliteDataSource(props.database().sqliteFile());
        };
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter(CaimanServerPropsConfig props) {
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform(switch (props.database().type()) {
            case POSTGRES -> PostgreSQLDialect.class.getName();
            case SQLITE   -> SQLiteDialect.class.getName();
        });
        adapter.setGenerateDdl(false);
        return adapter;
    }

    private DataSource buildPostgresDataSource(String url, String username, String password) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    private DataSource buildSqliteDataSource(String sqliteFile) {
        var config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(5000);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        config.setCacheSize(-2000);
        config.enforceForeignKeys(true);

        var ds = new SQLiteDataSource(config);
        ds.setUrl("jdbc:sqlite:" + sqliteFile);
        return ds;
    }
}
```

**Key design points:**
- No `@ConditionalOnProperty` — both cases always compiled, GraalVM-safe.
- `JpaVendorAdapter` bean: Spring Boot's `HibernateJpaAutoConfiguration` is `@ConditionalOnMissingBean(JpaVendorAdapter.class)` — our bean causes it to back off and use ours instead. Other JPA settings (`hibernate.jdbc.time_zone: UTC` from `application.yaml`) are still applied via `spring.jpa.properties.*` by `LocalContainerEntityManagerFactoryBean`.
- `foreign_keys = ON` is critical: SQLite ignores FK constraints by default without this PRAGMA.
- WAL mode + `busy_timeout = 5000` + `synchronous = NORMAL` is the standard production-safe SQLite config.

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :caiman-app:compileJava 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

### Task 7: Update `CaimanApplication` — exclude DataSource auto-config

**Files:**
- Modify: `caiman-app/src/main/java/com/caimanproject/app/CaimanApplication.java`

- [ ] **Step 1: Add exclusion and import**

Add import:
```java
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
```

Change the `@SpringBootApplication` annotation from:
```java
@SpringBootApplication(scanBasePackages = "com.caimanproject")
```

To:
```java
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class},
    scanBasePackages = "com.caimanproject"
)
```

This ensures Spring Boot never attempts to auto-configure the `DataSource`. Our `DataSourceConfig` bean takes over entirely.

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :caiman-app:compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

---

### Task 8: Update `IntegrationTestController` and delete `TestcontainersConfig`

**Files:**
- Modify: `caiman-app/src/test/java/com/caimanproject/app/test/IntegrationTestController.java`
- Delete: `caiman-app/src/test/java/com/caimanproject/app/test/config/TestcontainersConfig.java`

The old `TestcontainersConfig` used `@ServiceConnection` which wired `spring.datasource.*` from the container. Now that we exclude `DataSourceAutoConfiguration` and configure manually via `@ConfigurationProperties`, `@ServiceConnection` has no effect. We move the container to `IntegrationTestController` and use `@DynamicPropertySource` to set `caiman-server.database.*` properties **directly** — bypassing the env-var indirection in the YAML entirely. `@DynamicPropertySource` has higher priority than all YAML files, so it overwrites any test YAML values.

**Java class-loading guarantee:** Static fields and static initializers in a class run in declaration order before any static method of that class is called. Since both the container field and `@DynamicPropertySource` method are in the same class, the container is guaranteed to be started before the property registration method runs.

- [ ] **Step 1: Delete `TestcontainersConfig.java`**

```bash
rm /home/under7/Workspace/caiman/caiman-server/caiman-app/src/test/java/com/caimanproject/app/test/config/TestcontainersConfig.java
```

- [ ] **Step 2: Replace `IntegrationTestController.java` entirely**

```java
package com.caimanproject.app.test;

import com.caimanproject.app.CaimanApplication;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = CaimanApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestController {

    static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("caiman-server.database.type", () -> "POSTGRES");
        registry.add("caiman-server.database.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("caiman-server.database.username", POSTGRES_CONTAINER::getUsername);
        registry.add("caiman-server.database.password", POSTGRES_CONTAINER::getPassword);
    }

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
```

Changes from original:
- Removed `@ContextConfiguration(classes = {TestcontainersConfig.class})` — `TestcontainersConfig` is deleted.
- Added `static PostgreSQLContainer` field + static initializer to start it.
- Added `@DynamicPropertySource` to map container details to our custom env vars.

- [ ] **Step 3: Compile tests to check for import errors**

```bash
./gradlew :caiman-app:compileTestJava 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. If there are import errors for `TestcontainersConfig` anywhere, fix them.

---

### Task 9: Run existing tests — verify nothing broke

- [ ] **Step 1: Run the full test suite**

```bash
./gradlew :caiman-app:test 2>&1 | tail -40
```

Expected: All existing tests pass (`DebtorControllerIT`, `ApiDocumentationIT`).

If tests fail with datasource errors, check:
1. Is `CAIMAN_SERVER_DATABASE_TYPE` being set? The `@DynamicPropertySource` on `IntegrationTestController` must fire before context creation.
2. Is the test YAML's database section completely removed? (Task 5)
3. Does the main YAML's database section have the new `type:` field? (Task 4)

---

### Task 10: Create `CreateDebtorPostgresIT`

**Files:**
- Create: `caiman-app/src/test/java/com/caimanproject/app/integration/CreateDebtorPostgresIT.java`

This test extends `IntegrationTestController` (gets Postgres automatically) and calls `POST /v1/debtors` once to confirm end-to-end flow works with Postgres.

- [ ] **Step 1: Create the test**

```java
package com.caimanproject.app.integration;

import com.caimanproject.app.test.IntegrationTestController;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("createDebtor — PostgreSQL integration")
class CreateDebtorPostgresIT extends IntegrationTestController {

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE debtor_contact CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE debtor CASCADE");
    }

    @Test
    @DisplayName("should create debtor successfully via PostgreSQL")
    void should_create_debtor_postgres() {
        final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

        webTestClient
            .post()
            .uri("/v1/debtors")
            .header(RequestConstants.Headers.X_CORRELATION_ID, "test-postgres-001")
            .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(DebtorResponseDto.class)
            .value(response -> {
                assertThat(response.id()).isNotNull();
                assertThat(response.name()).isEqualTo(request.name());
                assertThat(response.active()).isTrue();
                assertThat(response.audit().createdAt()).isNotNull();
            });
    }
}
```

- [ ] **Step 2: Run the new Postgres test to verify it passes**

```bash
./gradlew :caiman-app:test --tests "com.caimanproject.app.integration.CreateDebtorPostgresIT" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 1 test passed.

---

### Task 11: Create `CreateDebtorSQLiteIT`

**Files:**
- Create: `caiman-app/src/test/java/com/caimanproject/app/integration/CreateDebtorSQLiteIT.java`

This test does NOT extend `IntegrationTestController` (which wires Postgres). It starts a fresh Spring context with SQLite via its own `@DynamicPropertySource`.

The database file uses a UUID suffix so each test run gets a clean database. `@AfterAll` deletes the file.

- [ ] **Step 1: Create the test**

```java
package com.caimanproject.app.integration;

import com.caimanproject.app.CaimanApplication;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = CaimanApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("createDebtor — SQLite integration")
class CreateDebtorSQLiteIT {

    private static final Path SQLITE_DB_PATH = Path.of(
        System.getProperty("java.io.tmpdir"),
        "caiman-test-" + UUID.randomUUID() + ".db"
    );

    @DynamicPropertySource
    static void registerSQLiteProperties(DynamicPropertyRegistry registry) {
        registry.add("caiman-server.database.type", () -> "SQLITE");
        registry.add("caiman-server.database.sqlite-file", SQLITE_DB_PATH::toString);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM debtor_contact");
        jdbcTemplate.execute("DELETE FROM debtor");
    }

    @AfterAll
    void deleteSqliteFile() throws IOException {
        Files.deleteIfExists(SQLITE_DB_PATH);
    }

    @Test
    @DisplayName("should create debtor successfully via SQLite")
    void should_create_debtor_sqlite() {
        final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

        webTestClient
            .post()
            .uri("/v1/debtors")
            .header(RequestConstants.Headers.X_CORRELATION_ID, "test-sqlite-001")
            .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(DebtorResponseDto.class)
            .value(response -> {
                assertThat(response.id()).isNotNull();
                assertThat(response.name()).isEqualTo(request.name());
                assertThat(response.active()).isTrue();
                assertThat(response.audit().createdAt()).isNotNull();
            });
    }
}
```

**SQLite-specific notes:**
- `DELETE FROM` instead of `TRUNCATE CASCADE` — SQLite has no `TRUNCATE`.
- `@DynamicPropertySource` sets `CAIMAN_SERVER_DATABASE_TYPE=SQLITE` → Spring creates a different `ApplicationContext` than Postgres tests (different context cache key). These two contexts never share state.
- Liquibase runs fresh migrations on the new SQLite file at context startup. The existing YAML changelogs are SQLite-compatible: all FKs are inline in `createTable` (not `addForeignKeyConstraint`), and Liquibase maps abstract types (`boolean`, `datetime`, `decimal`) to SQLite equivalents automatically.

- [ ] **Step 2: Run the SQLite test in isolation**

```bash
./gradlew :caiman-app:test --tests "com.caimanproject.app.integration.CreateDebtorSQLiteIT" 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`, 1 test passed.

If the context fails to start:
- Check `DataSourceConfig` compiles correctly with SQLite imports.
- Check that `CAIMAN_SERVER_DATABASE_SQLITE_FILE` is set by `@DynamicPropertySource` before context init.
- Check Liquibase logs — if it can't detect the dialect for SQLite, verify `org.sqlite.JDBC` driver is on the classpath and the URL format `jdbc:sqlite:<path>` is correct.

---

### Task 12: Run the full test suite — final verification

- [ ] **Step 1: Run all tests**

```bash
./gradlew test 2>&1 | tail -50
```

Expected: `BUILD SUCCESSFUL`, all tests pass including `DebtorControllerIT`, `ApiDocumentationIT`, `CreateDebtorPostgresIT`, `CreateDebtorSQLiteIT`.

- [ ] **Step 2: Verify the app starts with Postgres env vars**

```bash
cd /home/under7/Workspace/caiman/caiman-server
source .env && export CAIMAN_SERVER_DATABASE_TYPE=POSTGRES && \
  ./gradlew :caiman-app:bootRun 2>&1 | grep -E "Started|DataSource|Liquibase|ERROR" | head -10
```

Expected: `Started CaimanApplication` with no errors.

- [ ] **Step 3: Verify startup fails without DATABASE_TYPE (confirms no default)**

```bash
./gradlew :caiman-app:bootRun 2>&1 | grep -E "ERROR|Could not resolve placeholder" | head -5
```

Expected output contains `Could not resolve placeholder 'CAIMAN_SERVER_DATABASE_TYPE'` — confirms the env var is mandatory.

---

## Self-Review Checklist

**Spec coverage:**
- [x] Config in application.yaml from env var → Task 4
- [x] Remove Spring Data auto-configure, configure manually → Task 7 (exclude), Task 6 (DataSourceConfig)
- [x] Instantiate appropriate DataSource by type → Task 6 (`DataSourceConfig.dataSource()`)
- [x] No `@ConditionalOnProperty`, GraalVM-safe → Task 6 (plain switch in @Bean method)
- [x] No default database type → Task 4 (`${CAIMAN_SERVER_DATABASE_TYPE}` with no default)
- [x] Migrations work for both DBs → no changes needed (inline FKs + Liquibase type mapping)
- [x] New integration tests (createDebtor) for Postgres and SQLite → Tasks 10, 11

**Type consistency:**
- `DatabaseType` created in Task 2, used in Tasks 3 and 6 ✓
- `CaimanServerPropsConfig.DatabasePropImpl` made `public` in Task 3, used in Task 6 via `props.database().type()` ✓
- `DtoBuilder.buildCreateDebtorRequestDto()` used in Tasks 10 and 11 — already exists in test codebase ✓

**Placeholder scan:** No TBD/TODO/placeholder code present.
