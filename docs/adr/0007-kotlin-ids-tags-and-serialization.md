---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0007: Entity IDs are TypeID value classes, tagged and serialized through Kotlin-aware wiring

## Context and Problem Statement

Entity IDs must be the same string in DCB tags, events, the API and the UI (IDEM-04): a TypeID, that is a type prefix plus
a UUIDv7 in Crockford base32, for example `course_01jdq8x6v2f3k9m4r7t5w0y1zb`. In Kotlin an ID is a value class that
validates its prefix and format. Three places have to agree on that string, and spike 002 found that the Axon defaults do
not agree with it:

- Which TypeID implementation do the JVM and the TypeScript frontend use? Decision D-07 asked for a mature library and
  allowed an own codec as the fallback.
- How do events get tagged with the TypeID, and how do entities load by it?
- How do events, commands and sealed command results cross the wire and come back as the same types?
- Which names do messages carry in the event store, and when does a name become impossible to change?

## Decision Drivers

- The canonical TypeID string is the only representation, and a wrong tag silently disables a DCB invariant.
- The contracts modules carry no framework at runtime (ADR-0009), so serialization concerns cannot live in them.
- One Jackson configuration serves Axon and, later, the BFF.
- Message names and versions are persisted with the first event, so they must be chosen once and deliberately.

## Considered Options

- Own TypeID codec on the JVM, verified against the official vectors (chosen) versus `de.fxlae:typeid-java` or
  `me.lessis:typeid`.
- `typeid-js` in the frontend (chosen) versus a second own codec.
- Jackson 3 with the Kotlin module through the Axon converter (chosen) versus kotlinx.serialization.
- `@JsonTypeInfo` directly on the sealed command results (chosen) versus a Jackson mix-in registered in the shared module.
- Explicit message names and versions (chosen) versus the package-derived default.

## Decision Outcome

Chosen options: the first of each pair above, for the reasons below.

### TypeID codec and ID classes

- **Own codec, no library.** No mature JVM library exists. Maven Central and GitHub were searched on 2026-11-22:
  `de.fxlae:typeid-java` follows spec v0.3 but its last release and push were in May 2024, it has 33 stars and one
  maintainer; its JDK 8 variant and `me.lessis:typeid` (spec v0.2, last release July 2023) are outdated or abandoned; a
  Kotlin library is not on Maven Central under that name. The codec is a small class (`TypeId` in
  `contracts-kernel`), verified against the official TypeID specification v0.3.0: the 9 valid and 21 invalid vectors from
  `spec/valid.yml` and `spec/invalid.yml` (30 in all, copied byte for byte and covered by `REUSE.toml` and `NOTICE`), plus
  property tests over random IDs.
- **UUIDv7 from a library.** `com.fasterxml.uuid:java-uuid-generator` generates the UUIDv7 values, and accepts a clock, so
  tests that depend on ordering stay deterministic (D-11).
- **Value classes over `UUID`.** `value class CourseId(override val uuid: UUID)` fixes its prefix, and `toString()` returns
  the canonical TypeID (D-06). That maps onto native `uuid` columns later and keeps the UUIDv7 ordering. The parse API is
  `parse(s)`, which throws `IllegalArgumentException` with a wrong-prefix or malformed message, and `parseOrNull(s)`
  (D-09).
- **TypeScript.** The frontend uses the official `typeid-js` library when it arrives in Phase 5. IDEM-04 says so: its
  former wording, "generators come from vetted libraries (maturity verified in Phase 1)", was corrected to state that the
  Kotlin codec is the project's own and that TypeScript uses `typeid-js`.

### Tags and entity IDs

- **Registry.** `object Tags` in the kernel holds the tag keys as `const val`, because annotations need compile-time
  constants. A tag key equals its TypeID prefix, so `course=course_01…` names the entity the same way its ID does (D-10).
- **`KotlinTagResolver`.** Axon's default `AnnotationBasedTagResolver` reads the annotated JVM field or getter and calls
  `toString()` on it. For a value class over `UUID` that is the unboxed raw UUID, so events are tagged
  `course=01a20333-…` while every criterion built from a `CourseId` asks for `course=course_01…`. The two never match
  and the criteria select nothing: no error, only invariants that silently stop being enforced. The short
  `KotlinTagResolver` reads the property through kotlin-reflect, so the value class is boxed and the tag is the
  canonical TypeID. It is registered as the `TagResolver` component and tracked as UP-0004.
- **Entities register with `UUID`.** `@TargetEntityId` on a value-class property resolves the unboxed type, so an entity
  module registered with `CourseId::class.java` fails with `MissingRepositoryException … with id type [java.util.UUID]`.
  Registering the entity with `UUID::class.java` works, because the JVM signatures of the criteria builder and the
  getter agree while Kotlin code still sees `CourseId`. This is tracked as UP-0005 and applies from Phase 3, with the
  first entity.
- **`axon-kotlin`.** A nullable `@InjectEntity` parameter is treated as non-null without the Axon Kotlin extension, so
  the command service adds it with the first entity in Phase 3.

