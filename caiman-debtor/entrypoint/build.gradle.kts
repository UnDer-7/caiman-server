dependencies {
    implementation(project(":caiman-debtor-core"))
    implementation(project(":caiman-contracts"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
