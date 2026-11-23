---
title: "<slice name>"
description: "<one sentence: what the slice does and which rule it enforces>"
---

<!--
Template for the description of one Event Modeling slice (DOC-05). Copy it to the slice's page under docs/ and delete
this comment. One slice is one command and the decision that handles it, or one read model, or one processor, so keep
the page to that single unit.

Diagram: Mermaid `eventmodeling` (Mermaid 11.15 or newer). Frames are `tf <number> <type> <Name>` with the types `ui`,
`cmd`, `evt`, `rmo` and `pcr`; the numbers are unique, and relations between neighbouring frames are inferred. Delete the
lanes the slice does not use.

Given/When/Then: write the examples in business language, one per rule the command enforces, plus the narrow-boundary
case that must not conflict. Each example is a test of the decision (TEST-01), so name the same events and tags the
test uses.

Consistency boundary: name the criteria the decision reads, as tags and event types, and say why each is in the
boundary. A tag or type that is not needed to protect the rule widens the boundary and causes avoidable conflicts.
-->

# <slice name>

## Command

`<CommandName>`: what the caller asks for, and who the caller is (an actor, a UI or a processor).

## Diagram

```mermaid
eventmodeling

tf 01 ui <ScreenName>
tf 02 cmd <CommandName>
tf 03 evt <EventName>
tf 04 rmo <ReadModelName>
tf 05 pcr <ProcessorName>
```

## Examples

### <the success case>

- Given: <the events that already happened, in business language>
- When: <the command with its key values>
- Then: <the events appended, with their tags>

### <a rejection case>

- Given: <events>
- When: <command>
- Then: rejected with `<REJECTION_CODE>`, and no event is appended.

### <a case that must not conflict>

- Given: <events that concern other courses or students>
- When: <command>
- Then: the append succeeds, because the other events are outside the boundary.

## Consistency boundary

| Criterion (tag or event type) | Source events         | Why it is in the boundary          |
| ----------------------------- | --------------------- | ---------------------------------- |
| `<tag>`                       | `<EventType>`, `<..>` | <the rule that needs these events> |

## Rejection codes

| Code               | Meaning                     | Business rule |
| ------------------ | --------------------------- | ------------- |
| `<REJECTION_CODE>` | <when the decision rejects> | <rule ID>     |

## Related tests and decisions

- Tests: the decision tests and the business rule tag they carry, for example `SUBS-03`.
- Decisions: [ADR-NNNN: <title>](../adr/NNNN-<slug>.md).
