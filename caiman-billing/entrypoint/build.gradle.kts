dependencies {
    implementation(project(":caiman-billing-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
