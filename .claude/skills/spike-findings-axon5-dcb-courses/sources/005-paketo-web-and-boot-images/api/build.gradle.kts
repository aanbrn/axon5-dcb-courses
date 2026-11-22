plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}

tasks.bootBuildImage {
    imageName.set("spike005-api:latest")
    // Docker is discovered from the active docker context (colima works without docker {} config).
    // Default builder for Boot 4.1 is paketobuildpacks/builder-noble-java-tiny (multi-arch, so native arm64 on colima).
    environment.set(
        mapOf(
            "BP_JVM_VERSION" to "25",
            // Runtime defaults baked into the image, overridable at `docker run -e`:
            "BPE_DEFAULT_SERVER_PORT" to "8080",
            // Liberica already adds -XX:+ExitOnOutOfMemoryError; JVM memory defaults stay overridable at runtime.
            "BPE_DEFAULT_BPL_JVM_THREAD_COUNT" to "50",
            "BPE_DEFAULT_BPL_JAVA_NMT_ENABLED" to "false",
            // DEFAULT loses to the JAVA_TOOL_OPTIONS Liberica sets; PREPEND keeps a later runtime value winning.
            "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
            "BPE_PREPEND_JAVA_TOOL_OPTIONS" to "-XX:ReservedCodeCacheSize=64M",
            // Health checker (RES-07): shell-less image health check process.
            "BP_HEALTH_CHECKER_ENABLED" to "true",
            "BPE_DEFAULT_THC_PATH" to "/actuator/health/readiness",
            "BPE_DEFAULT_THC_PORT" to "8080",
            // Image labels (INFRA-17): document env defaults where `docker image inspect` can see them.
            "BP_OCI_DESCRIPTION" to "Spike 005 API",
            "BP_IMAGE_LABELS" to "io.github.aanbrn.courses.env.SERVER_PORT=8080 io.github.aanbrn.courses.env.BPL_JVM_THREAD_COUNT=50",
        )
    )
    buildpacks.set(listOf("urn:cnb:builder:paketo-buildpacks/java", "docker.io/paketobuildpacks/health-checker:latest"))
}
