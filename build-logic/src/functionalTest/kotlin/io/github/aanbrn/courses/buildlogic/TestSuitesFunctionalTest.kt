// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.github.aanbrn.courses.buildlogic.GradleFixture.Companion.PASSED
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element

/** Proves that `courses.test-suites` gives a module three suites that `check` runs, under the configuration cache. */
class TestSuitesFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `check depends on the unit, component and integration suites`() {
        val fixture = GradleFixture.testSuites(projectDir)

        val output = fixture.run(":lib:check", "--dry-run").output

        withClue(output) {
            SUITE_TASKS.forEach { output shouldContain it }
        }
    }

    @Test
    fun `a test in every suite can call a testFixtures class and check passes`() {
        val fixture = GradleFixture.testSuites(projectDir)
        SUITES.forEach { suite ->
            fixture.addTest(
                suite,
                "${suite.replaceFirstChar(Char::uppercase)}SampleTest",
                "the suite reads a class from testFixtures",
                "check(fixture.fixtures.Samples.sample() == \"sample\")",
            )
        }

        val result = fixture.run(":lib:check")

        SUITE_TASKS.forEach { task -> outcome(result, task) shouldBeIn PASSED }
        SUITES.forEach { suite ->
            fixture.testCount(suite, "${suite.replaceFirstChar(Char::uppercase)}SampleTest") shouldBe 1
        }
    }

    @Test
    fun `a module without java-test-fixtures has no testFixtures source set`() {
        val fixture = GradleFixture.testSuites(projectDir)
        fixture.file("lib/build.gradle.kts", "${GradleFixture.HEADER}\nplugins { id(\"courses.test-suites\") }\n")

        val output = fixture.run(":lib:tasks", "--all").output

        withClue(output) {
            output shouldContain "compileTestKotlin"
            output shouldNotContain "compileTestFixturesKotlin"
        }
    }

    @Test
    fun `a failing integration test fails check`() {
        val fixture = GradleFixture.testSuites(projectDir)
        fixture.addTest(
            "integrationTest",
            "BrokenTest",
            "an integration test that fails",
            "check(false) { \"broken\" }",
        )

        val result = fixture.runAndFail(":lib:check")

        withClue(result.output) {
            result.output shouldContain "Execution failed for task ':lib:integrationTest'"
            outcome(result, ":lib:integrationTest") shouldBe TaskOutcome.FAILED
        }
    }

    @Test
    fun `every suite classpath carries JUnit Jupiter, Kotest assertions and Kotest property`() {
        val fixture = GradleFixture.testSuites(projectDir)

        SUITES.forEach { suite ->
            val output = fixture.run(":lib:dependencies", "--configuration", "${suite}RuntimeClasspath").output

            withClue("$suite classpath:\n$output") {
                LIBRARIES.forEach { library -> output shouldContain library }
            }
        }
    }

    @Test
    fun `a module without component or integration sources reports NO-SOURCE for them and check stays green`() {
        val fixture = GradleFixture.testSuites(projectDir)
        fixture.addTest(
            "test",
            "OnlyUnitTest",
            "the only test of the module",
            "check(fixture.Library().add(40, 2) == 42)",
        )

        val result = fixture.run(":lib:check")

        outcome(result, ":lib:test") shouldBeIn PASSED
        outcome(result, ":lib:componentTest") shouldBe TaskOutcome.NO_SOURCE
        outcome(result, ":lib:integrationTest") shouldBe TaskOutcome.NO_SOURCE
    }

    @Test
    fun `within a module the suites run as test, componentTest and integrationTest`() {
        val fixture = GradleFixture.testSuites(projectDir)
        SUITES.forEach { suite ->
            fixture.addTest(
                suite,
                "${suite.replaceFirstChar(Char::uppercase)}OrderTest",
                "the suite runs",
                "check(true)",
            )
        }

        val executed = fixture.run(":lib:check", "--no-build-cache").tasks.map { it.path }

        withClue(executed.toString()) {
            executed.indexOf(":lib:test") shouldBeGreaterThan -1
            executed.indexOf(":lib:componentTest") shouldBeGreaterThan executed.indexOf(":lib:test")
            executed.indexOf(":lib:integrationTest") shouldBeGreaterThan executed.indexOf(":lib:componentTest")
        }
    }

    @Test
    fun `the component and integration suites run on a single fork`() {
        val fixture = GradleFixture.testSuites(projectDir)
        SUITES.forEach { suite ->
            fixture.addTest(
                suite,
                "${suite.replaceFirstChar(Char::uppercase)}ForkTest",
                "the suite runs",
                "check(true)",
            )
        }

        val output = fixture.run(":lib:check", "--no-build-cache").output

        output shouldContain "FORKS componentTest=1"
        output shouldContain "FORKS integrationTest=1"
    }

    @Test
    fun `prose test names without KDoc pass detekt in the component, integration and architecture suites`() {
        val fixture = GradleFixture.testSuites(projectDir)
        fixture.file(
            "lib/build.gradle.kts",
            fixture.read("lib/build.gradle.kts") + "\ndetekt { source.from(\"src/architectureTest/kotlin\") }\n",
        )
        listOf("componentTest", "integrationTest", "architectureTest").forEach { suite ->
            fixture.addTest(
                suite,
                "${suite.replaceFirstChar(Char::uppercase)}ProseTest",
                "a prose name with spaces",
                "check(true)",
            )
        }

        val result = fixture.run(":lib:detekt")

        outcome(result, ":lib:detekt") shouldBeIn PASSED
    }

    @Test
    fun `an undocumented public function in testFixtures fails detekt`() {
        val fixture = GradleFixture.testSuites(projectDir)
        fixture.file(
            "lib/src/testFixtures/kotlin/fixture/fixtures/Undocumented.kt",
            listOf(
                    GradleFixture.HEADER,
                    "package fixture.fixtures",
                    "",
                    "/** A documented holder. */",
                    "object Undocumented {",
                    "    fun hidden(): Int = 1",
                    "}",
                )
                .joinToString("\n", postfix = "\n"),
        )

        val result = fixture.runAndFail(":lib:detekt")

        result.output shouldContain "UndocumentedPublicFunction"
    }

    @Test
    fun `the Kover XML report is written under the configuration cache and isolated projects`() {
        val fixture = coverageFixture()

        val result = fixture.run(":lib:koverXmlReport")

        outcome(result, ":lib:koverXmlReport") shouldBeIn PASSED
        Files.exists(projectDir.resolve(KOVER_REPORT)) shouldBe true
    }

    @Test
    fun `code reached by each suite is reported covered and code reached by none is reported uncovered`() {
        val fixture = coverageFixture()
        fixture.run(":lib:koverXmlReport")

        val coveredLines = coveredLinesByMethod(fixture.read(KOVER_REPORT))

        withClue(coveredLines.toString()) {
            coveredLines.getValue("coveredByUnit") shouldBeGreaterThan 0
            coveredLines.getValue("coveredByComponent") shouldBeGreaterThan 0
            coveredLines.getValue("coveredByIntegration") shouldBeGreaterThan 0
            coveredLines.getValue("neverCalled") shouldBe 0
        }
    }

    @Test
    fun `a second report run with unchanged sources does not run the integration suite again`() {
        val fixture = coverageFixture()
        fixture.run(":lib:koverXmlReport")

        val second = fixture.run(":lib:koverXmlReport")

        outcome(second, ":lib:integrationTest") shouldBeIn setOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE)
    }

    private fun coverageFixture(): GradleFixture =
        GradleFixture.testSuites(projectDir).apply {
            file(
                "lib/src/main/kotlin/fixture/Coverage.kt",
                listOf(
                        GradleFixture.HEADER,
                        "package fixture",
                        "",
                        "/** Functions that exactly one suite reaches, and one that no test reaches. */",
                        "class Coverage {",
                        "    /** Called by the unit suite. */",
                        "    fun coveredByUnit(input: String): String = input.trim()",
                        "",
                        "    /** Called by the component suite. */",
                        "    fun coveredByComponent(input: String): String = input.trim()",
                        "",
                        "    /** Called by the integration suite. */",
                        "    fun coveredByIntegration(input: String): String = input.trim()",
                        "",
                        "    /** Called by no test. */",
                        "    fun neverCalled(input: String): String = input.trim()",
                        "}",
                    )
                    .joinToString("\n", postfix = "\n"),
            )
            mapOf(
                    "test" to "coveredByUnit",
                    "componentTest" to "coveredByComponent",
                    "integrationTest" to "coveredByIntegration",
                )
                .forEach { (suite, method) ->
                    addTest(
                        suite,
                        "${suite.replaceFirstChar(Char::uppercase)}CoverageTest",
                        "the suite calls $method",
                        "check(fixture.Coverage().$method(\" x \") == \"x\")",
                    )
                }
        }

    /** Maps each method name in a Kover XML report to the number of its covered lines. */
    private fun coveredLinesByMethod(report: String): Map<String, Int> {
        val factory = DocumentBuilderFactory.newInstance()
        // The report names a DTD that is not shipped with it; reading must not try to fetch it.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val methods = factory.newDocumentBuilder().parse(report.byteInputStream()).getElementsByTagName("method")
        return (0 until methods.length)
            .map { methods.item(it) as Element }
            .associate { method ->
                val lineCounters =
                    (0 until method.childNodes.length).map(method.childNodes::item).filterIsInstance<Element>().filter {
                        it.tagName == "counter" && it.getAttribute("type") == "LINE"
                    }
                method.getAttribute("name") to lineCounters.sumOf { it.getAttribute("covered").toInt() }
            }
    }

    private infix fun TaskOutcome?.shouldBeIn(outcomes: Set<TaskOutcome>) {
        outcomes shouldContain this
    }

    private fun outcome(result: BuildResult, path: String): TaskOutcome? = result.task(path)?.outcome

    private fun GradleFixture.testCount(suite: String, className: String): Int {
        val report = read("lib/build/test-results/$suite/TEST-fixture.$className.xml")
        val count = Regex("""tests="(\d+)"""").find(report)?.groupValues?.get(1)
        return checkNotNull(count) { "No test count in the $suite report of $className" }.toInt()
    }

    private companion object {
        val SUITES = listOf("test", "componentTest", "integrationTest")
        val SUITE_TASKS = SUITES.map { ":lib:$it" }
        val LIBRARIES = listOf("kotest-property", "kotest-assertions-core", "junit-jupiter")
        const val KOVER_REPORT = "lib/build/reports/kover/report.xml"
    }
}
