import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

tasks.bootRun {
    workingDir = rootProject.projectDir
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

    implementation("org.codehaus.janino:janino")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    testImplementation(project(":caiman-debtor-core"))
    testImplementation(project(":caiman-web-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
