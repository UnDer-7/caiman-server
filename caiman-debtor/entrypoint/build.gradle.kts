dependencies {
    implementation(project(":caiman-debtor-core"))
    implementation(project(":caiman-contracts"))
    implementation(project(":caiman-web-support"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
