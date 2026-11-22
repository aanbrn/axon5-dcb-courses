// Standalone spike build; not part of the main build. Run with ../../gradlew -p .planning/spikes <task>.
rootProject.name = "spikes"

dependencyResolutionManagement {
    repositories { mavenCentral() }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}

include(":003-probe")
project(":003-probe").projectDir = file("003-axon-server-compose-dcb-hostname/probe")

include(":001-capture")
project(":001-capture").projectDir = file("001-decision-record-capture")
