dependencies {
    implementation(project(":caiman-notification-core"))
    implementation(project(":caiman-shared"))

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
