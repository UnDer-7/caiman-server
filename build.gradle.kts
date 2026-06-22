import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarExtension

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
}

val springBootVersion =
    libs.versions.spring.boot
        .get()
val springdocVersion =
    libs.versions.springdoc.openapi
        .get()
val logstashEncoderVersion =
    libs.versions.logstash.logback.encoder
        .get()
val jacocoToolVersion = libs.versions.jacoco.get()

repositories {
    mavenCentral()
}

jacoco {
    toolVersion = jacocoToolVersion
}

// Classes with no meaningful coverage value: bootstrap, wiring, exceptions, generated code.
// Use *Foo*.class (not *Foo.class) so Ant glob's trailing * also matches inner-class suffixes
// like $Inner — e.g. "**/*Config*.class" covers both FooConfig.class and FooConfig$Bar.class.
val jacocoExcludes =
    listOf(
        "**/CaimanApplication.class",
        "**/exception/**",
        "**/*Config*.class", // @Configuration / @ConfigurationProperties + their inner classes
        "**/*Initializer.class",
        "**/*Constants*.class", // constant holders + their inner classes
        "**/*MapperImpl.class",
    )

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    configure<org.gradle.testing.jacoco.plugins.JacocoPluginExtension> {
        toolVersion = jacocoToolVersion
    }

    group = "com.caimanproject"
    version = "v0.0.2"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("org.springdoc:springdoc-openapi-bom:$springdocVersion")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("started", "passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }
    }

    tasks.register<Test>("unitTest") {
        description = "Runs unit tests (tagged @UnitTest)."
        group = "verification"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags("unit")
        }
    }

    tasks.register<Test>("integrationTestJvm") {
        description = "Runs integration tests (tagged @IntegrationTest) on JVM."
        group = "verification"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags("integration")
        }
        shouldRunAfter("unitTest")
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
        "implementation"("org.slf4j:slf4j-api")

        if (project.name != "caiman-test-support") {
            "testImplementation"(project(":caiman-test-support"))
        }

        "testImplementation"("org.junit.jupiter:junit-jupiter-engine")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params")
        "testImplementation"("org.mockito:mockito-core")
        "testImplementation"("org.mockito:mockito-junit-jupiter")
        "testImplementation"("org.assertj:assertj-core")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testRuntimeOnly"("ch.qos.logback:logback-classic")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dependency locking — update lockfiles: ./gradlew updateDependencyLocks --write-locks
// ─────────────────────────────────────────────────────────────────────────────
tasks.register("updateDependencyLocks") {
    notCompatibleWithConfigurationCache("Dependency locking does not support configuration cache")
    description = "Regenerates gradle.lockfile for all subprojects. Run with --write-locks."
    group = "verification"
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) {
            "Must run with --write-locks flag: ./gradlew updateDependencyLocks --write-locks"
        }
    }
    dependsOn(subprojects.map { "${it.path}:dependencies" })
}

// ─────────────────────────────────────────────────────────────────────────────
// Aggregated JaCoCo report — single XML/HTML for all modules
//
// Run after tests:  ./gradlew unitTest integrationTest jacocoRootReport
// Sonar property:   sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/aggregate/jacoco.xml
// ─────────────────────────────────────────────────────────────────────────────
// caiman-test-support: test infrastructure, no production code
// caiman-mapper-spi: MapStruct annotation processor SPI (compile-time tooling, not runtime code)
val reportableProjects = subprojects.filter { it.name !in setOf("caiman-test-support", "caiman-mapper-spi") }

tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Aggregates JaCoCo coverage reports from all modules into a single report."

    // Runs after tests when both are in the same build; does NOT trigger tests on its own.
    mustRunAfter(subprojects.flatMap { it.tasks.withType<Test>() })

    sourceDirectories.setFrom(
        files(
            reportableProjects.map { subproject ->
                subproject.extensions
                    .getByType<SourceSetContainer>()["main"]
                    .allSource.srcDirs
            },
        ),
    )
    classDirectories.setFrom(
        files(
            reportableProjects.map { subproject ->
                subproject.tasks.named<JavaCompile>("compileJava").map { compileTask ->
                    subproject.fileTree(compileTask.destinationDirectory) {
                        exclude(jacocoExcludes)
                    }
                }
            },
        ),
    )
    executionData.setFrom(
        files(
            subprojects.flatMap { subproject ->
                subproject.tasks
                    .withType<Test>()
                    .filter { it.name != "nativeTest" }
                    .mapNotNull { testTask ->
                        testTask.extensions.findByType<JacocoTaskExtension>()?.destinationFile
                    }
            },
        ),
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(
            rootProject.layout.buildDirectory.file("reports/jacoco/aggregate/jacoco.xml"),
        )
        html.outputLocation.set(
            rootProject.layout.buildDirectory.dir("reports/jacoco/aggregate/html"),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spotless — code formatting
// Docs: https://github.com/diffplug/spotless/tree/main/plugin-gradle
//
// Check:  ./gradlew spotlessCheck
// Apply:  ./gradlew spotlessApply
// ─────────────────────────────────────────────────────────────────────────────
spotless {
    format("misc") {
        target(
            ".gitignore",
            ".gitattributes",
            ".env",
            ".sdkmanrc",
            "docker-entrypoint.sh",
            "Dockerfile.jvm",
            "Dockerfile.native",
            "lombok.config",
        )
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(4)
    }

    java {
        target("**/*.java")
        palantirJavaFormat("2.89.0").style("PALANTIR").formatJavadoc(true)
        removeUnusedImports()
        formatAnnotations()
        // Regular imports first, then org.awaitility statics, then all other statics
        importOrder("", "\\#org.awaitility.Awaitility", "\\#")
    }

    kotlinGradle {
        target("**/*.kts")
        ktlint()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SonarCloud — static analysis + coverage upload
//
// Dashboard: https://sonarcloud.io/dashboard?id=UnDer-7_caiman-server
//
// Each subproject auto-detects its own sonar.sources/sonar.tests.
// The aggregate JaCoCo XML is pushed to every subproject so SonarCloud receives
// full coverage data per module. Sonar silently ignores entries in the XML that
// don't belong to the module being analyzed — no double-indexing, no BUILD FAILED.
// ─────────────────────────────────────────────────────────────────────────────
sonar {
    properties {
        property("sonar.projectKey", "UnDer-7_caiman-server")
        property("sonar.organization", "under-7")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/CaimanApplication.*",
                "**/exception/**",
                "**/*Config*",
                "**/*Initializer*",
                "**/*Constants*",
                "**/*MapperImpl*",
            ).joinToString(","),
        )
    }
}

// Push the aggregate JaCoCo XML and compiled class dirs to every subproject so
// each module reports accurate coverage without manual per-module jacoco tasks.
gradle.projectsEvaluated {
    val aggregateXml =
        rootProject.layout.buildDirectory
            .file("reports/jacoco/aggregate/jacoco.xml")
            .get()
            .asFile
            .absolutePath

    reportableProjects.forEach { subproject ->
        subproject.extensions
            .findByType<SonarExtension>()
            ?.properties {
                property("sonar.coverage.jacoco.xmlReportPaths", aggregateXml)
                property(
                    "sonar.java.binaries",
                    subproject.layout.buildDirectory
                        .dir("classes/java/main")
                        .get()
                        .asFile
                        .absolutePath,
                )
            }
    }
}
