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

/** Proves `checkAdrs` enforces the MADR front matter, the numbering and the structure of the ADRs. */
class AdrCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(): GradleFixture = GradleFixture.governance(projectDir)

    private fun GradleFixture.adr(name: String, content: String) = file("docs/adr/$name", content)

    private fun GradleFixture.fails(): String = runAndFail(TASK).output

    private fun GradleFixture.passes() {
        run(TASK).task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a valid pair of ADRs passes and an unchanged rerun is up to date`() {
        val fixture = fixture()
        fixture.adr("0001-first.md", adr())
        fixture.adr("0002-second.md", adr("status" to "proposed", title = "ADR-0002: Second decision"))
        fixture.passes()

        val second = fixture.run(TASK).task(":$TASK")?.outcome

        fixture.read("build/reports/governance/adrs.txt") shouldBe ""
        withClue("second run outcome") { listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second }
        fixture.adr(
            "0002-second.md",
            adr(title = "ADR-0002: Second decision", sections = SECTIONS - "Decision Outcome"),
        )
        fixture.fails() shouldContain "missing the section `## Decision Outcome`"
    }

    @Test
    fun `no ADR at all passes`() {
        val fixture = fixture()
        fixture.file("docs/adr/.gitkeep", "")

        fixture.passes()

        fixture.read("build/reports/governance/adrs.txt") shouldBe ""
    }

    @Test
    fun `two ADRs with the same number fail naming both files`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr())
        fixture.adr("0001-b.md", adr())

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md: duplicate number 0001, also in docs/adr/0001-b.md"
        output shouldContain "docs/adr/0001-b.md: duplicate number 0001, also in docs/adr/0001-a.md"
    }

    @Test
    fun `a gap in the numbering fails naming the missing number`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr())
        fixture.adr("0003-c.md", adr(title = "ADR-0003: Third decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr: ADR-0002 is missing; numbering runs from 0001 without gaps"
        output shouldNotContain "ADR-0001 is missing"
        output shouldNotContain "ADR-0003 is missing"
    }

    @Test
    fun `numbering must start at 0001 and 0000 is not a number`() {
        val fixture = fixture()
        fixture.adr("0000-zero.md", adr(title = "ADR-0000: Zero"))
        fixture.adr("0002-b.md", adr(title = "ADR-0002: Second decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr: ADR numbers start at 0001, not 0000"
        output shouldContain "docs/adr: ADR-0001 is missing; numbering runs from 0001 without gaps"
    }

    @Test
    fun `a title number that differs from the file name fails`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr(title = "ADR-0002: Wrong number"))

        fixture.fails() shouldContain
            "docs/adr/0001-a.md:6: the title heading names ADR-0002, but the file name says 0001"
    }

    @Test
    fun `a missing or malformed title heading fails`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr(title = "First decision"))
        fixture.adr("0002-b.md", adr().replace("# ADR-0001: First decision\n", ""))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:6: missing the title heading `# ADR-NNNN: <title>`"
        output shouldContain "docs/adr/0002-b.md:6: missing the title heading `# ADR-NNNN: <title>`"
    }

    @Test
    fun `a superseded status needs the superseding ADR to exist`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("status" to "superseded by ADR-0009"))
        fixture.adr("0002-b.md", adr("status" to "superseded by ADR-0003", title = "ADR-0002: Second decision"))
        fixture.adr("0003-c.md", adr(title = "ADR-0003: Third decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:2: `status` names ADR-0009, but no docs/adr/0009-<slug>.md exists"
        output shouldNotContain "0002-b.md"
    }

    @Test
    fun `a superseded status naming an existing ADR passes`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("status" to "superseded by ADR-0002"))
        fixture.adr("0002-b.md", adr(title = "ADR-0002: Second decision"))

        fixture.passes()
    }

    @Test
    fun `an ADR cannot supersede itself`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("status" to "superseded by ADR-0001"))

        fixture.fails() shouldContain "docs/adr/0001-a.md:2: `status` makes ADR-0001 supersede itself"
    }

    @Test
    fun `a status outside the allowed set fails`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("status" to "draft"))
        fixture.adr("0002-b.md", adr("status" to "superseded by ADR-2", title = "ADR-0002: Second decision"))
        fixture.adr("0003-c.md", adr("status" to "Accepted", title = "ADR-0003: Third decision"))

        val output = fixture.fails()

        val allowed = "`status` must be one of proposed, accepted, rejected, deprecated, or `superseded by ADR-NNNN`"
        output shouldContain "docs/adr/0001-a.md:2: $allowed, found `draft`"
        output shouldContain "docs/adr/0002-b.md:2: $allowed, found `superseded by ADR-2`"
        output shouldContain "docs/adr/0003-c.md:2: $allowed, found `Accepted`"
    }

    @Test
    fun `every allowed status passes`() {
        val fixture = fixture()
        listOf("proposed", "accepted", "rejected", "deprecated").forEachIndexed { index, status ->
            val number = "%04d".format(index + 1)
            fixture.adr("$number-$status.md", adr("status" to status, title = "ADR-$number: A $status decision"))
        }

        fixture.passes()
    }

    @Test
    fun `a missing required key fails naming the file and the key`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("decision-makers" to null))
        fixture.adr("0002-b.md", adr("status" to null, title = "ADR-0002: Second decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md: missing front matter key `decision-makers`"
        output shouldContain "docs/adr/0002-b.md: missing front matter key `status`"
    }

    @Test
    fun `an empty required key fails`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("date" to "\"\""))
        fixture.adr("0002-b.md", adr("decision-makers" to "[]", title = "ADR-0002: Second decision"))
        fixture.adr("0003-c.md", adr("status" to "\"\"", title = "ADR-0003: Third decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:3: `date` is empty"
        output shouldContain "docs/adr/0002-b.md:4: `decision-makers` is empty"
        output shouldContain "docs/adr/0003-c.md:2: `status` is empty"
    }

    @Test
    fun `a date must be a real YYYY-MM-DD day`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("date" to "\"2026-13-40\""))
        fixture.adr("0002-b.md", adr("date" to "\"yesterday\"", title = "ADR-0002: Second decision"))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:3: `date` must be a valid YYYY-MM-DD date, found `2026-13-40`"
        output shouldContain "docs/adr/0002-b.md:3: `date` must be a valid YYYY-MM-DD date, found `yesterday`"
    }

    @Test
    fun `decision makers may be one string or a list and the optional keys may be present`() {
        val fixture = fixture()
        fixture.adr(
            "0001-a.md",
            adr("decision-makers" to "\"repository author\"", "consulted" to "[]", "informed" to "[\"a\", \"b\"]"),
        )

        fixture.passes()
    }

    @Test
    fun `a blank list item and an unknown key fail`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("decision-makers" to "[\"me\", \"\"]", "owner" to "\"me\""))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:4: `decision-makers` has an empty item"
        output shouldContain "docs/adr/0001-a.md:5: unknown front matter key `owner`"
    }

    @Test
    fun `a missing required section fails naming it`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr(sections = SECTIONS - "Considered Options"))
        fixture.adr(
            "0002-b.md",
            adr(title = "ADR-0002: Second decision", sections = SECTIONS - "Context and Problem Statement"),
        )

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:6: missing the section `## Considered Options`"
        output shouldContain "docs/adr/0002-b.md:6: missing the section `## Context and Problem Statement`"
    }

    @Test
    fun `an empty required section fails and the optional sections are not required`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr(emptySection = "Decision Outcome"))
        fixture.adr("0002-b.md", adr(title = "ADR-0002: Second decision", sections = SECTIONS))

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:"
        output shouldContain "the section `## Decision Outcome` is empty"
        output shouldNotContain "0002-b.md"
    }

    @Test
    fun `a heading inside a code fence is not a section`() {
        val fixture = fixture()
        val fenced = adr(sections = SECTIONS - "Considered Options") + "\n```text\n## Considered Options\n```\n"
        fixture.adr("0001-a.md", fenced)

        fixture.fails() shouldContain "missing the section `## Considered Options`"
    }

    @Test
    fun `a tilde line inside a backtick fence does not close it`() {
        val fixture = fixture()
        val fenced = adr(sections = SECTIONS - "Considered Options") + "\n```text\n~~~\n## Considered Options\n```\n"
        fixture.adr("0001-a.md", fenced)

        fixture.fails() shouldContain "missing the section `## Considered Options`"
    }

    @Test
    fun `a malformed front matter fails with the file and the line`() {
        val fixture = fixture()
        fixture.adr("0001-a.md", adr("consulted" to "[]\n  nested: value"))
        fixture.adr("0002-b.md", "# ADR-0002: No front matter\n\n## Decision Outcome\n\nText.\n")

        val output = fixture.fails()

        output shouldContain "docs/adr/0001-a.md:6: unsupported front matter: indented line"
        output shouldContain "docs/adr/0002-b.md:1: unsupported front matter: the file must start with a --- line"
    }

    @Test
    fun `a file name that is not NNNN-slug fails`() {
        val fixture = fixture()
        fixture.adr("0001-first.md", adr())
        fixture.adr("README.md", adr(title = "ADR-0002: Readme"))
        fixture.adr("0002-Upper.md", adr(title = "ADR-0002: Upper"))

        val output = fixture.fails()

        output shouldContain "docs/adr/README.md: the file name must match NNNN-<slug>.md"
        output shouldContain "docs/adr/0002-Upper.md: the file name must match NNNN-<slug>.md"
    }

    @Test
    fun `the template and files outside docs adr are never checked`() {
        val fixture = fixture()
        fixture.adr("0001-first.md", adr())
        fixture.file("docs/templates/adr.md", "# not an ADR at all\n")
        fixture.file("docs/adr/drafts/9999-draft.md", "no front matter\n")
        fixture.file("docs/other/0007-note.md", "no front matter\n")

        fixture.passes()
    }

    @Test
    fun `problems are reported in file name order`() {
        val fixture = fixture()
        fixture.adr("0002-b.md", adr("status" to "draft", title = "ADR-0002: Second decision"))
        fixture.adr("0001-a.md", adr("status" to "draft"))

        val output = fixture.fails()

        output.shouldContainInOrder("docs/adr/0001-a.md:2:", "docs/adr/0002-b.md:2:")
        fixture.read("build/reports/governance/adrs.txt").lines().first() shouldContain "docs/adr/0001-a.md:2:"
    }

    private companion object {
        const val TASK = "checkAdrs"

        val SECTIONS = listOf("Context and Problem Statement", "Considered Options", "Decision Outcome")

        val DEFAULTS: Map<String, String?> =
            linkedMapOf(
                "status" to "\"accepted\"",
                "date" to "\"2026-11-21\"",
                "decision-makers" to "[\"repository author\"]",
            )

        /**
         * A valid ADR whose front matter values are the raw YAML text of [changes] over the defaults; a null value
         * drops the key. The first line of the body, line 7, is the title heading, and every section of [sections] has
         * one line of text except [emptySection].
         */
        fun adr(
            vararg changes: Pair<String, String?>,
            title: String = "ADR-0001: First decision",
            sections: List<String> = SECTIONS,
            emptySection: String? = null,
        ): String {
            val values = (DEFAULTS + changes.toList()).filterValues { it != null }
            val front = values.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
            val body =
                sections.joinToString(separator = "\n") { name ->
                    if (name == emptySection) "## $name\n" else "## $name\n\nText for $name.\n"
                }
            return "---\n$front\n---\n\n# $title\n\n$body"
        }
    }
}
