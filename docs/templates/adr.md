---
status: "proposed"
date: "YYYY-MM-DD"
decision-makers: ["<name>"]
consulted: []
informed: []
---

<!--
Template for an architecture decision record in docs/adr/. It follows MADR 4.0.0 (https://adr.github.io/madr/). Copy it
to docs/adr/NNNN-<slug>.md (four digits, numbered from 0001 without gaps, lowercase slug), replace every placeholder
and delete this comment. `./gradlew checkAdrs` validates the result.

Front matter: only flat `key: value` lines and one-line lists are accepted, with double-quoted strings. `status`, `date`
and `decision-makers` are required; `consulted` and `informed` are optional.
- status: one of proposed, accepted, rejected, deprecated, or `superseded by ADR-NNNN`, where that ADR exists.
- date: YYYY-MM-DD, the day the decision was last updated.
- decision-makers: who made the decision, for example the repository author and the decision identifier.

Title: `# ADR-NNNN: <short title>` carries the file's number.

Required sections: Context and Problem Statement, Considered Options and Decision Outcome. Decision Drivers, Pros and
Cons of the Options and More Information are optional, so a small decision can stay small: name the problem, list the
options (the chosen one included) and state the outcome with its consequences. Keep Decision Drivers when the choice
is a trade-off between named forces, Pros and Cons of the Options when the options need a closer comparison, and More
Information for evidence, links and the criteria that make you revisit the decision.

Confirmation says how compliance with the decision is checked (a test, a Gradle check, a review duty). Put the
criteria for revisiting the decision there or under More Information.
-->

# ADR-NNNN: <short title of the problem and its solution>

## Context and Problem Statement

What forces the decision: the situation, the problem in two or three sentences, and the constraints that apply.

## Decision Drivers

- <a force or requirement that shapes the choice>

## Considered Options

- <the first option>
- <the second option>

## Decision Outcome

Chosen option: "<option>", because <the reason, ideally tied to the decision drivers>.

### Consequences

- Good, because <a positive consequence>.
- Bad, because <a negative consequence or a residual risk>.

### Confirmation

How the decision is verified, for example a test, a Gradle check or a review rule.

## Pros and Cons of the Options

### <the first option>

- Good, because <an argument>.
- Bad, because <an argument>.

### <the second option>

- Good, because <an argument>.
- Bad, because <an argument>.

## More Information

Evidence, links and the criteria under which this decision should be revisited.
