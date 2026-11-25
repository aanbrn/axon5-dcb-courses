import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins { kotlin("jvm") }

// Mirror the main build's compiler flags exactly (no -Xannotation-default-target override) so that
// annotation use-site behaviour observed here is what production modules get.
extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict", "-Xjspecify-annotations=strict"))
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation("org.axonframework:axon-eventsourcing")
    implementation("io.axoniq.framework:axon-server-connector")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    runtimeOnly("ch.qos.logback:logback-classic")
    testImplementation("tools.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("io.axoniq.framework:axoniq-spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson")
    testImplementation("io.axoniq.framework:axoniq-testcontainer")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.kotest:kotest-assertions-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    if (providers.gradleProperty("withAxonKotlin").isPresent) testRuntimeOnly("org.axonframework.extensions.kotlin:axon-kotlin")
}

tasks.withType<Test>().configureEach {
    systemProperty("withAxonKotlin", providers.gradleProperty("withAxonKotlin").isPresent)
    outputs.upToDateWhen { false }
}
