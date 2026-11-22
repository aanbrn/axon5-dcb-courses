package spike

import io.kotest.matchers.shouldBe
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson.JacksonConverter
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import io.axoniq.framework.axonserver.connector.configuration.AxonServerConfigurationEnhancer
import org.junit.jupiter.api.Test
import spike.contracts.SubscribeResult
import spike.json.coursesJsonMapper
import spike.kernel.CourseId
import spike.kernel.StudentId

class ValueClassEntityPathTest {
    /** Documents the failure: @TargetEntityId on a value-class property yields the unboxed UUID. */
    @Test
    fun `entity registered with the value-class type as id type finds no repository`() {
        val failure = org.junit.jupiter.api.assertThrows<Throwable> { run(CourseId::class.java) }
        println("EXPECTED: ${failure.message}")
    }

    /** The JVM erases a value class to its underlying type: @TargetEntityId getters and the mangled criteria builder both use UUID. */
    @Test
    fun `entity registered with the underlying UUID as id type`() = run(java.util.UUID::class.java)

    private fun run(idType: Class<*>) {
        println("withAxonKotlin=${System.getProperty("withAxonKotlin")} idType=${idType.simpleName}")
        val axon =
            EventSourcingConfigurer.create()
                .registerEntity(EventSourcedEntityModule.autodetected(idType, CourseSeats::class.java))
                .registerCommandHandlingModule(
                    CommandHandlingModule.named("seats").commandHandlers().autodetectedCommandHandlingComponent { SeatHandlers() }
                )
                .componentRegistry { cr ->
                    cr.disableEnhancer(AxonServerConfigurationEnhancer::class.java)
                    cr.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
                    cr.registerComponent(GeneralConverter::class.java) { DelegatingGeneralConverter(JacksonConverter(coursesJsonMapper())) }
                    cr.registerComponent(TagResolver::class.java) { KotlinTagResolver() }
                }
                .start()
        try {
            val gw = axon.getComponent(CommandGateway::class.java)
            fun send(c: Any) = runCatching { gw.send(c).resultAs(SubscribeResult::class.java).get() }
                .onFailure { println("FAILURE ${c.javaClass.simpleName}: ${generateSequence(it) { e -> e.cause }.last().let { e -> "${e.javaClass.simpleName}: ${e.message}" }}") }
                .getOrNull()
            val course = CourseId.random()
            val r1 = send(CreateCourse(course, "DDD"))
            val r2 = send(CreateCourse(course, "DDD"))
            val subs = (1..3).map { send(SubscribeToCourse(course, StudentId.random())) }
            println("RESULTS create=$r1 createAgain=$r2 subs=$subs")
            r1 shouldBe SubscribeResult.Subscribed
            r2 shouldBe SubscribeResult.Rejected("course-exists", course)
            subs shouldBe listOf(SubscribeResult.Subscribed, SubscribeResult.Subscribed, SubscribeResult.Rejected("course-full", course))
        } finally {
            axon.shutdown()
        }
    }
}
