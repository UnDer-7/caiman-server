dependencies {
    implementation(project(":caiman-billing-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
