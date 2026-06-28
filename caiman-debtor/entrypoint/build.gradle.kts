dependencies {
    // --- Internal: core ---
    implementation(project(":caiman-debtor-core"))

    // --- Internal: external-libraries ---
    implementation(project(":caiman-contracts"))
    implementation(project(":caiman-web-support"))
    implementation(project(":caiman-mapper-common"))

    // --- Web ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    // --- Mapping ---
    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
