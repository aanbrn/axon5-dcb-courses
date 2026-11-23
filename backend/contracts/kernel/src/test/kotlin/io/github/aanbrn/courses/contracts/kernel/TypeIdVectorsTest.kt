// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.yaml.YAMLMapper

/** Checks the codec against the official TypeID v0.3.0 vectors, pinned in `src/test/resources/typeid`. */
class TypeIdVectorsTest {

    private val yaml = YAMLMapper()

    private fun vectors(resource: String): List<Map<String, String>> {
        val stream =
            checkNotNull(javaClass.getResourceAsStream("/typeid/$resource")) { "Missing test resource $resource" }
        return stream
            .use { yaml.readValue(it, List::class.java) }
            .map { entry ->
                (entry as Map<*, *>).entries.associate { (key, value) -> key.toString() to value.toString() }
            }
    }

    @Test
    fun `every official valid vector decodes to its prefix and UUID and re-encodes to the same string`() {
        val valid = vectors("valid.yml")

        valid shouldHaveSize 9
        valid.forEach { vector ->
            val decoded = TypeId.decode(vector.getValue("typeid"))

            decoded.prefix shouldBe vector.getValue("prefix")
            decoded.uuid shouldBe UUID.fromString(vector.getValue("uuid"))
            TypeId.encode(decoded.prefix, decoded.uuid) shouldBe vector.getValue("typeid")
        }
    }

    @Test
    fun `every official invalid vector is rejected as malformed`() {
        val invalid = vectors("invalid.yml")

        invalid shouldHaveSize 21
        invalid.forEach { vector ->
            val failure = shouldThrow<IllegalArgumentException> { TypeId.decode(vector.getValue("typeid")) }

            failure.message.orEmpty() shouldStartWith "Malformed TypeID '"
        }
    }
}
