dependencies {
    implementation(project(":caiman-notification-core"))
    implementation(project(":caiman-contracts"))

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
