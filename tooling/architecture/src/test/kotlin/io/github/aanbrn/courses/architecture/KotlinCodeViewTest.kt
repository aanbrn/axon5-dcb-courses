// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/**
 * Every state of the scanner, with the text `KEEP` where the view must keep code and `GONE` where it must not.
 *
 * Inputs that contain a dollar brace are multi-dollar strings, so that the test source itself holds no template.
 */
class KotlinCodeViewTest {
    @Test
    fun `a line comment is masked to the end of its line and the next line is kept`() {
        val view = KotlinCodeView.of("val a = 1 // GONE\nval b = KEEP")

        view shouldBe "val a = 1 " + blank(7) + "\nval b = KEEP"
    }

    @Test
    fun `slashes inside a string do not start a comment`() {
        val view = KotlinCodeView.of("val url = \"http://host\"; KEEP")

        view shouldBe "val url = \"" + blank(11) + "\"; KEEP"
    }

    @Test
    fun `a block comment opener inside a string does not hide the code after it`() {
        val view = KotlinCodeView.of("val glob = \"**/*.kt\"\nKEEP\n/* GONE */\nval end = 1")

        view shouldContain "KEEP"
        view shouldContain "val end = 1"
        view shouldNotContain "GONE"
        view shouldNotContain ".kt"
    }

    @Test
    fun `block comments nest`() {
        val view = KotlinCodeView.of("/* a /* b */ GONE */ KEEP")

        view shouldBe blank(20) + " KEEP"
    }

    @Test
    fun `an unterminated block comment is masked to the end of the text`() {
        val view = KotlinCodeView.of("KEEP /* GONE\nGONE")

        view shouldBe "KEEP " + blank(7) + "\n" + blank(4)
    }

    @Test
    fun `an escaped quote does not end a string`() {
        val view = KotlinCodeView.of("val s = \"a\\\"//b\"; KEEP")

        view shouldBe "val s = \"" + blank(6) + "\"; KEEP"
    }

    @Test
    fun `a raw string with slashes and apostrophes is one literal`() {
        val view = KotlinCodeView.of("val r = \"\"\"it's // GONE\n'x\"\"\"; KEEP")

        view shouldBe "val r = \"\"\"" + blank(12) + "\n" + blank(2) + "\"\"\"; KEEP"
    }

    @Test
    fun `a raw string ends at the last three quotes of a run of quotes`() {
        val view = KotlinCodeView.of("val r = \"\"\"a\"\"\"\"; KEEP")

        view shouldBe "val r = \"\"\"" + blank(2) + "\"\"\"; KEEP"
    }

    @Test
    fun `a character literal holding a double quote does not open a string`() {
        val view = KotlinCodeView.of("val c = '\"'; KEEP // GONE")

        view shouldBe "val c = '" + blank(1) + "'; KEEP " + blank(7)
    }

    @Test
    fun `an escaped apostrophe does not end a character literal`() {
        val view = KotlinCodeView.of("val c = '\\''; KEEP")

        view shouldBe "val c = '" + blank(2) + "'; KEEP"
    }

    @Test
    fun `a backtick identifier with an apostrophe does not open a character literal`() {
        val view = KotlinCodeView.of("fun `it's fine`() { KEEP } // GONE")

        view shouldBe "fun `it's fine`() { KEEP } " + blank(7)
    }

    @Test
    fun `the code of a template stays visible and the template closes the string`() {
        val view = KotlinCodeView.of("val s = \"a" + $$"${KEEP}" + "b\"; /* GONE */ KEEP")

        view shouldBe "val s = \"" + blank(1) + $$"${KEEP}" + blank(1) + "\"; " + blank(10) + " KEEP"
    }

    @Test
    fun `a string inside a template is masked and its comment opener is not a comment`() {
        val view = KotlinCodeView.of("val s = \"" + $$"${" + "\"/*\"" + "}\"; KEEP /* GONE */")

        view shouldBe "val s = \"" + $$"${" + "\"" + blank(2) + "\"}\"; KEEP " + blank(10)
    }

    @Test
    fun `braces inside a template do not end it early`() {
        val view = KotlinCodeView.of("val s = \"" + $$"${mapOf(1 to { 2 })}" + "\"; KEEP")

        view shouldContain "mapOf(1 to { 2 })"
        view shouldContain "KEEP"
    }

    @Test
    fun `a dollar brace in a multi dollar string is content`() {
        val view = KotlinCodeView.of("val s = $$\"" + $$"${GONE}" + "\"; KEEP")

        view shouldBe "val s = $$\"" + blank(7) + "\"; KEEP"
    }

    @Test
    fun `a double dollar brace in a multi dollar string is a template`() {
        val view = KotlinCodeView.of("val s = $$\"x" + $$$"$${KEEP}" + "\"; // GONE")

        view shouldBe "val s = $$\"" + blank(1) + $$$"$${KEEP}" + "\"; " + blank(7)
    }

    @Test
    fun `the contents of literals and comments are absent from the view`() {
        val view =
            KotlinCodeView.of("val a = \"secret\"\nval b = 'x'\n// secret\n/* secret */\nval c = \"\"\"secret\"\"\"")

        view shouldNotContain "secret"
    }

    @Test
    fun `the view has the length and the line breaks of the source`() {
        val source = "val a = \"one\ntwo\"\r\n/* three\nfour */ // five\r\nval b = 1\n"

        val view = KotlinCodeView.of(source)

        view.length shouldBe source.length
        view.indices.filter { view[it] == '\n' || view[it] == '\r' } shouldBe
            source.indices.filter { source[it] == '\n' || source[it] == '\r' }
    }

    private fun blank(count: Int): String = " ".repeat(count)
}
