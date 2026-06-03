dependencies {
    // ### Database ###
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // ----------------

    implementation(project(":caiman-debtor-core"))
    implementation(project(":caiman-shared"))
}
