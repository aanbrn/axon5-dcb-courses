plugins { kotlin("jvm") }

dependencies {
    implementation("org.axonframework:axon-eventsourcing")
    implementation("io.axoniq.framework:axon-server-connector")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("ch.qos.logback:logback-classic")
    testImplementation("io.axoniq.framework:axoniq-testcontainer")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.kotest:kotest-assertions-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
