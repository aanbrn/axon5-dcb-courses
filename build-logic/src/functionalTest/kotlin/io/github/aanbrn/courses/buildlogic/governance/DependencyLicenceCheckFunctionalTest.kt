// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves the semantics of `DependencyLicenceCheck` over handcrafted SBOMs and policies (CICD-09, D-31). */
class DependencyLicenceCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(bom: String, policy: String = POLICY): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file("build.gradle.kts", BUILD_SCRIPT)
            file("bom.json", bom)
            file("policy.json", policy)
        }

    @Test
    fun `an allowed SPDX identifier passes`() {
        val result = fixture(bom(component("lib", id("Apache-2.0")))).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `alternatives pass when one of them is allowed`() {
        val dual = component("annotations", id("EPL-2.0"), id("GPL-2.0-with-classpath-exception"))

        val result = fixture(bom(dual)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a forbidden licence alone fails naming the coordinates and the licence`() {
        val output = fixture(bom(component("gpl", id("GPL-3.0-only")))).runAndFail(TASK).output

        output shouldContain "org.example:gpl:1.0"
        output shouldContain "'GPL-3.0-only' is forbidden"
    }

    @Test
    fun `an AND expression passes when every licence is allowed`() {
        val result = fixture(bom(component("both", expression("MIT AND Apache-2.0")))).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an AND expression fails when one licence is forbidden`() {
        val output = fixture(bom(component("mixed", expression("MIT AND GPL-3.0-only")))).runAndFail(TASK).output

        output shouldContain "org.example:mixed:1.0"
        output shouldContain "'GPL-3.0-only' is forbidden"
        output shouldNotContain "'MIT'"
    }

    @Test
    fun `an OR expression passes when one alternative is allowed`() {
        val either = component("either", expression("(GPL-3.0-only AND MIT) OR Apache-2.0"))

        val result = fixture(bom(either)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a malformed expression fails closed`() {
        val output = fixture(bom(component("broken", expression("MIT AND (Apache-2.0")))).runAndFail(TASK).output

        output shouldContain "org.example:broken:1.0"
        output shouldContain "not in the policy"
    }

    @Test
    fun `a component without a licence entry fails naming its coordinates`() {
        val output = fixture(bom(component("bare"))).runAndFail(TASK).output

        output shouldContain "org.example:bare:1.0: no licence entry in the SBOM"
    }

    @Test
    fun `an unknown licence name fails naming the text`() {
        val output = fixture(bom(component("odd", named("Some Custom Licence 1.0")))).runAndFail(TASK).output

        output shouldContain "org.example:odd:1.0"
        output shouldContain "'Some Custom Licence 1.0' is not in the policy"
    }

    @Test
    fun `an alias lets a free-text name pass`() {
        val bom = bom(component("legacy", named("The Apache Software License, Version 2.0")))
        val policy = policy(aliases = """{ "The Apache Software License, Version 2.0": "Apache-2.0" }""")

        val result = fixture(bom, policy).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an exception covers exactly its artifact and its licence text`() {
        val terms = named("Vendor Terms")
        val policy = policy(exceptions = exception("org.example:vendor", "Vendor Terms"))

        val covered = fixture(bom(component("vendor", terms)), policy).run(TASK)
        val otherArtifact = fixture(bom(component("other", terms)), policy).runAndFail(TASK)
        val otherText = fixture(bom(component("vendor", named("Vendor Terms 2"))), policy).runAndFail(TASK)

        covered.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
        otherArtifact.output shouldContain "org.example:other:1.0"
        otherText.output shouldContain "'Vendor Terms 2' is not in the policy"
    }

    @Test
    fun `violations are reported sorted by coordinates whatever the SBOM order`() {
        val components = listOf("zeta", "alpha", "mid").map { component(it, id("GPL-3.0-only")) }

        val fixture = fixture(bom(*components.toTypedArray()))
        val output = fixture.runAndFail(TASK).output

        output.indexOf("org.example:alpha:1.0") shouldBeLessThan output.indexOf("org.example:mid:1.0")
        output.indexOf("org.example:mid:1.0") shouldBeLessThan output.indexOf("org.example:zeta:1.0")
        val reported = fixture.read("build/report.txt").lines().filter { it.isNotEmpty() }
        reported shouldBe reported.sorted()
        reported shouldHaveSize components.size
    }

    @Test
    fun `a project dependency without a licence is not judged, the same shape from outside is`() {
        val project = component("module", purl = "pkg:maven/org.example/module@unspecified?project_path=%3Amodule")
        val external = component("module", purl = "pkg:maven/org.example/module@1.0")

        val result = fixture(bom(project)).run(TASK)
        val output = fixture(bom(external)).runAndFail(TASK).output

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
        output shouldContain "org.example:module:1.0: no licence entry in the SBOM"
    }

    @Test
    fun `an exception for a pattern is rejected`() {
        val policy = policy(exceptions = exception("org.example:*", "Vendor Terms"))

        val output = fixture(bom(component("vendor", named("Vendor Terms"))), policy).runAndFail(TASK).output

        output shouldContain "exception 'org.example:*' must name exactly one artifact as group:name"
    }

    @Test
    fun `an exception without a written reason is rejected`() {
        val policy = policy(exceptions = exception("org.example:vendor", "Vendor Terms", reason = " "))

        val output = fixture(bom(component("vendor", named("Vendor Terms"))), policy).runAndFail(TASK).output

        output shouldContain "exception 'org.example:vendor' needs a written reason"
    }

    @Test
    fun `an alias that resolves to a forbidden licence is rejected`() {
        val policy = policy(aliases = """{ "Totally Permissive": "GPL-3.0-only" }""")

        val output = fixture(bom(component("lib", id("MIT"))), policy).runAndFail(TASK).output

        output shouldContain "alias 'Totally Permissive' resolves to the forbidden licence 'GPL-3.0-only'"
    }

    @Test
    fun `a module with the library convention writes its own SBOM and passes under the isolated projects flags`() {
        val fixture = GradleFixture.standard(projectDir)

        val first = fixture.run(":lib:cyclonedxDirectBom", ":lib:checkDependencyLicences")
        val second = fixture.run(":lib:cyclonedxDirectBom", ":lib:checkDependencyLicences")

        first.task(":lib:cyclonedxDirectBom")?.outcome shouldBeIn GradleFixture.PASSED
        first.task(":lib:checkDependencyLicences")?.outcome shouldBeIn GradleFixture.PASSED
        fixture.read("lib/build/licences/bom.json") shouldContain "kotlin-stdlib"
        second.task(":lib:checkDependencyLicences")?.outcome shouldBeIn
            setOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE)
    }

    private fun bom(vararg components: String): String =
        """{ "bomFormat": "CycloneDX", "components": [${components.joinToString(",")}] }"""

    private fun component(name: String, vararg licences: String, purl: String? = null): String {
        val purlField = purl?.let { """, "purl": "$it"""" }.orEmpty()
        return """{ "group": "org.example", "name": "$name", "version": "1.0", "licenses": [${licences.joinToString(",")}]$purlField }"""
    }

    private fun id(spdx: String): String = """{ "license": { "id": "$spdx" } }"""

    private fun named(name: String): String = """{ "license": { "name": "$name" } }"""

    private fun expression(text: String): String = """{ "expression": "$text" }"""

    private fun exception(artifact: String, license: String, reason: String = "Justified in the test."): String =
        """{ "artifact": "$artifact", "license": "$license", "reason": "$reason" }"""

    private fun policy(aliases: String = "{}", exceptions: String = ""): String =
        """
        {
          "allowed": ["Apache-2.0", "MIT", "EPL-2.0"],
          "forbidden": ["GPL-*", "AGPL-*"],
          "aliases": $aliases,
          "exceptions": [$exceptions]
        }
        """
            .trimIndent()

    private companion object {
        const val TASK = "checkLicences"

        const val POLICY = """{ "allowed": ["Apache-2.0", "MIT", "EPL-2.0"], "forbidden": ["GPL-*", "AGPL-*"] }"""

        val BUILD_SCRIPT =
            """
            ${GradleFixture.HEADER}
            import io.github.aanbrn.courses.buildlogic.governance.DependencyLicenceCheck

            plugins { id("courses.governance") }

            tasks.register<DependencyLicenceCheck>("$TASK") {
                bom.set(layout.projectDirectory.file("bom.json"))
                policy.set(layout.projectDirectory.file("policy.json"))
                report.set(layout.buildDirectory.file("report.txt"))
            }
            """
                .trimIndent() + "\n"
    }
}
