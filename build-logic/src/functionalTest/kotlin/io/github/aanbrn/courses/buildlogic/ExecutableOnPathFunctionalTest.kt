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
 * Proves that the build resolves `node`, `taplo` and `shfmt` from each build's own `PATH`, including in a reused Gradle
 * daemon, and that the formatter runs the binary the guard verified.
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

        val result = fixture.run("spotlessPrettierCheck", "--no-build-cache", path = onPath(loggingDir))

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

    @Test
    fun `the configuration cache survives a PATH change that resolves the same node and not one that does not`() {
        val fixture = GradleFixture.standard(projectDir)
        // The relative entry's directory exists under the fixture root, so builds A and B see the
        // same project tree. TestKit starts the daemon's build from the test JVM's working
        // directory, not the fixture root, so the entry is written relative to that directory.
        val relativeBin = executable(projectDir.resolve("relative-bin"), "node", NODE_V1).parent
        val relativeEntry = Path.of("").toAbsolutePath().relativize(relativeBin.toAbsolutePath()).toString()
        check(!File(relativeEntry).isAbsolute) { "The relative entry '$relativeEntry' is absolute" }
        fixture.file(
            "build.gradle.kts",
            """
            $HEADER
            plugins { id("courses.root") }

            // Realises the value source at configuration time, which `courses.root` does only for
            // graphs that hold Spotless tasks (which read `PATH` themselves).
            val resolvedNode =
                providers
                    .of(io.github.aanbrn.courses.buildlogic.ExecutableOnPath::class.java) {
                        parameters.executableName.set("node")
                    }
                    .orNull

            tasks.register("relativeProbe") {
                val node = resolvedNode
                doLast {
                    println("resolved-node=" + node)
                    val relative = File("$relativeEntry", "node")
                    println("relative-node-visible=" + (relative.isFile && relative.canExecute()))
                }
            }
            """
                .trimIndent() + "\n",
        )
        val nonExecutableDir = toolsDir.resolve("non-executable")
        Files.createDirectories(nonExecutableDir)
        Files.writeString(nonExecutableDir.resolve("node"), NODE_V1)
        val directoryDir = toolsDir.resolve("directory")
        Files.createDirectories(directoryDir.resolve("node"))
        val otherNodeDir = toolsDir.resolve("other-node")
        executable(otherNodeDir, "node", NODE_V1)
        val skippedEntries =
            listOf(
                    relativeEntry,
                    nonExecutableDir.toAbsolutePath().toString(),
                    "",
                    directoryDir.toAbsolutePath().toString(),
                    System.getenv("PATH").orEmpty(),
                )
                .joinToString(File.pathSeparator)

        val a = fixture.run("relativeProbe", path = onPath())
        val b = fixture.run("relativeProbe", path = skippedEntries)
        val c = fixture.run("relativeProbe", path = onPath(otherNodeDir))

        a.output shouldContain "Configuration cache entry stored"
        // The control: the resolver would find this file if it honoured relative entries.
        b.output shouldContain "relative-node-visible=true"
        b.output shouldContain "Configuration cache entry reused"
        c.output shouldContain
            "configuration cache cannot be reused because a build logic input of type " +
                "'ExecutableOnPath' has changed"
    }

    @Test
    fun `a taplo or shfmt switched on PATH invalidates a configuration cache that realises the resolver`() {
        val fixture = GradleFixture.standard(projectDir)
        // The fixture script realises the value source at configuration time for both tools, so
        // the test proves the value source and not the lazy production wiring, which Spotless
        // graphs realise while Spotless reads `PATH` itself.
        fixture.file(
            "build.gradle.kts",
            """
            $HEADER
            plugins { id("courses.root") }

            val taplo =
                providers
                    .of(io.github.aanbrn.courses.buildlogic.ExecutableOnPath::class.java) {
                        parameters.executableName.set("taplo")
                    }
                    .orNull
            val shfmt =
                providers
                    .of(io.github.aanbrn.courses.buildlogic.ExecutableOnPath::class.java) {
                        parameters.executableName.set("shfmt")
                    }
                    .orNull
            """
                .trimIndent() + "\n",
        )
        val taploDir = toolsDir.resolve("taplo")
        executable(taploDir, "taplo", "#!/bin/sh\nexit 0\n")
        val shfmtDir = toolsDir.resolve("shfmt")
        executable(shfmtDir, "shfmt", "#!/bin/sh\nexit 0\n")
        val changed = "a build logic input of type 'ExecutableOnPath' has changed"

        val a = fixture.run("help", path = onPath())
        val b = fixture.run("help", path = onPath(taploDir))
        val c = fixture.run("help", path = onPath(shfmtDir, taploDir))

        a.output shouldContain "Configuration cache entry stored"
        b.output shouldContain changed
        c.output shouldContain changed
    }

    private fun executable(dir: Path, name: String, script: String): Path {
        Files.createDirectories(dir)
        val file = Files.writeString(dir.resolve(name), script)
        check(file.toFile().setExecutable(true)) { "Cannot make $file executable" }
        return file
    }

    /**
     * The absolute path of the first executable regular file called [name] on the test JVM's `PATH`, found
     * independently of the production resolver.
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
        (first.map { it.toAbsolutePath().toString() } + System.getenv("PATH").orEmpty()).joinToString(
            File.pathSeparator,
        )

    private fun daemonPid(result: BuildResult): String = result.output.lines().single { it.startsWith("daemon-pid=") }

    /** The lines of [log], or none when nothing has written it. */
    private fun lines(log: Path): List<String> = if (Files.exists(log)) Files.readAllLines(log) else emptyList()

    private companion object {
        /** A `node` stand-in that reports Node.js 1 and does nothing else. */
        const val NODE_V1 = "#!/bin/sh\necho v1.0.0\n"
    }
}
