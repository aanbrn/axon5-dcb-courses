package spike.contracts

import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.commandhandling.annotation.Command
import org.axonframework.messaging.eventhandling.annotation.Event
import spike.kernel.CourseId
import spike.kernel.StudentId
import spike.kernel.Tags

/** Plain annotation on the constructor property, as in the official Kotlin example. */
@Event(namespace = "courses", name = "CourseCreated", version = "1")
data class CourseCreated(@EventTag(key = Tags.COURSE) val courseId: CourseId, val name: String)

/** Explicit use-site targets: field and getter. */
@Event(namespace = "courses", name = "StudentSubscribed", version = "1")
data class StudentSubscribed(
    @field:EventTag(key = Tags.COURSE) val courseId: CourseId,
    @get:EventTag(key = Tags.STUDENT) val studentId: StudentId,
)

@Command(namespace = "courses", name = "SubscribeStudent", version = "1")
data class SubscribeStudent(val courseId: CourseId, val studentId: StudentId)

/** Contracts stay free of Jackson annotations: polymorphism is added by a mix-in in the shared JSON module. */
sealed interface SubscribeResult {
    data object Subscribed : SubscribeResult

    data class Rejected(val code: String, val courseId: CourseId) : SubscribeResult
}
