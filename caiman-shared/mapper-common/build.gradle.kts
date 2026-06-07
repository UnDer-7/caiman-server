dependencies {
    implementation(project(":caiman-contracts"))
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    compileOnly("org.springframework:spring-context")
}
