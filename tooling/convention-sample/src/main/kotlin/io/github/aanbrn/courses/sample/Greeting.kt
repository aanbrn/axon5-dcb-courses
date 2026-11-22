// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.sample

/**
 * Builds greetings for a fixed salutation.
 *
 * The class exists to exercise the build conventions end to end: it is compiled, formatted,
 * analysed and tested exactly like production code.
 *
 * @param salutation the word that opens every greeting
 */
class Greeting(private val salutation: String) {

    /**
     * Greets [name] with the configured salutation.
     *
     * @param name the person to greet
     * @return the greeting text
     */
    fun greet(name: String): String = "$salutation, $name!"
}
