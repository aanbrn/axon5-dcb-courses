// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.governance.ConfigPolicyCheck

// Repository governance checks (D-13): cacheable build-logic tasks wired into `check`, so a local
// build and CI enforce the same rules. The plugin is applied to the root project only.

plugins { base }

val reports = layout.buildDirectory.dir("reports/governance")

val checkGsdConfigPolicy =
    tasks.register<ConfigPolicyCheck>("checkGsdConfigPolicy") {
        group = "verification"
        description =
            "Fails when .planning/config.json deviates from governance/gsd-config-policy.json."
        config.set(layout.projectDirectory.file(".planning/config.json"))
        policy.set(layout.projectDirectory.file("governance/gsd-config-policy.json"))
        report.set(reports.map { it.file("gsd-config-policy.txt") })
    }

tasks.named("check") { dependsOn(checkGsdConfigPolicy) }
