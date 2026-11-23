// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventstreaming.Tag

/**
 * Reads `@EventTag` through kotlin-reflect, so the tag of a value-class property is the property value's `toString()`
 * (the canonical TypeID) and not the unboxed JVM value.
 *
 * Axon's own `AnnotationBasedTagResolver` reads the annotated field or getter through Java reflection. For an entity ID
 * that is a Kotlin value class over a [java.util.UUID], that returns the raw UUID, so the event is tagged
 * `course=0190...` while every criterion built from a `CourseId` asks for `course=course_01h...`. The two never match,
 * and the criteria silently select nothing: no error, only decisions that no longer see their own events. This resolver
 * boxes the property first, so the tag text and the criteria text come from the same `toString()`.
 *
 * The `@EventTag` annotation is found on the Kotlin property, on its backing field and on its getter, because Kotlin
 * places an annotation on a constructor property according to the annotation's targets.
 *
 * An empty `key` falls back to the property name, as Axon's resolver does.
 */
// UPSTREAM: UP-0004 Axon's tag resolver does not box Kotlin value classes; drop this class when it does.
class KotlinTagResolver : TagResolver {

    /**
     * Resolves the tags of [event] from its payload.
     *
     * @param event the event to tag
     * @return the tags of every `@EventTag`-annotated property of the payload with a value, empty when there is no
     *   payload
     */
    override fun resolve(event: EventMessage): Set<Tag> {
        val payload = event.payload() ?: return emptySet()
        return payload::class
            .memberProperties
            .flatMap { property ->
                val annotations = eventTags(property)
                val value = if (annotations.isEmpty()) null else property.getter.call(payload)
                if (value == null) {
                    emptyList()
                } else {
                    annotations.distinct().map { Tag.of(it.key.ifBlank { property.name }, value.toString()) }
                }
            }
            .toSet()
    }

    private fun eventTags(property: KProperty1<out Any, *>): List<EventTag> = buildList {
        addAll(property.annotations.filterIsInstance<EventTag>())
        property.javaField?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
        property.javaGetter?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
    }
}
