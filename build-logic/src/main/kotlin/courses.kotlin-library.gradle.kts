// SPDX-License-Identifier: MIT
plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// Every `implementation` and `*Implementation` configuration (main, test, and the test suites and
// fixtures later phases add) is aligned through the `:platform` module (INFRA-11).
val platformProject = dependencies.project(":platform")

configurations
    .matching { it.name == "implementation" || it.name.endsWith("Implementation") }
    .configureEach { this.dependencies.add(project.dependencies.platform(platformProject)) }

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotest.assertions.core)
}

// `.java-version` is the only place the JDK version lives (INFRA-09).
val javaLanguageVersion: Provider<JavaLanguageVersion> =
    providers
        .fileContents(isolated.rootProject.projectDirectory.file(".java-version"))
        .asText
        .map { text ->
            val major =
                text.trim().substringBefore('.').toIntOrNull()
                    ?: throw GradleException(
                        ".java-version must hold a numeric JDK major version, but it holds '${text.trim()}'"
                    )
            JavaLanguageVersion.of(major)
        }

kotlin {
    jvmToolchain { languageVersion.set(javaLanguageVersion) }
    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xjspecify-annotations=strict")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
