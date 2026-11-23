// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.serialization

import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.TypedId
import io.github.aanbrn.courses.contracts.kernel.TypedIdCompanion
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule

/**
 * The one Jackson 3 module for the value types of the contracts, shared by the Axon converter and the BFF REST layer.
 *
 * Entity IDs are written as their canonical TypeID string and read back through [TypedIdCompanion.parse], so a
 * malformed or wrong-prefix ID fails deserialisation. A [RejectionCode] is written as its plain text. The module knows
 * the kernel only: polymorphic results carry their own `@JsonTypeInfo` on the contract type.
 */
class CoursesJsonModule : SimpleModule("courses") {
    init {
        typedId(CourseId::class.java, CourseId)
        typedId(StudentId::class.java, StudentId)
        addSerializer(RejectionCode::class.java, RejectionCodeSerializer)
        addDeserializer(RejectionCode::class.java, RejectionCodeDeserializer)
    }

    private fun <T : TypedId> typedId(type: Class<T>, companion: TypedIdCompanion<T>) {
        addSerializer(type, TypedIdSerializer())
        addDeserializer(type, TypedIdDeserializer(companion))
    }
}

/** Writes any entity ID as its canonical TypeID string. */
private class TypedIdSerializer<T : TypedId> : ValueSerializer<T>() {
    override fun serialize(value: T, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.toString())
    }
}

/** Reads an entity ID of one type from its canonical TypeID string. */
private class TypedIdDeserializer<T : TypedId>(private val companion: TypedIdCompanion<T>) : ValueDeserializer<T>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = companion.parse(p.valueAsString)
}

/** Writes a rejection code as its plain text. */
private object RejectionCodeSerializer : ValueSerializer<RejectionCode>() {
    override fun serialize(value: RejectionCode, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.value)
    }
}

/** Reads a rejection code from its plain text, rejecting anything that is not lower-case kebab-case. */
private object RejectionCodeDeserializer : ValueDeserializer<RejectionCode>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): RejectionCode =
        RejectionCode(p.valueAsString)
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
