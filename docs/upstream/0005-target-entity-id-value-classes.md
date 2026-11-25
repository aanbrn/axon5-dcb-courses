---
id: "UP-0005"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: false
upstream: ["https://github.com/AxonIQ/AxonFramework/issues/3761"]
reported: "2025-10-08"
updated: "2026-11-23"
---

# UP-0005: @TargetEntityId on a Kotlin value-class property resolves the unboxed UUID

## Symptom

A command whose `@TargetEntityId` property has a Kotlin value-class type, such as `CourseId`, resolves the entity ID as
the underlying `java.util.UUID`, not as the value class. An entity registered for the value-class ID type is then never
found: `EventSourcedEntityModule.autodetected(CourseId::class.java, ...)` fails with a `MissingRepositoryException`
that names the id type `java.util.UUID`.

Observed with Axon Framework 5.4.2, with and without the `axon-kotlin` extension on the classpath. Its
`KotlinReflectPropertyAccessStrategy`, which boxes value classes for property access, does not apply to the
resolution of `@TargetEntityId`.

The defect is already reported upstream as [AxonIQ/AxonFramework#3761][issue], open since 2025-10-08 with the label
`Priority 3: Could`. The observations above go beyond that issue and could be added to it as a comment. Writing the
comment is a publication step that needs the approval of the author (plan 02-22), so nothing is posted by this record.

[issue]: https://github.com/AxonIQ/AxonFramework/issues/3761

## Minimal reproduction

```kotlin
@JvmInline
value class CourseId(val uuid: UUID)

data class SubscribeStudent(@TargetEntityId val courseId: CourseId)

@EventSourced
class CourseSeats { /* ... */ }

EventSourcedEntityModule.autodetected(CourseId::class.java, CourseSeats::class.java)
```

Send a `SubscribeStudent` command. The command handler fails to find a repository for the id type `java.util.UUID`.
Registering the module with `UUID::class.java` as the id type works, because the JVM signatures agree: an
`@EventCriteriaBuilder` function taking a `CourseId` compiles to a function that takes a `java.util.UUID`, and the
getter of the property returns a `java.util.UUID`, while Kotlin code inside the entity still sees `CourseId`.

## Root cause

`AnnotationBasedEntityIdResolver` reads the annotated JVM member reflectively, and the resolved identifier is whatever
`ReflectionUtils.getMemberValue` returns ([`resolve` and `getIdentifiers`, lines 56-87][resolver]). For a value-class
property the member holds the underlying `UUID`, so the resolved id type is `java.util.UUID`, and the repository
lookup by the value-class type fails. It is the same unboxing as in [UP-0004](0004-tag-resolver-value-classes.md).

[resolver]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/modelling/src/main/java/org/axonframework/modelling/annotation/AnnotationBasedEntityIdResolver.java#L56-L87

## Workaround

None in the code yet, which is why `workaround` is false. The entities that need it arrive with the command service
of Phase 3, which registers them with the underlying `UUID` as the id type, as described in the reproduction. That
change sets `workaround` to true, adds an `UPSTREAM` marker with this record's number at each registration and lists
the locations here.

## Removal criteria

A released Axon Framework version that closes issue 3761 so that `@TargetEntityId` on a value-class property resolves
the value class, and `EventSourcedEntityModule.autodetected(CourseId::class.java, ...)` finds the repository. While the
workaround does not exist yet, the same release lets Phase 3 register the entities with the value-class ID type from the
start.

## How to verify removal

In a scratch test on the new release, register a minimal entity with `EventSourcedEntityModule.autodetected` and the
value-class id type, send a command with an annotated value-class property and expect it to be handled, with no
`MissingRepositoryException`. Once the workaround exists, the same test after changing the registrations to the
value-class id type is the check, followed by `mise x -- ./gradlew :command-service:integrationTest`.
