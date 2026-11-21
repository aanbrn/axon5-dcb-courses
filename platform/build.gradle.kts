// SPDX-License-Identifier: MIT
plugins { `java-platform` }

javaPlatform { allowDependencies() }

dependencies {
    api(platform(libs.spring.boot.bom))
    api(platform(libs.axon.framework.bom))
    api(platform(libs.axoniq.framework.bom))
    api(platform(libs.testcontainers.bom))
    api(platform(libs.jackson.bom))
    api(platform(libs.kotlin.bom))
    api(platform(libs.junit.bom))
    api(platform(libs.kotest.bom))

    constraints {
        // Deliberate override: Spring Boot manages an older Kotlin than the one this build compiles
        // with. The catalog's Kotlin wins, because Gradle selects the highest requested version.
        api(libs.kotlin.stdlib)
    }
}
