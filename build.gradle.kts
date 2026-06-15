import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.spotless)
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
    version = "v0.0.1"

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

    // Makefile requires tab indentation — keep separate from spaces-based misc format
    format("makefile") {
        target("Makefile")
        trimTrailingWhitespace()
        endWithNewline()
        leadingSpacesToTabs()
    }

    java {
        target("**/*.java")
        palantirJavaFormat("2.89.0").style("PALANTIR").formatJavadoc(true)
        removeUnusedImports()
        // Regular imports first, then org.awaitility statics, then all other statics
        importOrder("", "\\#org.awaitility.Awaitility", "\\#")
    }

    kotlinGradle {
        target("**/*.kts")
        ktlint()
    }
}
