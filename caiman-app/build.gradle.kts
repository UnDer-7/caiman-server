import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.graalvm.native)
}

graalvmNative {
    binaries {
        named("main") {
            // Allow native-image's JVM to use up to 90% of system RAM.
            // Default is ~43% of available (not total) memory. Build is CPU-bound so the gain is small,
            // but it reduces GC pressure during points-to analysis.
            buildArgs.add("-J-XX:MaxRAMPercentage=90.0")
            // spring-orm already includes this flag via its META-INF/native-image/. Kept explicit because
            // the tracing agent previously collected ByteBuddy entries that caused BytecodeProviderImpl
            // static init (ClassLoader.defineClass) to run during image build — those entries are now
            // removed from reachability-metadata.json, but this flag guards against accidental re-addition.
            buildArgs.add("-H:ServiceLoaderFeatureExcludeServices=org.hibernate.bytecode.spi.BytecodeProvider")
        }
    }
}

tasks.bootRun {
    workingDir = rootProject.projectDir
}

tasks.register("integrationTestNative") {
    description = "Runs integration tests (tagged @IntegrationTest) in GraalVM native mode. Requires Docker."
    group = "verification"
    dependsOn("nativeTest")
}

tasks.named<ProcessResources>("processResources") {
    filesMatching(listOf("application*.yml", "application*.yaml")) {
        filter(ReplaceTokens::class, "tokens" to mapOf(
            "project.name" to "caiman-server",
            "project.description" to "toDo colocar description",
            "project.version" to project.version.toString()
        ))
    }
}

dependencies {
    // ### Modules ####
    implementation(project(":caiman-contracts"))

    implementation(project(":caiman-debtor-entrypoint"))
    implementation(project(":caiman-debtor-infrastructure"))

    implementation(project(":caiman-billing-entrypoint"))
    implementation(project(":caiman-billing-infrastructure"))

    implementation(project(":caiman-payment-entrypoint"))
    implementation(project(":caiman-payment-infrastructure"))

    implementation(project(":caiman-notification-entrypoint"))
    implementation(project(":caiman-notification-infrastructure"))
    // ----------------

    implementation("org.hibernate.orm:hibernate-graalvm")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(project(":caiman-debtor-core"))
    testImplementation(project(":caiman-web-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
