dependencies {

    // --- Internal: core ---
    implementation(project(":caiman-debtor-core"))

    // --- Internal: external-libraries ---
    implementation(project(":caiman-contracts"))
    implementation(project(":caiman-mapper-common"))
    implementation(project(":caiman-jpa-support"))
    annotationProcessor(project(":caiman-mapper-spi"))

    // --- Persistence ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // --- Mapping ---
    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)

    // --- Test ---
    testImplementation(testFixtures(project(":caiman-debtor-core")))
}
