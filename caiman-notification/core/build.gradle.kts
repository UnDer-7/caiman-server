plugins {
    `java-test-fixtures`
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":caiman-contracts"))
}
