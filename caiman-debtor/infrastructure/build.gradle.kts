dependencies {
    // ### Database ###
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // ----------------

    implementation(project(":caiman-debtor-core"))
    implementation(project(":caiman-shared"))

    implementation("org.mapstruct:mapstruct:1.6.2")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.2")
}
