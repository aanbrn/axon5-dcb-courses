---
spike: 002
idea: phase2-runtime-dcb-foundations
name: kotlin-tags-jackson-typeid
type: standard
validates: "Given TypeID UUID value-class ids, @EventTag keys from a registry and @Event names, when events and commands round-trip through Jackson 3 and Axon Server, then payloads, tags, qualified names and sealed command results survive"
verdict: PARTIAL
related: [001, 003]
tags: [kotlin, jackson3, typeid, value-class, event-tags, sealed-results, axon-kotlin]
---

# Spike 002: Kotlin Tags, Jackson 3 and TypeID Value-Class IDs

## What This Validates
Given `@JvmInline value class CourseId(val uuid: UUID)` ids whose `toString()` is the canonical TypeID, `@EventTag`
keys from `object Tags`, explicit `@Event`/`@Command(namespace, name, version)`, and sealed command results, when they
round-trip through Jackson 3 (AF5 `JacksonConverter`) and Axon Server 2026.1.8, then payloads, tag values, stored type
names and command results survive — including the DCB entity path (`@EventCriteriaBuilder`, `@TargetEntityId`,
`@InjectEntity`). Also: is there a mature JVM TypeID library (D-07)?

## Research
- **TypeID spec**: jetify-com/typeid `spec/README.md`, **Version 0.3.0** (last spec change 2025-03-03 "Clarify typeid
  spec (#445)"); vectors `spec/valid.yml` (9) and `spec/invalid.yml` (21) at commit
  `26a84e85491203834ed0d1c3423ba5f679a67114`. Prefix regex `^([a-z]([a-z_]{0,61}[a-z])?)?$`; suffix = 26 chars,
  lowercase Crockford alphabet, 2 zero bits + 128 bits, first char ≤ `7`. Decoders accept any UUID version; new ids
  should be UUIDv7.
- **JVM libraries** (Maven Central search + GitHub, 2026-11-22):

| Library | Spec | Activity | Verdict |
|---------|------|----------|---------|
| `de.fxlae:typeid-java` 0.3.1 | v0.3 | last release/push 2024-05-18, 33 stars, single maintainer | not mature enough |
| `de.fxlae:typeid-java-jdk8` 0.2.1 | v0.2 | 2024-05 | outdated |
| `me.lessis:typeid` 0.0.2 (softprops) | v0.2 | 2023-07 | abandoned |
| aleris/typeid-kotlin | v0.3 | 8 stars, not on Maven Central under that name | no |

  → D-07 fallback: own codec (≈60 lines), verified by the official vectors + 100 000 random round trips; UUIDv7 from
  `com.fasterxml.uuid:java-uuid-generator` 5.2.0 (`Generators.timeBasedEpochGenerator()`; it also accepts a custom
  clock for D-11).
- **Tag resolution** (`AnnotationBasedTagResolver`): reads the annotated *JVM field or method* reflectively and uses
  `value.toString()`. A Kotlin value-class property compiles to a `UUID` field and a mangled getter returning `UUID`.
- **axon-kotlin 5.4.2**: `KotlinReflectPropertyAccessStrategy` (boxes value classes for property access) and
  `KotlinReflectNullabilityResolver` (nullable `@InjectEntity`), registered via `META-INF/services`. It has no tag
  resolver and does not affect `@TargetEntityId` resolution.

## How to Run
```bash
./gradlew -p .planning/spikes :002-serialization:test -PwithAxonKotlin   # Docker needed (Axon Server Testcontainer)
./gradlew -p .planning/spikes :002-serialization:test --tests '*ValueClassEntityPath*'   # without axon-kotlin
```

## What to Expect
9 tests pass with `-PwithAxonKotlin`. Output shows JSON (`{"courseId":"course_01…","studentId":"student_01…"}`,
`{"@type":"Rejected",…}`), default vs Kotlin tags, sourced types `courses.CourseCreated#1`, and the entity results.

## Investigation Trail
1. **TypeID codec**: all 9 valid vectors decode and re-encode, all 21 invalid vectors throw `IllegalArgumentException`,
   100 000 random UUIDs round-trip; `CourseId.random()` is UUIDv7; wrong prefix gives
   `Wrong TypeID prefix in 'student_…': expected 'course', got 'student'`; `parseOrNull("garbage") == null`.
2. **Jackson 3**: a `SimpleModule` with `ValueSerializer`/`ValueDeserializer` per id type is honoured for value-class
   properties by jackson-module-kotlin (JSON holds the TypeID string, reads back equal). Sealed results: contracts stay
   free of Jackson — a **mix-in** `@JsonTypeInfo(use = SIMPLE_NAME, property = "@type")` registered in the same module
   is enough; jackson-module-kotlin discovers the sealed subtypes; `data object Subscribed` deserialises to the same
   singleton (`===`).
3. **Tags — surprise**: the default `AnnotationBasedTagResolver` produces `course=01a20333-c479-…` (raw UUID) for all
   three annotation forms (`@EventTag` on the constructor property — lands on the field with the project's compiler
   flags —, `@field:EventTag`, `@get:EventTag`). Criteria built with `Tag.of(COURSE, id.toString())` would then match
   **nothing**: DCB invariants silently unenforced. A 25-line `KotlinTagResolver` (public `TagResolver` interface,
   kotlin-reflect, so value classes are boxed) yields `course=course_01…`. Over Axon Server, sourcing by the TypeID tag
   returns both events, by the raw UUID tag returns 0.
4. **Stored names**: types are stored as `courses.CourseCreated#1` / `courses.StudentSubscribed#1`
   (`namespace.name#version`); payload is the Jackson JSON with TypeID strings.
5. **Command results over Axon Server**: `SubscribeResult.Subscribed` and `Rejected(code, courseId)` return through
   `CommandGateway.resultAs(SubscribeResult::class.java)` (results cross the wire even inside one JVM — spike 001 first
   failed with `Cannot construct instance of spike.SubscribeResult` before polymorphic type info).
6. **Spring Boot** (`axon.converter.general=jackson`): a `CoursesJsonModule` bean is picked up by Spring's `JsonMapper`
   and thus by Axon's converter; a `TagResolver` bean replaces the default; sealed result via gateway works.
7. **Entity path — surprise**: `EventSourcedEntityModule.autodetected(CourseId::class.java, …)` →
   `MissingRepositoryException … with id type [java.util.UUID]`: `@TargetEntityId` reads the unboxed `UUID`, with or
   without axon-kotlin. Registering with **`UUID::class.java`** works, because the JVM signatures agree:
   `@EventCriteriaBuilder fun criteria(id: CourseId)` compiles to `criteria-dDSv8GE(java.util.UUID)` and the getter to
   `getCourseId-9rP37ds(): UUID`; Kotlin code inside still sees `CourseId`.
8. `@InjectEntity seats: CourseSeats` (non-null) on a not-yet-existing entity → `EntityNotFoundException`
   (`MissingEntityStrategy.FAIL`). `CourseSeats?` works **only with axon-kotlin** on the classpath (without it the `?`
   is ignored and the same exception is thrown). With axon-kotlin + `UUID` id type: create → `Subscribed`, duplicate
   create → `Rejected(course-exists)`, capacity 2 → `[Subscribed, Subscribed, Rejected(course-full)]`.

## Results
**PARTIAL** — everything works, but two defaults do not, and both fail silently or confusingly:
- Required for the build:
  - a `KotlinTagResolver` registered as the `TagResolver` component/bean (without it every tag holds the raw UUID);
  - entities registered with the id's underlying type (`UUID`) — or a custom `EntityIdResolverDefinition` (not tried);
  - `axon-kotlin` on every command service classpath (nullable `@InjectEntity`);
  - one shared Jackson 3 module: id (de)serializers + sealed-result mix-ins; contracts need no Jackson dependency;
  - own TypeID codec + JUG UUIDv7; ADR records why no library (D-07) and clarifies IDEM-04 "vetted libraries".
- Permanent tests to carry over: TypeID vectors; Jackson round trip; **tag values equal `id.toString()`** (guards the
  silent-mismatch failure); Axon Server round trip with stored type names; sealed result over the gateway.
- Upstream-record candidates: `AnnotationBasedTagResolver` ignores Kotlin value classes; `@TargetEntityId` on a
  value-class property resolves the unboxed type (axon-kotlin's property strategy does not apply there).
