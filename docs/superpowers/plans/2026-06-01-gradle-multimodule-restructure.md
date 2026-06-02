# Gradle Multi-Module Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the current single-module Spring Boot project into a Gradle multi-module build with `caiman-shared`, `caiman-debtor` (hexagonal submodules), and `caiman-app`.

**Architecture:** Root project holds common Gradle config (Java toolchain, dependency management, repositories). `caiman-app` is the only module with the Spring Boot plugin and produces the runnable JAR. `caiman-debtor` is a directory grouping for three subprojects (`core`, `entrypoint`, `infrastructure`) following hexagonal architecture. `caiman-shared` is a plain library with no Spring.

**Tech Stack:** Gradle 9.x (Kotlin DSL), Java 25, Spring Boot 4.0.6, `io.spring.dependency-management` 1.1.7

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `settings.gradle.kts` | Declare all subprojects |
| Rewrite | `build.gradle.kts` | Root: common Java config + Spring BOM, no Spring Boot plugin applied |
| Create | `caiman-shared/build.gradle.kts` | Plain Java library, no extra deps |
| Create | `caiman-debtor/core/build.gradle.kts` | No Spring; depends on `:caiman-shared` |
| Create | `caiman-debtor/entrypoint/build.gradle.kts` | Spring Web MVC; depends on `:caiman-debtor:core` |
| Create | `caiman-debtor/infrastructure/build.gradle.kts` | Depends on `:caiman-debtor:core`, `:caiman-shared` |
| Create | `caiman-app/build.gradle.kts` | Spring Boot plugin; wires `:entrypoint` + `:infrastructure` |
| Move+edit | `caiman-app/src/main/java/com/caimanproject/app/CaimanApplication.java` | Main class (from root `src/`) |
| Move+edit | `caiman-app/src/main/resources/application.yaml` | App config (from root `src/`) |
| Move+edit | `caiman-app/src/test/java/com/caimanproject/app/CaimanApplicationTests.java` | Context load test |
| Move+edit | `caiman-debtor/entrypoint/src/main/java/com/caimanproject/debtor/entrypoint/TempController.java` | Temp REST controller (from root `src/`) |
| Delete | `src/` | Entire old monolithic source tree |

---

## Task 1: Update `settings.gradle.kts`

**Files:**
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Replace file content**

```kotlin
rootProject.name = "caiman-server"

include("caiman-shared")
include("caiman-debtor:core")
include("caiman-debtor:entrypoint")
include("caiman-debtor:infrastructure")
include("caiman-app")
```

- [ ] **Step 2: Verify Gradle sees the projects**

```bash
./gradlew projects
```

Expected output contains:
```
+--- Project ':caiman-app'
+--- Project ':caiman-debtor:core'
+--- Project ':caiman-debtor:entrypoint'
+--- Project ':caiman-debtor:infrastructure'
\--- Project ':caiman-shared'
```

---

## Task 2: Rewrite root `build.gradle.kts`

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Rewrite to multi-module root config**

The root build declares plugins as `apply false` so only subprojects that opt-in get them.
`io.spring.dependency-management` is applied to every subproject via `subprojects {}` so version numbers need not be repeated anywhere.

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    group = "com.caimanproject"
    version = "0.0.1-SNAPSHOT"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 2: Verify root config parses**

```bash
./gradlew help --project-dir .
```

Expected: no BUILD FAILED, no unresolved plugin errors.

---

## Task 3: Create `caiman-shared` module

**Files:**
- Create: `caiman-shared/build.gradle.kts`
- Create: `caiman-shared/src/main/java/com/caimanproject/shared/.gitkeep`

- [ ] **Step 1: Create build file**

`caiman-shared` is a plain Java library. No Spring dependencies yet.

```kotlin
// caiman-shared/build.gradle.kts
dependencies {
    // No inter-module dependencies
}
```

- [ ] **Step 2: Create source directory placeholder**

```bash
mkdir -p caiman-shared/src/main/java/com/caimanproject/shared
touch caiman-shared/src/main/java/com/caimanproject/shared/.gitkeep
```

- [ ] **Step 3: Verify module compiles**

```bash
./gradlew :caiman-shared:compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 4: Create `caiman-debtor:core` module

**Files:**
- Create: `caiman-debtor/core/build.gradle.kts`
- Create: `caiman-debtor/core/src/main/java/com/caimanproject/debtor/core/.gitkeep`

- [ ] **Step 1: Create build file**

No Spring in core — pure domain model layer.

```kotlin
// caiman-debtor/core/build.gradle.kts
dependencies {
    implementation(project(":caiman-shared"))
}
```

- [ ] **Step 2: Create source directory placeholder**

```bash
mkdir -p caiman-debtor/core/src/main/java/com/caimanproject/debtor/core
touch caiman-debtor/core/src/main/java/com/caimanproject/debtor/core/.gitkeep
```

- [ ] **Step 3: Verify module compiles**

```bash
./gradlew :caiman-debtor:core:compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 5: Create `caiman-debtor:entrypoint` module and move `TempController`

