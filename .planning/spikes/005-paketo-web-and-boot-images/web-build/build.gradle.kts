import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

// No application here: the Spring Boot plugin is applied only for its BootBuildImage task type, so the frontend
// image is built by the same Gradle/buildpack toolchain as the services, with no pack CLI.
plugins { id("org.springframework.boot") apply false }

val variants = mapOf("custom" to "../web-custom", "generated" to "../web-generated")

variants.forEach { (name, dir) ->
    val app = tasks.register<Zip>("${name}WebApp") {
        description = "Packages the prebuilt dist/ and nginx configuration as buildpack application content."
        group = "spike"
        from(dir)
        archiveFileName.set("web-$name.zip")
        destinationDirectory.set(layout.buildDirectory.dir("web"))
    }
    tasks.register<BootBuildImage>("${name}WebImage") {
        description = "Builds the $name nginx image with the Paketo Web Servers buildpack."
        group = "spike"
        archiveFile.set(app.flatMap { it.archiveFile })
        // Docker 29 containerd image store cannot `docker save --platform` the full noble run image (moby#52193),
        // so the lifecycle exports straight to a registry instead of the daemon when SPIKE_REGISTRY is set.
        val registry = System.getenv("SPIKE_REGISTRY")
        imageName.set(if (registry != null) "$registry/spike005-web-$name:latest" else "spike005-web-$name:latest")
        publish.set(registry != null)
        builder.set("paketobuildpacks/ubuntu-noble-builder")
        buildpacks.set(listOf("paketo-buildpacks/web-servers", "docker.io/paketobuildpacks/health-checker:latest"))
        environment.set(
            if (name == "generated") {
                mapOf(
                    "BP_WEB_SERVER" to "nginx",
                    "BP_WEB_SERVER_ROOT" to "dist",
                    "BP_WEB_SERVER_ENABLE_PUSH_STATE" to "true",
                    "BP_WEB_SERVER_INCLUDE_FILE_PATH" to "proxy.conf",
                    "BP_HEALTH_CHECKER_ENABLED" to "true",
                )
            } else {
                mapOf(
                    "BPE_DEFAULT_API_UPSTREAM" to "http://api:8080",
                    "BPE_DEFAULT_PROXY_BUFFERING" to "off",
                    "BPE_DEFAULT_THC_PATH" to "/healthz",
                    "BP_HEALTH_CHECKER_ENABLED" to "true",
                )
            }
        )
    }
}
