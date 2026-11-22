package spike

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.messaging.commandhandling.annotation.Command
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.annotation.Event
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.axonframework.modelling.annotation.InjectEntity
import org.axonframework.modelling.annotation.TargetEntityId

object Tags {
    const val COURSE = "course"
    const val STUDENT = "student"
}

@Event(namespace = "spike", name = "CourseCreated", version = "1")
data class CourseCreated(@field:EventTag(key = Tags.COURSE) val courseId: String, val capacity: Int)

@Event(namespace = "spike", name = "StudentSubscribed", version = "1")
data class StudentSubscribed(
    @field:EventTag(key = Tags.COURSE) val courseId: String,
    @field:EventTag(key = Tags.STUDENT) val studentId: String,
)

@Command(namespace = "spike", name = "CreateCourse", version = "1")
data class CreateCourse(@get:TargetEntityId val courseId: String, val capacity: Int)

@Command(namespace = "spike", name = "Subscribe", version = "1")
data class Subscribe(@get:TargetEntityId val courseId: String, val studentId: String)

/** Sealed result: business rejection is a value, not an exception (D-05). */
@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME)
sealed interface SubscribeResult {
    data object Subscribed : SubscribeResult
    data class Rejected(val code: String) : SubscribeResult
}

@EventSourcedEntity
class CourseState @EntityCreator constructor() {
    var exists = false
    var capacity = 0
    var subscribed = 0

    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        fun criteria(courseId: String): EventCriteria = EventCriteria.havingTags(Tag.of(Tags.COURSE, courseId))
    }

    @EventSourcingHandler fun on(e: CourseCreated) { exists = true; capacity = e.capacity }
    @EventSourcingHandler fun on(@Suppress("UNUSED_PARAMETER") e: StudentSubscribed) { subscribed++ }
}

class CourseCommandHandlers {
    @CommandHandler
    fun handle(cmd: CreateCourse, appender: EventAppender) {
        appender.append(CourseCreated(cmd.courseId, cmd.capacity))
    }

    @CommandHandler
    fun handle(cmd: Subscribe, @InjectEntity state: CourseState, appender: EventAppender): SubscribeResult =
        when {
            !state.exists -> SubscribeResult.Rejected("course-not-found")
            state.subscribed >= state.capacity -> SubscribeResult.Rejected("course-full")
            else -> SubscribeResult.Subscribed.also { appender.append(StudentSubscribed(cmd.courseId, cmd.studentId)) }
        }
}