**Files:**
- Create: `caiman-debtor/entrypoint/build.gradle.kts`
- Create: `caiman-debtor/entrypoint/src/main/java/com/caimanproject/debtor/entrypoint/TempController.java`

- [ ] **Step 1: Create build file**

Entrypoint holds REST controllers — needs Spring Web MVC.

```kotlin
// caiman-debtor/entrypoint/build.gradle.kts
dependencies {
    implementation(project(":caiman-debtor:core"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
}
```

- [ ] **Step 2: Create source directory**

```bash
mkdir -p caiman-debtor/entrypoint/src/main/java/com/caimanproject/debtor/entrypoint
```

- [ ] **Step 3: Create `TempController.java` in new location with updated package**

```java
// caiman-debtor/entrypoint/src/main/java/com/caimanproject/debtor/entrypoint/TempController.java
package com.caimanproject.debtor.entrypoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TempController {

    @GetMapping("/tst")
    public Map<String, String> testEndpoint() {
        return Map.of("msg", "is working!!!");
    }
}
```

- [ ] **Step 4: Verify module compiles**

```bash
./gradlew :caiman-debtor:entrypoint:compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 6: Create `caiman-debtor:infrastructure` module

**Files:**
- Create: `caiman-debtor/infrastructure/build.gradle.kts`
- Create: `caiman-debtor/infrastructure/src/main/java/com/caimanproject/debtor/infrastructure/.gitkeep`

- [ ] **Step 1: Create build file**

Infrastructure holds JPA repositories and external adapters.

```kotlin
// caiman-debtor/infrastructure/build.gradle.kts
dependencies {
    implementation(project(":caiman-debtor:core"))
    implementation(project(":caiman-shared"))
}
```

- [ ] **Step 2: Create source directory placeholder**

```bash
mkdir -p caiman-debtor/infrastructure/src/main/java/com/caimanproject/debtor/infrastructure
touch caiman-debtor/infrastructure/src/main/java/com/caimanproject/debtor/infrastructure/.gitkeep
```

- [ ] **Step 3: Verify module compiles**

```bash
./gradlew :caiman-debtor:infrastructure:compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 7: Create `caiman-app` module and migrate main class

**Files:**
- Create: `caiman-app/build.gradle.kts`
- Create: `caiman-app/src/main/java/com/caimanproject/app/CaimanApplication.java`
- Create: `caiman-app/src/main/resources/application.yaml`
- Create: `caiman-app/src/test/java/com/caimanproject/app/CaimanApplicationTests.java`

- [ ] **Step 1: Create build file**

`caiman-app` is the composition root — only module that applies the Spring Boot plugin and produces the runnable JAR.
`scanBasePackages = "com.caimanproject"` ensures Spring component scan covers all bounded contexts.

```kotlin
// caiman-app/build.gradle.kts
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":caiman-debtor:entrypoint"))
    implementation(project(":caiman-debtor:infrastructure"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [ ] **Step 2: Create source and resource directories**

```bash
mkdir -p caiman-app/src/main/java/com/caimanproject/app
mkdir -p caiman-app/src/main/resources
mkdir -p caiman-app/src/test/java/com/caimanproject/app
```

- [ ] **Step 3: Create main class**

```java
// caiman-app/src/main/java/com/caimanproject/app/CaimanApplication.java
package com.caimanproject.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.caimanproject")
public class CaimanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaimanApplication.class, args);
    }
}
```

- [ ] **Step 4: Create `application.yaml`**

```yaml
# caiman-app/src/main/resources/application.yaml
server:
  port: 8080

spring:
  application:
    name: caiman-server
```

- [ ] **Step 5: Create context load test**

```java
// caiman-app/src/test/java/com/caimanproject/app/CaimanApplicationTests.java
package com.caimanproject.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CaimanApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: Verify caiman-app compiles**

```bash
./gradlew :caiman-app:compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 8: Delete old root `src/` tree

**Files:**
- Delete: `src/` (entire directory)

- [ ] **Step 1: Remove old source tree**

```bash
rm -rf src/
```

- [ ] **Step 2: Verify no root-level src remains**

```bash
ls -la | grep src
```

Expected: no output.

---

## Task 9: Full build verification and commit

- [ ] **Step 1: Run full build**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`, all subprojects compile, `caiman-app` tests pass.

- [ ] **Step 2: Verify bootJar produced only in caiman-app**

```bash
ls caiman-app/build/libs/
```

Expected: `caiman-app-0.0.1-SNAPSHOT.jar` (or similar Spring Boot fat JAR).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: convert to Gradle multi-module (shared, debtor, app)"
```
