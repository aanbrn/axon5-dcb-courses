// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.sample

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GreetingTest {

    @Test
    fun `greets the given name with the configured salutation`() {
        Greeting("Hello").greet("Axon") shouldBe "Hello, Axon!"
    }
}
