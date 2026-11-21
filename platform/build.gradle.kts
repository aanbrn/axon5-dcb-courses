// SPDX-License-Identifier: MIT
plugins { `java-platform` }

javaPlatform { allowDependencies() }

dependencies {
    api(platform(libs.kotlin.bom))
    api(platform(libs.junit.bom))
    api(platform(libs.kotest.bom))
}
