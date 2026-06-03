plugins {
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    // ### Modules ####
    implementation(project(":caiman-debtor-entrypoint"))
    implementation(project(":caiman-debtor-infrastructure"))

    implementation(project(":caiman-billing-entrypoint"))
    implementation(project(":caiman-billing-infrastructure"))

    implementation(project(":caiman-payment-entrypoint"))
    implementation(project(":caiman-payment-infrastructure"))

    implementation(project(":caiman-notification-entrypoint"))
    implementation(project(":caiman-notification-infrastructure"))
    // ----------------

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
