package spike

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.messaging.commandhandling.annotation.Command
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.axonframework.modelling.annotation.InjectEntity
import org.axonframework.modelling.annotation.TargetEntityId
import spike.contracts.CourseCreated
import spike.contracts.StudentSubscribed
import spike.contracts.SubscribeResult
import spike.kernel.CourseId
import spike.kernel.StudentId
import spike.kernel.Tags

@Command(namespace = "courses", name = "CreateCourse", version = "1")
data class CreateCourse(@TargetEntityId val courseId: CourseId, val name: String)

@Command(namespace = "courses", name = "SubscribeToCourse", version = "1")
data class SubscribeToCourse(@TargetEntityId val courseId: CourseId, val studentId: StudentId)

@EventSourcedEntity
class CourseSeats @EntityCreator constructor() {
    var exists = false
    var taken = 0

    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        fun criteria(id: CourseId): EventCriteria = EventCriteria.havingTags(Tag.of(Tags.COURSE, id.toString()))
    }

    @EventSourcingHandler fun on(@Suppress("UNUSED_PARAMETER") e: CourseCreated) { exists = true }
    @EventSourcingHandler fun on(@Suppress("UNUSED_PARAMETER") e: StudentSubscribed) { taken++ }
}

class SeatHandlers {
    @CommandHandler
    fun handle(cmd: CreateCourse, @InjectEntity seats: CourseSeats?, appender: EventAppender): SubscribeResult =
        if (seats?.exists == true) SubscribeResult.Rejected("course-exists", cmd.courseId)
        else SubscribeResult.Subscribed.also { appender.append(CourseCreated(cmd.courseId, cmd.name)) }

    @CommandHandler
    fun handle(cmd: SubscribeToCourse, @InjectEntity seats: CourseSeats?, appender: EventAppender): SubscribeResult =
        when {
            seats == null || !seats.exists -> SubscribeResult.Rejected("course-not-found", cmd.courseId)
            seats.taken >= 2 -> SubscribeResult.Rejected("course-full", cmd.courseId)
            else -> SubscribeResult.Subscribed.also { appender.append(StudentSubscribed(cmd.courseId, cmd.studentId)) }
        }
}
