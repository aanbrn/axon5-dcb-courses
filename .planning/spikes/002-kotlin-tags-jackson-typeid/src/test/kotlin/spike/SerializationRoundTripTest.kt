package spike

import io.axoniq.framework.testcontainer.AxonServerContainer
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import org.axonframework.eventsourcing.eventstore.AnnotationBasedTagResolver
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventhandling.GenericEventMessage
import org.axonframework.messaging.eventhandling.gateway.EventGateway
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.axonframework.conversion.GeneralConverter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import spike.contracts.CourseCreated
import spike.contracts.StudentSubscribed
import spike.contracts.SubscribeResult
import spike.contracts.SubscribeStudent
import spike.json.coursesJsonMapper
import spike.kernel.CourseId
import spike.kernel.StudentId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializationRoundTripTest {
    private val server = AxonServerContainer("docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot").withDevMode(true).withDcbContext(true)
    private val mapper = coursesJsonMapper()

    @BeforeAll fun start() = server.start()
    @AfterAll fun stop() = server.stop()

    private fun message(payload: Any) = GenericEventMessage(MessageType(payload.javaClass), payload)

    @Test
    fun `jackson round trip of events, commands and sealed results`() {
        val course = CourseId.random()
        val student = StudentId.random()
        val json = mapper.writeValueAsString(StudentSubscribed(course, student))
        println("JSON event: $json")
        json shouldBe """{"courseId":"$course","studentId":"$student"}"""
        mapper.readValue(json, StudentSubscribed::class.java) shouldBe StudentSubscribed(course, student)
        mapper.readValue(mapper.writeValueAsString(SubscribeStudent(course, student)), SubscribeStudent::class.java) shouldBe SubscribeStudent(course, student)
        for (r in listOf(SubscribeResult.Subscribed, SubscribeResult.Rejected("course-full", course))) {
            val rj = mapper.writeValueAsString(r)
            println("JSON result: $rj")
            val back = mapper.readValue(rj, SubscribeResult::class.java)
            back shouldBe r
            if (r === SubscribeResult.Subscribed) (back === SubscribeResult.Subscribed) shouldBe true
        }
    }

    @Test
    fun `default annotation tag resolver on value-class properties`() {
        val course = CourseId.random()
        val student = StudentId.random()
        val created = AnnotationBasedTagResolver().resolve(message(CourseCreated(course, "DDD")))
        val subscribed = AnnotationBasedTagResolver().resolve(message(StudentSubscribed(course, student)))
        println("DEFAULT TAGS CourseCreated(plain @EventTag)=$created")
        println("DEFAULT TAGS StudentSubscribed(@field/@get)=$subscribed")
        println("KOTLIN TAGS CourseCreated=${KotlinTagResolver().resolve(message(CourseCreated(course, "DDD")))}")
        println("KOTLIN TAGS StudentSubscribed=${KotlinTagResolver().resolve(message(StudentSubscribed(course, student)))}")
        KotlinTagResolver().resolve(message(StudentSubscribed(course, student))) shouldContainExactlyInAnyOrder
            setOf(Tag.of("course", course.toString()), Tag.of("student", student.toString()))
        KotlinTagResolver().resolve(message(CourseCreated(course, "DDD"))) shouldBe setOf(Tag.of("course", course.toString()))
    }

    @Test
    fun `axon server round trip with kotlin tag resolver, qualified names and sealed command results`() {
        val axon = startAxon(server.axonServerAddress, KotlinTagResolver())
        try {
            val course = CourseId.random()
            val student = StudentId.random()
            axon.getComponent(EventGateway::class.java).publish(null, CourseCreated(course, "DDD")).get()
            val gw = axon.getComponent(CommandGateway::class.java)
            gw.send(SubscribeStudent(course, student)).resultAs(SubscribeResult::class.java).get() shouldBe SubscribeResult.Subscribed
            val unlucky = generateSequence { StudentId.random() }.first { it.toString().endsWith("0") }
            gw.send(SubscribeStudent(course, unlucky)).resultAs(SubscribeResult::class.java).get() shouldBe
                SubscribeResult.Rejected("student-unlucky", course)

            val engine = axon.getComponent(EventStorageEngine::class.java)
            fun read(tag: Tag): List<EventMessage> =
                engine.source(SourcingCondition.conditionFor(EventCriteria.havingTags(tag)))
                    .reduce(mutableListOf<EventMessage>()) { acc, e -> acc!!.also { if (e.message() !is TerminalEventMessage) it += e.message() } }
                    .get()!!
            val byTypeId = read(Tag.of("course", course.toString()))
            val byRawUuid = read(Tag.of("course", course.uuid.toString()))
            println("SOURCED by TypeID tag: ${byTypeId.map { "${it.type()} payloadType=${it.payloadType().simpleName}" }}; by raw UUID tag: ${byRawUuid.size}")
            byTypeId.map { it.type().toString() } shouldBe listOf("courses.CourseCreated#1", "courses.StudentSubscribed#1")
            byRawUuid.size shouldBe 0

            val converter = axon.getComponent(GeneralConverter::class.java)
            val raw = String(byTypeId[1].payload() as ByteArray)
            println("STORED payload: $raw")
            raw shouldContain course.toString()
            converter.convert(byTypeId[0].payload(), CourseCreated::class.java) shouldBe CourseCreated(course, "DDD")
            converter.convert(byTypeId[1].payload(), StudentSubscribed::class.java) shouldBe StudentSubscribed(course, student)
            read(Tag.of("student", student.toString())).size shouldBe 1
        } finally {
            axon.shutdown()
        }
    }

    @Suppress("unused") private val keep = UUID.randomUUID()
}
