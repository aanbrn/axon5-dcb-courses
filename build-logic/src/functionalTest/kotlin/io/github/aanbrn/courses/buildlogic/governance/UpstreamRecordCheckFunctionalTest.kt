// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainInOrder
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkUpstreamRecords` validates the upstream records and keeps markers and records consistent. */
class UpstreamRecordCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(unrecorded: String = "emptySet()"): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(
                "build.gradle.kts",
                """
                ${GradleFixture.HEADER}
                import io.github.aanbrn.courses.buildlogic.governance.UpstreamRecordCheck

                plugins { id("courses.governance") }

                tasks.named<UpstreamRecordCheck>("$TASK") { unrecordedMarkers.set($unrecorded) }
                """
                    .trimIndent() + "\n",
            )
        }

    private fun GradleFixture.upstream(name: String, content: String) = file("docs/upstream/$name", content)

    private fun GradleFixture.fails(): String = runAndFail(TASK).output

    private fun GradleFixture.passes() {
        run(TASK).task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `zero records and zero markers pass and the index has only its header`() {
        val fixture = fixture()

        fixture.passes()

        fixture.read("build/reports/governance/upstream-index.md") shouldContain "| ID | Title |"
        fixture.read("build/reports/governance/upstream-index.md") shouldNotContain "| UP-"
        fixture.read("build/reports/governance/upstream-records.txt") shouldBe ""
    }

    @Test
    fun `a valid record with its marker passes and the index lists the records sorted by id`() {
        val fixture = fixture()
        fixture.upstream("0002-second.md", record("id" to "\"UP-0002\"", title = "UP-0002: Second | pipe"))
        fixture.upstream(
            "0001-first.md",
            record(
                "upstream" to "[\"https://github.com/o/r/issues/1\"]",
                "reported" to "2026-11-20",
                "status" to "reported",
            ),
        )
        fixture.file("src/First.kt", "// UPSTREAM: UP-0001 drop this when upstream fixes it\n")
        fixture.file("src/Second.kt", "/* UPSTREAM: UP-0002 same */\n")

        fixture.passes()

        fixture
            .read("build/reports/governance/upstream-index.md")
            .shouldContainInOrder(
                "| UP-0001 | Capture hook | axon | reported | yes | https://github.com/o/r/issues/1 | 2026-11-22 |",
                "| UP-0002 | Second \\| pipe | axon | open | yes | - | 2026-11-22 |",
            )
    }

    @Test
    fun `a record without a required key fails naming the file and the key`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("product-version" to null))
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 workaround\n")

        fixture.fails() shouldContain "docs/upstream/0001-a.md: missing front matter key `product-version`"
    }

    @Test
    fun `a record with an empty required key fails`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("product" to "\"\""))
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 workaround\n")

        fixture.fails() shouldContain "docs/upstream/0001-a.md:3: `product` is empty"
    }

    @Test
    fun `a record with a missing or empty required section fails`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record(sections = SECTIONS - "Root cause"))
        fixture.upstream("0002-b.md", record("id" to "\"UP-0002\"", title = "UP-0002: B", emptySection = "Symptom"))
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 workaround\n// UPSTREAM: UP-0002 workaround\n")

        val output = fixture.fails()

        output shouldContain "docs/upstream/0001-a.md:"
        output shouldContain "missing the section `## Root cause`"
        output shouldContain "the section `## Symptom` is empty"
    }

    @Test
    fun `a heading inside a code fence is not a section`() {
        val fixture = fixture()
        val fenced = record(sections = SECTIONS - "Root cause") + "\n```text\n## Root cause\n```\n"
        fixture.upstream("0001-a.md", fenced)
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 workaround\n")

        fixture.fails() shouldContain "missing the section `## Root cause`"
    }

    @Test
    fun `an id must match UP-NNNN exactly and the file name NNNN-slug`() {
        val fixture = fixture()
        fixture.upstream("0001-lower.md", record("id" to "\"up-0001\""))
        fixture.upstream("0002-short.md", record("id" to "\"UP-002\""))
        fixture.upstream("3-name.md", record())
        fixture.upstream("0004-Upper.md", record("id" to "\"UP-0004\""))

        val output = fixture.fails()

        output shouldContain "`id` must be UP-NNNN with four digits, found `up-0001`"
        output shouldContain "`id` must be UP-NNNN with four digits, found `UP-002`"
        output shouldContain "docs/upstream/3-name.md: the file name must match NNNN-<slug>.md"
        output shouldContain "docs/upstream/0004-Upper.md: the file name must match NNNN-<slug>.md"
    }

    @Test
    fun `two records with the same id fail naming both files`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record(workaround = "false"))
        fixture.upstream("0001-b.md", record(workaround = "false"))

        val output = fixture.fails()

        output shouldContain "docs/upstream/0001-a.md: duplicate id UP-0001, also in docs/upstream/0001-b.md"
        output shouldContain "docs/upstream/0001-b.md: duplicate id UP-0001, also in docs/upstream/0001-a.md"
    }

    @Test
    fun `a file number that differs from its id fails`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("id" to "\"UP-0002\"", "workaround" to "false", title = "UP-0002: A"))

        fixture.fails() shouldContain "`id` UP-0002 does not match the file number 0001"
    }

    @Test
    fun `an unknown status and an unknown key fail`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("status" to "\"closed\"", "owner" to "\"me\"", "workaround" to "false"))

        val output = fixture.fails()

        output shouldContain "`status` must be one of open, reported, fixed-upstream, workaround-removed, wont-fix"
        output shouldContain "unknown front matter key `owner`"
    }

    @Test
    fun `dates must be YYYY-MM-DD or null`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("updated" to "2026-13-40", "workaround" to "false"))
        fixture.upstream(
            "0002-b.md",
            record("id" to "\"UP-0002\"", "reported" to "yesterday", "workaround" to "false", title = "UP-0002: B"),
        )
        fixture.upstream(
            "0003-c.md",
            record("id" to "\"UP-0003\"", "updated" to "null", "workaround" to "false", title = "UP-0003: C"),
        )

        val output = fixture.fails()

        output shouldContain "docs/upstream/0001-a.md:9: `updated` must be a YYYY-MM-DD date or null"
        output shouldContain "docs/upstream/0002-b.md:8: `reported` must be a YYYY-MM-DD date or null"
        output shouldNotContain "0003-c.md"
    }

    @Test
    fun `a reported record needs an upstream link and a reported date`() {
        val fixture = fixture()
        val link = "[\"https://github.com/o/r/issues/1\"]"
        fixture.upstream("0001-a.md", record("status" to "reported", "reported" to "2026-11-20", "upstream" to "[]"))
        fixture.upstream(
            "0002-b.md",
            record(
                "id" to "\"UP-0002\"",
                "status" to "reported",
                "upstream" to link,
                "reported" to "null",
                title = "UP-0002: B",
            ),
        )
        fixture.upstream(
            "0003-c.md",
            record(
                "id" to "\"UP-0003\"",
                "status" to "reported",
                "upstream" to link,
                "reported" to "2026-11-20",
                title = "UP-0003: C",
            ),
        )
        fixture.file("src/Markers.kt", "// UPSTREAM: UP-0001 a\n// UPSTREAM: UP-0002 b\n// UPSTREAM: UP-0003 c\n")

        val output = fixture.fails()

        output shouldContain "docs/upstream/0001-a.md:7: status is reported, so `upstream` needs at least one link"
        output shouldContain
            "docs/upstream/0002-b.md:8: status is reported, so `reported` needs the date it was reported"
        output shouldNotContain "0003-c.md"
    }

    @Test
    fun `front matter outside the supported subset fails with the line`() {
        val cases =
            listOf(
                Triple(
                    "nested",
                    "---\nid: \"UP-0001\"\nupstream:\n  - https://x\n---\n",
                    "0001-a.md:3: unsupported front matter: `upstream` has no value",
                ),
                Triple(
                    "indented",
                    "---\nid: x\n  more: y\n---\n",
                    "0001-a.md:3: unsupported front matter: indented line",
                ),
                Triple("anchor", "---\nid: &a x\n---\n", "0001-a.md:2: unsupported front matter: `&` starts"),
                Triple(
                    "duplicate",
                    "---\nid: x\nid: y\n---\n",
                    "0001-a.md:3: unsupported front matter: duplicate key `id`",
                ),
                Triple(
                    "unclosed",
                    "---\nid: x\n",
                    "0001-a.md:1: unsupported front matter: there is no closing ---",
                ),
                Triple("single quote", "---\nid: 'x'\n---\n", "0001-a.md:2: unsupported front matter: `'` starts"),
                Triple(
                    "missing opening",
                    "id: x\n",
                    "0001-a.md:1: unsupported front matter: the file must start with",
                ),
                Triple("block scalar", "---\nid: |\n---\n", "0001-a.md:2: unsupported front matter: `|` starts"),
                Triple(
                    "unterminated list",
                    "---\nupstream: [a,\n---\n",
                    "0001-a.md:2: unsupported front matter: unterminated list",
                ),
            )
        cases.forEach { (name, content, expected) ->
            val fixture = fixture()
            fixture.upstream("0001-a.md", content)

            withClue(name) { fixture.fails() shouldContain expected }
            projectDir.resolve("docs").toFile().deleteRecursively()
        }
    }

    @Test
    fun `front matter keeps quoted keywords as strings and accepts comments and bare values`() {
        val fixture = fixture()
        val content =
            record(
                "id" to "UP-0001 # bare id with comment",
                "product" to "\"null\"",
                "product-version" to "5.4.2",
                "workaround" to "false",
                "upstream" to "[https://a.example/1, \"https://b.example/2\"] # two links",
            )
        fixture.upstream("0001-a.md", content)

        fixture.passes()

        fixture.read("build/reports/governance/upstream-index.md") shouldContain
            "| UP-0001 | Capture hook | null | open | no | https://a.example/1, https://b.example/2 | 2026-11-22 |"
    }

    @Test
    fun `a marker that names no record fails`() {
        val fixture = fixture()
        fixture.file("src/A.kt", "val x = 1\n// UPSTREAM: UP-0009 no record\n")

        fixture.fails() shouldContain "src/A.kt:2: UPSTREAM: UP-0009 names no record in docs/upstream/"
    }

    @Test
    fun `a marker with five digits or a trailing letter is malformed`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record())
        fixture.file(
            "src/A.kt",
            "// UPSTREAM: UP-0001 fine\n// UPSTREAM: UP-00010 too long\n// UPSTREAM: UP-0001x suffix\n",
        )
        fixture.file("src/b.sh", "# UPSTREAM:UP-0001 missing space\n# UPSTREAM: up-0001 lower case\n")

        val output = fixture.fails()

        output shouldContain "src/A.kt:2: malformed marker `UPSTREAM: UP-00010 too long`"
        output shouldContain "src/A.kt:3: malformed marker `UPSTREAM: UP-0001x suffix`"
        output shouldContain "src/b.sh:1: malformed marker `UPSTREAM:UP-0001 missing space`"
        output shouldContain "src/b.sh:2: malformed marker `UPSTREAM: up-0001 lower case`"
        output shouldNotContain "src/A.kt:1:"
    }

    @Test
    fun `an open record with a workaround and no marker fails and one without a workaround passes`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record())
        fixture.upstream("0002-b.md", record("id" to "\"UP-0002\"", "workaround" to "false", title = "UP-0002: B"))

        val output = fixture.fails()

        output shouldContain
            "docs/upstream/0001-a.md: workaround is true and status is open, but no UPSTREAM: UP-0001 marker"
        output shouldNotContain "0002-b.md"
    }

    @Test
    fun `fixed-upstream and wont-fix keep their markers while removed and workaround-free records have none`() {
        val fixture = fixture()
        fixture.upstream("0001-fixed.md", record("status" to "fixed-upstream"))
        fixture.upstream("0002-wont.md", record("id" to "\"UP-0002\"", "status" to "wont-fix", title = "UP-0002: B"))
        fixture.file("src/Keep.kt", "// UPSTREAM: UP-0001 still needed\n// UPSTREAM: UP-0002 permanent\n")
        fixture.passes()

        fixture.upstream(
            "0003-gone.md",
            record("id" to "\"UP-0003\"", "status" to "workaround-removed", title = "UP-0003: C"),
        )
        fixture.upstream("0004-none.md", record("id" to "\"UP-0004\"", "workaround" to "false", title = "UP-0004: D"))
        fixture.file("src/Stale.kt", "// UPSTREAM: UP-0003 stale\n// UPSTREAM: UP-0004 none\n")

        val output = fixture.fails()

        output shouldContain
            "src/Stale.kt:1: UPSTREAM: UP-0003 remains, but docs/upstream/0003-gone.md says status is workaround-removed"
        output shouldContain
            "src/Stale.kt:2: UPSTREAM: UP-0004 remains, but docs/upstream/0004-none.md says workaround is false"
        output shouldNotContain "src/Keep.kt"
    }

    @Test
    fun `a wont-fix record without a marker fails because its workaround is permanent`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record("status" to "wont-fix"))

        fixture.fails() shouldContain "workaround is true and status is wont-fix, but no UPSTREAM: UP-0001 marker"
    }

    @Test
    fun `records, templates, planning, claude and build output are not scanned for markers`() {
        val fixture = fixture()
        val example = "// UPSTREAM: UP-9999 an example\n"
        fixture.upstream("0001-a.md", record(workaround = "false") + "\n$example")
        fixture.file("docs/templates/upstream-record.md", "# UPSTREAM: UP-NNNN\n")
        fixture.file(".planning/notes.md", example)
        fixture.file(".claude/skills/x.md", example)
        fixture.file("module/build/generated/Gen.kt", example)
        fixture.file("build-logic/src/functionalTest/Fixture.kt", example)
        fixture.file(
            "src/Real.kt",
            "val s = \"UPSTREAM: UP-9998\"\n// see UPSTREAM: UP-9997 in prose\nUPSTREAM: UP-9996 bare\n",
        )

        fixture.passes()
    }

    @Test
    fun `problems are sorted by file and line`() {
        val fixture = fixture()
        fixture.file("src/b/Late.kt", "// UPSTREAM: UP-0003 x\n")
        fixture.file("src/a/Many.kt", "\n// UPSTREAM: UP-0002 x\n\n// UPSTREAM: UP-0001 x\n")

        val output = fixture.fails()

        output.shouldContainInOrder("src/a/Many.kt:2:", "src/a/Many.kt:4:", "src/b/Late.kt:1:")
    }

    @Test
    fun `the bridge tolerates exactly the listed markers and demands its own removal`() {
        val fixture = fixture("setOf(\"UP-0001\", \"UP-0002\")")
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 pending\n// UPSTREAM: UP-0003 not listed\n")

        val unlisted = fixture.fails()

        unlisted shouldContain "src/A.kt:2: UPSTREAM: UP-0003 names no record"
        unlisted shouldNotContain "src/A.kt:1:"
        unlisted shouldContain "unrecordedMarkers: no marker names UP-0002 any more; remove it from the list"

        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 pending\n// UPSTREAM: UP-0002 pending\n")
        fixture.passes()

        fixture.upstream("0001-a.md", record())
        fixture.fails() shouldContain
            "unrecordedMarkers: UP-0001 has a record (docs/upstream/0001-a.md); remove it from the list"
    }

    @Test
    fun `an unrecorded marker entry must be an UP-NNNN identifier`() {
        val fixture = fixture("setOf(\"UP-1\")")

        fixture.fails() shouldContain "unrecordedMarkers: `UP-1` is not an UP-NNNN identifier"
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a new violation is still reported`() {
        val fixture = fixture()
        fixture.upstream("0001-a.md", record())
        fixture.file("src/A.kt", "// UPSTREAM: UP-0001 workaround\n")
        fixture.passes()

        val second = fixture.run(TASK).task(":$TASK")?.outcome

        withClue("second run outcome") { listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second }
        fixture.file("src/A.kt", "val x = 1\n")
        fixture.fails() shouldContain "no UPSTREAM: UP-0001 marker exists"
    }

    private companion object {
        const val TASK = "checkUpstreamRecords"

        val SECTIONS =
            listOf(
                "Symptom",
                "Minimal reproduction",
                "Root cause",
                "Workaround",
                "Removal criteria",
                "How to verify removal",
            )

        val DEFAULTS: Map<String, String?> =
            linkedMapOf(
                "id" to "\"UP-0001\"",
                "product" to "\"axon\"",
                "product-version" to "\"5.4.2\"",
                "status" to "open",
                "workaround" to "true",
                "upstream" to "[]",
                "reported" to "null",
                "updated" to "\"2026-11-22\"",
            )

        /**
         * A valid record whose front matter values are the raw YAML text of [changes] over the defaults; a null value
         * drops the key. The first line of the body is the title heading, and every section of [sections] has one line
         * of text except [emptySection].
         */
        fun record(
            vararg changes: Pair<String, String?>,
            title: String = "UP-0001: Capture hook",
            sections: List<String> = SECTIONS,
            workaround: String? = null,
            emptySection: String? = null,
        ): String {
            val values =
                (DEFAULTS + changes.toList() + listOfNotNull(workaround?.let { "workaround" to it })).filterValues {
                    it != null
                }
            val front = values.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
            val body =
                sections.joinToString(separator = "\n") { name ->
                    if (name == emptySection) "## $name\n" else "## $name\n\nText for $name.\n"
                }
            return "---\n$front\n---\n\n# $title\n\n$body"
        }
    }
}
