dependencies {
    implementation(project(":caiman-billing-core"))
    implementation(project(":caiman-contracts"))

    implementation(libs.mapstruct)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.mapstruct.processor)
}
