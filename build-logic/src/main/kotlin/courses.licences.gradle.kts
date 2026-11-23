// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.governance.DependencyLicenceCheck

// Dependency licences (CICD-09, D-30, D-31). Every Kotlin module writes a CycloneDX SBOM of its runtime classpaths and
// checks each component against `governance/dependency-licences.json` from `check`.
//
// The SBOM plugin is applied per module here and never from the root project or `allprojects`: that is the combination
// that works under the configuration cache and isolated projects, and a root task cannot read the outputs of other
// projects under isolated projects. The check therefore runs per module, and every task writes only below its own
// module's build directory. The SBOMs are also the input Phase 6 reuses for image SBOMs.

plugins { id("org.cyclonedx.bom") }

// The root directory through the isolated-projects view, the only safe way to read root files under isolated
// projects. Gradle still marks `Project.isolated` @Incubating; a Gradle upgrade that changes it fails this build
// in its Renovate pull request.
@Suppress("UnstableApiUsage") val rootDirectory: Directory = isolated.rootProject.projectDirectory

tasks.cyclonedxDirectBom {
    // Only what ships or runs: the compiler, analysis and documentation tool classpaths are not dependencies of the
    // product, and they would drown the check in tooling components.
    includeConfigs = listOf(".*RuntimeClasspath")
    jsonOutput = layout.buildDirectory.file("licences/bom.json").get().asFile
    xmlOutput.unsetConvention()
    // A serial number changes on every run and would make the SBOM, a task output, differ between identical builds.
    includeBomSerialNumber = false
}

val checkDependencyLicences =
    tasks.register<DependencyLicenceCheck>("checkDependencyLicences") {
        group = "verification"
        description = "Fails when a runtime dependency's licence is not allowed by governance/dependency-licences.json."
        bom.set(tasks.cyclonedxDirectBom.flatMap { it.jsonOutput })
        policy.set(rootDirectory.file("governance/dependency-licences.json"))
        report.set(layout.buildDirectory.file("reports/licences/dependency-licences.txt"))
    }

tasks.named("check") { dependsOn(checkDependencyLicences) }
