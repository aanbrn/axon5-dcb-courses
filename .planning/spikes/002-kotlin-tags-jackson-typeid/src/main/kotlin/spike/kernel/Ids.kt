package spike.kernel

import com.fasterxml.uuid.Generators
import java.util.UUID
import spike.typeid.TypeId

/** Common shape of every entity id: a UUID with a fixed TypeID prefix; toString() is the canonical TypeID. */
interface TypedId {
    val uuid: UUID
}

abstract class TypedIdCompanion<T : TypedId>(val prefix: String, private val wrap: (UUID) -> T) {
    fun of(uuid: UUID): T = wrap(uuid)
    fun random(): T = wrap(Generators.timeBasedEpochGenerator().generate())

    fun parse(value: String): T {
        val (p, uuid) = TypeId.decode(value)
        require(p == prefix) { "Wrong TypeID prefix in '$value': expected '$prefix', got '$p'" }
        return wrap(uuid)
    }

    fun parseOrNull(value: String): T? = runCatching { parse(value) }.getOrNull()
}

@JvmInline
value class CourseId(override val uuid: UUID) : TypedId {
    override fun toString() = TypeId.encode(PREFIX, uuid)

    companion object : TypedIdCompanion<CourseId>("course", ::CourseId) {
        const val PREFIX = "course"
    }
}

@JvmInline
value class StudentId(override val uuid: UUID) : TypedId {
    override fun toString() = TypeId.encode(PREFIX, uuid)

    companion object : TypedIdCompanion<StudentId>("student", ::StudentId) {
        const val PREFIX = "student"
    }
}

/** Tag registry (D-10): entity tag key == TypeID prefix. */
object Tags {
    const val COURSE = CourseId.PREFIX
    const val STUDENT = StudentId.PREFIX
}
