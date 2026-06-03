dependencies {
    implementation(project(":caiman-payment-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.mapstruct:mapstruct:1.6.2")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.2")
}
