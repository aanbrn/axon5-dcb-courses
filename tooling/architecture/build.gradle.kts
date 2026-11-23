// SPDX-License-Identifier: MIT
plugins { id("courses.architecture-tests") }

dependencies {
    // The scope builders in `main` return Konsist scopes, so the rules and the scopes share one Konsist version.
    implementation(libs.konsist)
}
