# Kotlin IDs, Event Tags and Jackson 3 Serialization

## Requirements

- Spike code lives in `.planning/spikes/`, built by a standalone Gradle build whose versions come from the main catalog.
  (No further idea-scoped requirement covers this area; the D-03..D-10 decisions in `02-CONTEXT.md` apply.)

## How to Build It

1. **TypeID codec** (spec v0.3.0, own implementation — no mature JVM library exists): prefix regex
   `^([a-z]([a-z_]{0,61}[a-z])?)?$`, suffix 26 chars lowercase Crockford `0123456789abcdefghjkmnpqrstvwxyz`,
   2 zero bits + 128 bits, first char ≤ `7`. ~60 lines in `sources/002-…/src/main/kotlin/spike/typeid/TypeId.kt`.
   Test with the official `spec/valid.yml` (9) and `spec/invalid.yml` (21) from jetify-com/typeid commit
   `26a84e85491203834ed0d1c3423ba5f679a67114`, plus 100 000 random round trips. UUIDv7:
   `com.fasterxml.uuid:java-uuid-generator` (`Generators.timeBasedEpochGenerator()`, accepts a clock for D-11).
2. **ID value class** (D-06/D-09):

   ```kotlin
   @JvmInline
   value class CourseId(override val uuid: UUID) : TypedId {
       override fun toString() = TypeId.encode(PREFIX, uuid)
       companion object : TypedIdCompanion<CourseId>("course", ::CourseId) { const val PREFIX = "course" }
   }
   // companion gives parse(s) (IllegalArgumentException "Wrong TypeID prefix … expected 'course', got 'student'"),
   // parseOrNull(s), random(), of(uuid)
   object Tags { const val COURSE = CourseId.PREFIX; const val STUDENT = StudentId.PREFIX }
   ```

3. **KotlinTagResolver** — register as the `TagResolver` component/bean (mandatory):

   ```kotlin
   class KotlinTagResolver : TagResolver {
       override fun resolve(event: EventMessage): Set<Tag> {
           val payload = event.payload() ?: return emptySet()
           return payload::class.memberProperties.flatMap { prop ->
               val anns = buildList {
                   addAll(prop.annotations.filterIsInstance<EventTag>())
                   prop.javaField?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
                   prop.javaGetter?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
               }
               if (anns.isEmpty()) return@flatMap emptyList()
               val value = prop.getter.call(payload) ?: return@flatMap emptyList()
               anns.distinct().map { Tag.of(it.key.ifBlank { prop.name }, value.toString()) }
           }.toSet()
       }
   }
   ```

4. **Messages**: `@Event(namespace = "courses", name = "CourseCreated", version = "1")`, same for `@Command`; stored as
   `courses.CourseCreated#1`. `@EventTag(key = Tags.COURSE)` on the constructor property works (lands on the field).
5. **One Jackson 3 module** (`tools.jackson`), shared by the Axon converter and the BFF: `SimpleModule` with a
   `ValueSerializer`/`ValueDeserializer` per id type (`gen.writeString(id.toString())` / `Companion.parse`) and a
   mix-in per sealed result: `@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "@type")`.
   jackson-module-kotlin discovers sealed subtypes; `data object` deserialises to the singleton. Contracts need no
   Jackson dependency. In Spring Boot (`axon.converter.general=jackson`) a module `@Bean` reaches Axon's converter.
6. **Entities**: register with the **underlying** id type, keep Kotlin types in code:

   ```kotlin
   EventSourcedEntityModule.autodetected(UUID::class.java, CourseSeats::class.java)
   @EventCriteriaBuilder fun criteria(id: CourseId) = …   // JVM: criteria-<hash>(java.util.UUID)
   data class SubscribeToCourse(@TargetEntityId val courseId: CourseId, …)  // getter returns UUID
   @CommandHandler fun handle(cmd: …, @InjectEntity seats: CourseSeats?, appender: EventAppender)
   ```

   `org.axonframework.extensions.kotlin:axon-kotlin` must be on the command service classpath (nullable
   `@InjectEntity`).
7. **Permanent tests**: TypeID vectors; Jackson round trip of events, commands and sealed results; **tag value equals
   `id.toString()`** (guards the silent mismatch); Axon Server round trip (source by TypeID tag finds, by raw UUID
   finds nothing; stored type names); sealed result over `CommandGateway.resultAs(...)`.

## What to Avoid

- **Axon's default `AnnotationBasedTagResolver`** with value classes: tags become the raw UUID for every annotation
  form (`@EventTag`, `@field:`, `@get:`) — criteria built from `id.toString()` silently match nothing and DCB
  invariants go unenforced.
- **`autodetected(CourseId::class.java, …)`**: `MissingRepositoryException … with id type [java.util.UUID]`, with or
  without axon-kotlin.
- **Non-null `@InjectEntity` for possibly-new entities**: `EntityNotFoundException` (`MissingEntityStrategy.FAIL`).
  `Entity?` without axon-kotlin behaves the same.
- **Sealed results without polymorphic type info**: command results cross the wire even in one JVM →
  `Cannot construct instance of … SubscribeResult (abstract types …)`.
- **Third-party TypeID libraries**: `de.fxlae:typeid-java` 0.3.1 (idle since 2024-05, 33 stars), `me.lessis:typeid`
  (spec v0.2, abandoned), aleris/typeid-kotlin (not on Maven Central under that name).
- `@SpringBootApplication` classes without `kotlin("plugin.spring")`: "@Configuration class may not be final".

## Constraints

- Project compiler flags (`-Xjsr305=strict -Xjspecify-annotations=strict`, `allWarningsAsErrors`) were mirrored; no
  `-Xannotation-default-target` override.
- `AxonConfiguration`/`MessageStream.reduce` results are JSpecify-nullable in Kotlin (`!!` needed).
- The UUID-id-type approach relies on Kotlin's documented value-class JVM ABI (name mangling, unboxed getters).
  Alternative not tried: a custom `EntityIdResolverDefinition` (also needs a custom criteria resolver).
- Upstream-record candidates: `AnnotationBasedTagResolver` ignores value classes; `@TargetEntityId` on a value-class
  property resolves the unboxed type.

## Origin

Synthesized from spikes: 002
Source files available in: sources/002-kotlin-tags-jackson-typeid/
