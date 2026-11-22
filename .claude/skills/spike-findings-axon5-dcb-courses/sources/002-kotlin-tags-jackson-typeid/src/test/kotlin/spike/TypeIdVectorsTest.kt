package spike

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import kotlin.random.Random
import org.junit.jupiter.api.Test
import spike.kernel.CourseId
import spike.typeid.TypeId
import tools.jackson.dataformat.yaml.YAMLMapper

class TypeIdVectorsTest {
    private val yaml = YAMLMapper()

    @Suppress("UNCHECKED_CAST")
    private fun load(name: String) = yaml.readValue(javaClass.getResourceAsStream("/$name"), List::class.java) as List<Map<String, String>>

    @Test
    fun `all official valid vectors decode and re-encode`() {
        val cases = load("valid.yml")
        cases.forEach { c ->
            val (prefix, uuid) = TypeId.decode(c.getValue("typeid"))
            prefix shouldBe c.getValue("prefix")
            uuid shouldBe UUID.fromString(c.getValue("uuid"))
            TypeId.encode(prefix, uuid) shouldBe c.getValue("typeid")
        }
        println("valid vectors: ${cases.size}")
    }

    @Test
    fun `all official invalid vectors are rejected`() {
        val cases = load("invalid.yml")
        cases.forEach { c -> shouldThrow<IllegalArgumentException> { TypeId.decode(c.getValue("typeid")) } }
        println("invalid vectors: ${cases.size}")
    }

    @Test
    fun `random round trip and prefix checks`() {
        val rnd = Random(42)
        repeat(100_000) {
            val uuid = UUID(rnd.nextLong(), rnd.nextLong())
            TypeId.decode(TypeId.encode("course", uuid)) shouldBe ("course" to uuid)
        }
        val id = CourseId.random()
        CourseId.parse(id.toString()) shouldBe id
        id.toString().length shouldBe "course_".length + 26
        (id.uuid.version()) shouldBe 7
        shouldThrow<IllegalArgumentException> { CourseId.parse("student_01h455vb4pex5vsknk084sn02q") }.message!! shouldContain "expected 'course'"
        CourseId.parseOrNull("garbage") shouldBe null
    }
}
