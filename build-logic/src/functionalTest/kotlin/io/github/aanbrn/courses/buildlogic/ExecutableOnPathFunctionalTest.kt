// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.github.aanbrn.courses.buildlogic.GradleFixture.Companion.HEADER
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves that the build resolves `node`, `taplo` and `shfmt` from each build's own `PATH`,
 * including in a reused Gradle daemon, and that the formatter runs the binary the guard verified.
 */
class ExecutableOnPathFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @TempDir lateinit var toolsDir: Path

    @Test
    fun `a node switched on PATH between builds of one daemon is the node verifyNodeVersion checks`() {
        val fixture = GradleFixture.standard(projectDir)
        // A per-test temporary directory makes this daemon incompatible with every other daemon, so
        // build 1 is its first build and nothing earlier has initialised its process launcher.
        val daemonTmp = Files.createDirectories(toolsDir.resolve("daemon-tmp"))
        fixture.file(
            "gradle.properties",
            "org.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=384m " +
                "-Djava.io.tmpdir=${daemonTmp.toAbsolutePath()}\n",
        )
        fixture.file(
            "build.gradle.kts",
            """
            $HEADER
            plugins { id("courses.root") }

            tasks.register("daemonPid") {
                doLast { println("daemon-pid=" + ProcessHandle.current().pid()) }
            }
            """
                .trimIndent() + "\n",
        )
        val log = toolsDir.resolve("node-calls.log")
        val loggingDir = toolsDir.resolve("logging")
        executable(
            loggingDir,
            "node",
            "#!/bin/sh\necho \"$*\" >> '$log'\nexec '${realExecutable("node")}' \"$@\"\n",
        )
        val oldDir = toolsDir.resolve("old")
        executable(oldDir, "node", "#!/bin/sh\necho v1.0.0\n")

        val first = fixture.run("daemonPid", "verifyNodeVersion", path = onPath())
        val second = fixture.run("daemonPid", "verifyNodeVersion", path = onPath(loggingDir))

        // The builds shared one daemon, so only the lookup of `node` can differ between them.
        daemonPid(second) shouldBe daemonPid(first)
        lines(log) shouldContain "--version"

        // The failing build comes last: on the unchanged code it cannot fail, which is the bug.
        val third =
            fixture.runAndFail(
                "daemonPid",
                "verifyNodeVersion",
                "--continue",
                path = onPath(oldDir),
            )

        daemonPid(third) shouldBe daemonPid(first)
        third.output shouldContain "Node.js 1 is active"
        third.output shouldContain ".nvmrc"
    }

    @Test
    fun `Prettier runs the node verifyNodeVersion checked even with no npm beside it`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file("data/formatted.json", "{ \"name\": \"x\" }\n")
        val log = toolsDir.resolve("node-calls.log")
        val loggingDir = toolsDir.resolve("logging")
        executable(
            loggingDir,
            "node",
            "#!/bin/sh\necho \"$*\" >> '$log'\nexec '${realExecutable("node")}' \"$@\"\n",
        )

        val result =
            fixture.run("spotlessPrettierCheck", "--no-build-cache", path = onPath(loggingDir))

        result.task(":spotlessPrettier")?.outcome shouldBe TaskOutcome.SUCCESS
        val calls = lines(log)
        calls shouldContain "--version"
        calls.any { it.contains("serve.js") } shouldBe true
    }

    @Test
    fun `without node on PATH configuration succeeds and verifyNodeVersion fails before Prettier`() {
        val fixture = GradleFixture.standard(projectDir)

        fixture.runWithout("node", "help").task(":help")?.outcome shouldBe TaskOutcome.SUCCESS

        val result = fixture.runAndFailWithout("node", "spotlessPrettierCheck")

        result.task(":verifyNodeVersion")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain "Node.js is not available on PATH"
        result.output shouldContain "mise install"
        result.task(":spotlessPrettier") shouldBe null
    }

    private fun executable(dir: Path, name: String, script: String): Path {
        Files.createDirectories(dir)
        val file = Files.writeString(dir.resolve(name), script)
        check(file.toFile().setExecutable(true)) { "Cannot make $file executable" }
        return file
    }

    /**
     * The absolute path of the first executable regular file called [name] on the test JVM's
     * `PATH`, found independently of the production resolver.
     */
    private fun realExecutable(name: String): String =
        System.getenv("PATH")
            .orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotEmpty() && File(it).isAbsolute }
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath ?: error("No executable '$name' on the test JVM's PATH")

    /** The test JVM's `PATH` with [first] put in front of it. */
    private fun onPath(vararg first: Path): String =
        (first.map { it.toAbsolutePath().toString() } + System.getenv("PATH").orEmpty())
            .joinToString(File.pathSeparator)

    private fun daemonPid(result: BuildResult): String =
        result.output.lines().single { it.startsWith("daemon-pid=") }

    /** The lines of [log], or none when nothing has written it. */
    private fun lines(log: Path): List<String> =
        if (Files.exists(log)) Files.readAllLines(log) else emptyList()
}
