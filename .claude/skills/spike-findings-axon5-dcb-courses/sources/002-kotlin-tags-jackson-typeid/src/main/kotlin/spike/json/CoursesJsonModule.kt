package spike.json

import com.fasterxml.jackson.annotation.JsonTypeInfo
import spike.contracts.SubscribeResult
import spike.kernel.CourseId
import spike.kernel.StudentId
import spike.kernel.TypedId
import spike.kernel.TypedIdCompanion
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule

/** One module for value-class ids and sealed results, shared by the Axon converter and (later) the BFF REST layer. */
class CoursesJsonModule : SimpleModule("courses") {
    init {
        id(CourseId::class.java, CourseId)
        id(StudentId::class.java, StudentId)
        setMixInAnnotation(SubscribeResult::class.java, SealedTypeInfo::class.java)
    }

    private fun <T : TypedId> id(type: Class<T>, companion: TypedIdCompanion<T>) {
        addSerializer(type, object : ValueSerializer<T>() {
            override fun serialize(value: T, gen: JsonGenerator, ctxt: SerializationContext) {
                gen.writeString(value.toString())
            }
        })
        addDeserializer(type, object : ValueDeserializer<T>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = companion.parse(p.valueAsString)
        })
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "@type")
    private interface SealedTypeInfo
}

fun coursesJsonMapper(): JsonMapper =
    JsonMapper.builder().addModule(KotlinModule.Builder().build()).addModule(CoursesJsonModule()).build()
