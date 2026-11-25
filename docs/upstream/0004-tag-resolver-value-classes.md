---
id: "UP-0004"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: true
upstream: ["https://github.com/AxonIQ/AxonFramework/issues/5328"]
reported: "2026-11-23"
updated: "2026-11-24"
---

# UP-0004: AnnotationBasedTagResolver tags the raw UUID of a Kotlin value-class property

## Symptom

Entity IDs of this repository are Kotlin value classes over a `UUID` whose `toString()` is the canonical TypeID, for
example `course_01h...`. Events carry them in `@EventTag` properties, and every criterion is built from the same IDs
with `Tag.of(key, id.toString())`. Axon's default `AnnotationBasedTagResolver` tags the event with the raw `UUID`
instead, so the stored tag is `course=0190...` while the criterion asks for `course=course_01h...`.

The failure is silent. Nothing throws: the criteria simply select nothing, so every decision runs without seeing its
own earlier events and the DCB invariants are not enforced. Against Axon Server 2026.1.8, sourcing by the TypeID tag
returned both events of a course and sourcing by the raw `UUID` tag returned none, with the events tagged by the
custom resolver. Observed with Axon Framework 5.4.2, with and without the `axon-kotlin` extension, which has no tag
resolver.

## Minimal reproduction

```kotlin
@JvmInline
value class CourseId(val uuid: UUID) {
    override fun toString() = "course_$uuid"
}

data class StudentSubscribed(@field:EventTag(key = "course") val courseId: CourseId)

val event = StudentSubscribed(CourseId(UUID.randomUUID()))
val tags = AnnotationBasedTagResolver().resolve(GenericEventMessage(MessageType(StudentSubscribed::class.java), event))

println(tags) // one tag with key course and the raw uuid as its value, not course_<the uuid>
```

The other two placements of the annotation, `@get:EventTag` and `@EventTag` on the constructor property (which lands
on the field), give the same result. The permanent unit test `KotlinTagResolverTest` in the command service pins the
behavior with the test `Axon's own resolver tags the raw UUID, which is why the custom resolver exists`.

## Root cause

`AnnotationBasedTagResolver` reads the annotated JVM field or method reflectively and builds the tag from
`value.toString()` ([`tagsFrom`, lines 91-109][resolver]). A Kotlin value-class property compiles to a `UUID` field and to a
getter with a mangled name that returns the `UUID`, so reflection never sees the value class, and `toString()` is the
one of the underlying `UUID`. A resolver based on `kotlin-reflect` boxes the property first and gets the TypeID.

The same unboxing defeats `@TargetEntityId`, see [UP-0005](0005-target-entity-id-value-classes.md). The discussion
in [AxonIQ/AxonFramework#3761][entity-id-issue] points to a Kotlin-aware property access as the direction. The
maintainer confirmed that this defect is separate from #3761, which concerns `AnnotationBasedEntityIdResolver` (the
`@TargetEntityId` side recorded in UP-0005), while this one concerns `AnnotationBasedTagResolver`.

[resolver]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/AnnotationBasedTagResolver.java#L91-L109
[entity-id-issue]: https://github.com/AxonIQ/AxonFramework/issues/3761

## Workaround

`KotlinTagResolver` reads `@EventTag` through `kotlin-reflect`, from the Kotlin property, its backing field and its
getter, boxes the value and uses its `toString()`. `CommandServiceConfigurationEnhancer` registers it as the
`TagResolver` component.

- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt`: the resolver, marked
  on the class.
- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt`: the
  registration of the resolver.

## Removal criteria

An Axon Framework or `axon-kotlin` release whose default tag resolution yields `value.toString()` of the Kotlin value
class for an annotated value-class property, for example through a Kotlin-aware resolver that the Spring Boot starter
or the Kotlin extension registers. The release notes or the changed resolver show it. Upstream plans it for Axon
Framework 5.7.0 (milestone on 2026-11-24), earlier if a pull request lands first.

## How to verify removal

Run the `KotlinTagResolverTest` against the new release: the test `Axon's own resolver tags the raw UUID...` fails as
soon as Axon's resolver returns the TypeID, which is the signal. Then delete `KotlinTagResolver`, its registration, its
test and both markers, and run `mise x -- ./gradlew :command-service:integrationTest`: the capture and conflict tests
still see their events through criteria built from the TypeIDs.

## Upstream issue draft

Target: `AxonIQ/AxonFramework` (the Kotlin extension lives in the same ecosystem), as a bug report. Published as https://github.com/AxonIQ/AxonFramework/issues/5328 on 2026-11-23.

Maintainer response on 2026-11-23 ([comment](https://github.com/AxonIQ/AxonFramework/issues/5328#issuecomment-7106068823)):
the report was filed as a bug and is accepted as a fair enhancement. It first looked like a duplicate of
AxonIQ/AxonFramework#3761, but that issue concerns `AnnotationBasedEntityIdResolver`, this one
`AnnotationBasedTagResolver`. A pull request would be much welcome; otherwise the issue is placed on 5.7.0, for the
Axoniq team to tackle at their earliest convenience.

Status on 2026-11-24: open, milestone "Release 5.7.0", label "Priority 3: Could".

Decision of the author on 2026-11-24: contribute the Kotlin-aware tag resolver that the published issue offers, after this
repository's Phase 2 pull request merges and after the UP-0001 pull request (#5326), starting with where the code
belongs (core or the Kotlin extension). This record is updated when the pull request merges, and the workaround is
removed only after a release.

### AnnotationBasedTagResolver tags the unboxed value of a Kotlin value-class property

#### Environment and versions

- Product and version: Axon Framework 5.4.2, `axon-kotlin` 5.4.2
- Related products and versions: Kotlin 2.4.21
- JDK, operating system and build tool: Temurin 25, Linux and macOS, Gradle 9.9.2

#### Steps to reproduce

1. Declare `@JvmInline value class CourseId(val uuid: UUID)` with a `toString()` that returns a prefixed string.
2. Annotate a property of an event with `@EventTag` that has the value class as its type.
3. Resolve the tags of the event with `AnnotationBasedTagResolver`.

#### Expected behavior

The tag value is `courseId.toString()`, the same text that application code uses to build `EventCriteria` from the
same ID.

#### Actual behavior

The tag value is the `toString()` of the underlying `UUID`, so criteria built from the value class match no event.
There is no error: the criteria select nothing and the decision is taken without its own history.

#### Minimal reproduction

```kotlin
@JvmInline
value class CourseId(val uuid: UUID) {
    override fun toString() = "course_$uuid"
}

data class StudentSubscribed(@field:EventTag(key = "course") val courseId: CourseId)

val id = CourseId(UUID.randomUUID())
val event = GenericEventMessage(MessageType(StudentSubscribed::class.java), StudentSubscribed(id))

check(AnnotationBasedTagResolver().resolve(event) == setOf(Tag.of("course", id.toString()))) // fails
```

#### Analysis

`AnnotationBasedTagResolver` reads the annotated JVM member reflectively
([source](https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/AnnotationBasedTagResolver.java#L91-L109)).
For a value-class property that member holds the underlying `UUID`. A `TagResolver` that reads the property through
`kotlin-reflect` and calls `toString()` on the boxed value returns the expected tag.

#### Proposed fix

Provide a Kotlin-aware `TagResolver` in the Kotlin extension and register it when the extension is on the classpath,
in the way `KotlinReflectPropertyAccessStrategy` is registered, or make the default resolver read properties through
the configured property access strategy. We can contribute the resolver (about 25 lines) as a pull request.
