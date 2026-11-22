package spike

import io.axoniq.framework.testcontainer.AxonServerContainer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.axonframework.conversion.GeneralConverter
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.eventhandling.GenericEventMessage
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import spike.contracts.StudentSubscribed
import spike.contracts.SubscribeResult
import spike.contracts.SubscribeStudent
import spike.json.CoursesJsonModule
import spike.kernel.CourseId
import spike.kernel.StudentId

@SpringBootApplication(scanBasePackages = [])
open class WiringApp {
    @Bean open fun coursesJsonModule() = CoursesJsonModule()
    @Bean open fun kotlinTagResolver(): TagResolver = KotlinTagResolver()
    @Bean open fun handlers() = Handlers()
}

class SpringWiringTest {
    @Test
    fun `spring boot picks up the shared json module and the kotlin tag resolver`() {
        AxonServerContainer("docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot").withDevMode(true).withDcbContext(true).use { server ->
            server.start()
            SpringApplicationBuilder(WiringApp::class.java)
                .properties(
                    "axon.axonserver.servers=${server.axonServerAddress}",
                    "axon.converter.general=jackson",
                    "axoniq.update-checker.disabled=true",
                )
                .run().use { ctx ->
                    val course = CourseId.random()
                    val student = StudentId.random()
                    val converter = ctx.getBean(GeneralConverter::class.java)
                    val bytes = converter.convert(StudentSubscribed(course, student), ByteArray::class.java)!!
                    println("SPRING converter JSON: ${String(bytes)} converter=${converter.javaClass.simpleName}")
                    String(bytes) shouldContain course.toString()
                    val resolver = ctx.getBean(TagResolver::class.java)
                    val tags = resolver.resolve(GenericEventMessage(MessageType(StudentSubscribed::class.java), StudentSubscribed(course, student)))
                    println("SPRING TagResolver=${resolver.javaClass.simpleName} tags=$tags")
                    tags.map { it.value() }.toSet() shouldBe setOf(course.toString(), student.toString())
                    ctx.getBean(CommandGateway::class.java).send(SubscribeStudent(course, student)).resultAs(SubscribeResult::class.java).get() shouldBe
                        SubscribeResult.Subscribed
                }
        }
    }
}
