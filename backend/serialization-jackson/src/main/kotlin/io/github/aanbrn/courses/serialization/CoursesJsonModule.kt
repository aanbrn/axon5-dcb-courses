// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.serialization

import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.TypedId
import io.github.aanbrn.courses.contracts.kernel.TypedIdCompanion
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import tools.jackson.module.kotlin.KotlinModule

/**
 * The one Jackson 3 module for the value types of the contracts, shared by the Axon converter and the BFF REST layer.
 *
 * Entity IDs and a [RejectionCode] are written through `toString()`, which is their wire form: the canonical TypeID for
 * an ID and the plain text for a code. They are read back through [TypedIdCompanion.parse] and the [RejectionCode]
 * constructor, so a malformed or wrong-prefix ID, or a code that is not lower-case kebab-case, fails deserialisation. A
 * failure is reported through the [DeserializationContext] as an `InvalidFormatException` that names the value and the
 * target type and keeps the original `IllegalArgumentException` as its cause. The module knows the kernel only:
 * polymorphic results carry their own `@JsonTypeInfo` on the contract type.
 */
class CoursesJsonModule : SimpleModule("courses") {
    init {
        typedId(CourseId::class.java, CourseId)
        typedId(StudentId::class.java, StudentId)
        addSerializer(RejectionCode::class.java, ToStringSerializer.instance)
        addDeserializer(
            RejectionCode::class.java,
            FromStringDeserializer(RejectionCode::class.java) { RejectionCode(it) },
        )
    }

    private fun <T : TypedId> typedId(type: Class<T>, companion: TypedIdCompanion<T>) {
        addSerializer(type, ToStringSerializer.instance)
        addDeserializer(type, FromStringDeserializer(type, companion::parse))
    }
}

/**
 * Reads a value of [target] from its JSON string with [parse], and reports text that [parse] rejects through the
 * deserialization context, which is Jackson's own idiom for types that are read from a string.
 */
private class FromStringDeserializer<T : Any>(private val target: Class<T>, private val parse: (String) -> T) :
    ValueDeserializer<T>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val text = p.valueAsString.orEmpty()
        try {
            return parse(text)
        } catch (failure: IllegalArgumentException) {
            throw ctxt.weirdStringException(text, target, failure.message.orEmpty()).withCause(failure)
        }
    }
}

/**
 * Builds the mapper for the contracts: the Kotlin module for constructors and `data object`s, and [CoursesJsonModule].
 *
 * Default typing stays off, so only types that declare `@JsonTypeInfo` themselves are polymorphic.
 *
 * @return a new, fully configured [JsonMapper]
 */
fun coursesJsonMapper(): JsonMapper =
    JsonMapper.builder().addModule(KotlinModule.Builder().build()).addModule(CoursesJsonModule()).build()
