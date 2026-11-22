package spike

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventstreaming.Tag

/**
 * Reads @EventTag through kotlin-reflect, so value-class properties are boxed and tagged with their toString()
 * (the TypeID), not with the unboxed JVM field value.
 */
class KotlinTagResolver : TagResolver {
    override fun resolve(event: EventMessage): Set<Tag> {
        val payload = event.payload() ?: return emptySet()
        return payload::class.memberProperties.flatMap { prop ->
            val annotations = buildList {
                addAll(prop.annotations.filterIsInstance<EventTag>())
                prop.javaField?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
                prop.javaGetter?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
            }
            if (annotations.isEmpty()) return@flatMap emptyList()
            val value = prop.getter.call(payload) ?: return@flatMap emptyList()
            annotations.distinct().map { Tag.of(it.key.ifBlank { prop.name }, value.toString()) }
        }.toSet()
    }
}