### Serialization

- **Jackson 3 and the Kotlin module** through the Axon converter (`axon.converter.general=jackson` under Spring Boot)
  with one shared module, `CoursesJsonModule` in `serialization-jackson`, that holds the ID and `RejectionCode`
  (de)serializers and is reused by the BFF REST layer. kotlinx.serialization is rejected (D-08).
- **Sealed results carry `@JsonTypeInfo`.** `@JsonTypeInfo(use = SIMPLE_NAME, property = "@type")` sits on the sealed
  result interface, for example `SubscribeResult`, with `jackson-annotations` as a `compileOnly` metadata dependency of
  the contracts, the same rule as for Axon annotations (D-44). It is needed even inside one JVM, because command results
  cross the wire: without it Axon Server fails with `Cannot construct instance of … SubscribeResult` for the abstract
  type.
- **Message names and versions.** Every message declares `@Event`, `@Command` or `@Query` with the namespace
  `courses`, its class name as `name` and an explicit `version`. Axon Server stores the qualified type as
  `courses.CourseCreated#1` and the payload as the Jackson JSON with the TypeID strings. **They become one-way with the
  first persisted event**: renaming a message, changing its namespace or reusing a version breaks replay and needs
  upcasters or a new store (D-03).

### Consequences

- Good, because the TypeID has one definition per language, checked against the official vectors, and the project owes
  nothing to an abandoned dependency.
- Good, because tag values and criteria are produced from the same `toString()`, and a test guards the one failure that
  would be silent.
- Good, because the contracts stay free of framework runtime, and Jackson and Axon concerns sit in one module each.
- Bad, because the project maintains its own codec. A mature library would replace it, and the vectors would still
  be the test.
- Bad, because the Axon wiring leans on three defects worked around until upstream changes: UP-0004 (tags), UP-0005
  (entity ID type) and the nullable `@InjectEntity` behaviour.
- Bad, because a message name or version chosen now cannot change cheaply, so every message in Phase 3 needs a deliberate
  name.

### Confirmation

- `TypeIdVectorsTest` runs the 30 official vectors, and `TypeIdPropertyTest` the round-trip properties, in the kernel.
- `KotlinTagResolverTest` asserts that the tag value equals `id.toString()` and that Axon's own resolver tags the raw
  UUID, the guard for the silent mismatch.
- `JsonRoundTripTest` and `AxonServerSerializationIntegrationTest` prove the Jackson round trip, the stored type names
  and a sealed result through the command gateway on a real Axon Server.
- `MessageArchitectureTest` requires the `courses` namespace, the class name and an explicit version on every message, and
  an `@EventTag` key that is a reference into `Tags`. `ContractsArchitectureTest` keeps Jackson and Axon out of the
  contracts except as annotations (ADR-0009).

## Pros and Cons of the Options

### Own TypeID codec versus a JVM library

- Good, because no JVM library is mature enough: the best candidate is two years without a release and has one
  maintainer.
- Bad, because the project carries the code and the duty to follow spec changes. The vectors make a future switch safe.

### `typeid-js` versus a second own codec

- Good, because it is the official TypeScript implementation of the specification, so the frontend needs no codec of its
  own.
- Bad, because it is a dependency whose maturity is checked again in Phase 5, when the frontend lands.

### Jackson 3 versus kotlinx.serialization

- Good, because Jackson is the AF5 converter and the Boot 4 default, and one module serves Axon and the BFF.
- Bad, because value classes need explicit (de)serializers, which `CoursesJsonModule` provides.
- kotlinx.serialization would need a second converter in Axon and would not serve the REST layer, so it was rejected.

### `@JsonTypeInfo` on the sealed results versus a mix-in

- Good, because the type carries its own discriminator, so only the declared cases resolve when a result is read back, and
  nothing else has to be registered anywhere.
- Bad, because the contracts depend on `jackson-annotations` as `compileOnly` metadata. The spike's mix-in design,
  registered in the shared module so the contracts stayed annotation-free, is the rejected alternative (D-44): it works,
  but it keeps a rule about a contract type in a different module from the type.

### Explicit names and versions versus the package-derived default

- Good, because a rename or move of a class no longer changes the persisted name, and the namespace is short.
- Bad, because the author must choose a name and version for every message.

## More Information

- Spike: `.planning/spikes/002-kotlin-tags-jackson-typeid/README.md`, verdict PARTIAL: everything works, two defaults do
  not.
- Decisions: D-03, D-05 to D-10, D-44, and the IDEM-04 wording fix required by D-07.
- Upstream records: [UP-0004](../upstream/0004-tag-resolver-value-classes.md) (reported as AxonIQ/AxonFramework#5328) and
  [UP-0005](../upstream/0005-target-entity-id-value-classes.md) (covered by AxonIQ/AxonFramework#3761).
- Revisit the codec decision when a JVM TypeID library has a recent release, several maintainers and the vectors in its own
  tests. Revisit the tag and entity workarounds when UP-0004 and UP-0005 are fixed upstream, and the message naming only
  with upcasters in hand.
