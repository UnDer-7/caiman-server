dependencies {
    // ### Database ###
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // ----------------

    implementation(project(":caiman-debtor-core"))
    testImplementation(testFixtures(project(":caiman-debtor-core")))
    implementation(project(":caiman-contracts"))
    implementation(project(":caiman-mapper-common"))
    implementation(project(":caiman-jpa-support"))

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(project(":caiman-mapper-spi"))
}
