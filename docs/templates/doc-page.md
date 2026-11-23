---
title: "<page title>"
description: "<one sentence that says what the reader learns, used by the documentation site and search engines>"
---

<!--
Template for a documentation page. Copy it to the place of the page in docs/ (the documentation site is built from it)
and delete this comment. `title` and `description` are required by the site; keep both on one line each, in double
quotes.

Write for one named audience per page, in the order the sections below give. Keep a section only when it has something
to say, except Purpose and audience and Content. Link to the decisions and the tests instead of repeating them: an ADR
owns the reasoning, a test owns the proof.

Diagrams are text: use Mermaid blocks (a fenced block marked `mermaid`) and keep each one small enough to read without
zooming. Name files and code in backticks, and write the full command when a page asks the reader to run something.
-->

# <page title>

## Purpose and audience

Who this page is for, what they can do after reading it, and what they should already know.

## Content

The body of the page. Use as many second-level headings as the topic needs, each answering one question.

## Related decisions

- [ADR-NNNN: <title>](adr/NNNN-<slug>.md): <why this decision matters to the reader of this page>.

## Related tests

The tests that prove what this page claims, named by the business rule IDs they carry as `@Tag` values, for example
`SUBS-03`, so a reader can find them by tag.
