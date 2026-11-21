// SPDX-License-Identifier: MIT
plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.foojay.resolver.plugin)
    // Required for the typed `libs` accessor inside precompiled script plugins (still needed on
    // Gradle 9.9.2).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
