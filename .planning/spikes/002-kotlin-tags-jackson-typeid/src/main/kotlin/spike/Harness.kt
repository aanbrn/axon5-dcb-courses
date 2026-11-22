package spike

import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import org.axonframework.common.configuration.AxonConfiguration
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson.JacksonConverter
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import spike.contracts.CourseCreated
import spike.contracts.StudentSubscribed
import spike.contracts.SubscribeResult
import spike.contracts.SubscribeStudent
import spike.json.coursesJsonMapper

class Handlers {
    @CommandHandler
    fun handle(cmd: SubscribeStudent, appender: EventAppender): SubscribeResult =
        if (cmd.studentId.toString().endsWith("0")) {
            SubscribeResult.Rejected("student-unlucky", cmd.courseId)
        } else {
            appender.append(StudentSubscribed(cmd.courseId, cmd.studentId))
            SubscribeResult.Subscribed
        }
}

fun startAxon(servers: String, tagResolver: TagResolver? = null): AxonConfiguration =
    EventSourcingConfigurer.create()
        .registerCommandHandlingModule(
            CommandHandlingModule.named("courses").commandHandlers().autodetectedCommandHandlingComponent { Handlers() }
        )
        .componentRegistry { cr ->
            cr.registerComponent(GeneralConverter::class.java) { DelegatingGeneralConverter(JacksonConverter(coursesJsonMapper())) }
            cr.registerComponent(AxonServerConfiguration::class.java) { AxonServerConfiguration().apply { setServers(servers) } }
            if (tagResolver != null) cr.registerComponent(TagResolver::class.java) { tagResolver }
        }
        .start()

@Suppress("unused")
private val keepImport = CourseCreated::class
