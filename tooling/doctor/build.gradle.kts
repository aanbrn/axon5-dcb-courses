// SPDX-License-Identifier: MIT
plugins { id("courses.kotlin-library") }

dependencies {
    // The probe reaches Docker through Testcontainers itself, the way every test does, so it shares the version and the
    // configuration (`~/.testcontainers.properties`, `DOCKER_HOST`, Ryuk) with the test infrastructure.
    implementation(project(":tooling:test-support"))
}

val testcontainersProbe =
    tasks.register<JavaExec>("testcontainersProbe") {
        group = "help"
        description = "Checks Testcontainers connectivity through the Testcontainers library; changes nothing."
        mainClass.set("io.github.aanbrn.courses.doctor.DoctorKt")
        classpath = sourceSets.main.get().runtimeClasspath
        // The catalog pins the image (INFRA-09): the probe starts the same Ryuk reaper as the tests.
        environment("TESTCONTAINERS_RYUK_CONTAINER_IMAGE", libs.versions.image.ryuk.get())
        // The result depends on the environment (Docker, user configuration), not on declared inputs, so it runs on
        // every build.
        outputs.upToDateWhen { false }
    }

// The one command that diagnoses the JVM side of the environment. It delegates to the checks that already exist as root
// tasks (one implementation per check, D-48) and adds the Testcontainers probe. The tasks are named by path, so no
// other project's model is touched under isolated projects.
tasks.register("doctor") {
    group = "help"
    description =
        "Runs every JVM-side environment check: Node and formatter binaries, daemon JVM consistency and Testcontainers " +
            "connectivity; changes nothing."
    dependsOn(testcontainersProbe, ":verifyNodeVersion", ":verifyShfmt", ":verifyTaplo", ":checkDaemonJvm")
}
