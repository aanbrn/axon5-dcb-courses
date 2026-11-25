# Vendored skill: dynamic-consistency-boundaries

`SKILL.md` and `references/` are a byte for byte copy of the upstream skill and are never edited here. This README is
the project's provenance record and not part of the upstream skill. Claude Code loads the skill as a project skill, and
GSD maps it to agents through `agent_skills` (ADR-0004).

## Source

- Repository: https://github.com/AxonIQ/AxonFramework
- Path: `.claude/skills/dynamic-consistency-boundaries`
- Commit: `f521585912e2915d086158f62e1e6e2c351ae39b` of 2026-06-16
- Licence: Apache-2.0 (the repository's `LICENSE.txt`; the text is in `LICENSES/Apache-2.0.txt`, and `REUSE.toml` and
  `NOTICE` record it)
- Approval: by the repository author on 2026-11-25, against the review criteria in `docs/tooling.md` (section Claude
  Code skills)

## Why vendored

AxonIQ does not publish the skill in its marketplace `AxonIQ/agent-skills`. Once a marketplace publishes it, a plugin
pinned by commit replaces this directory (`docs/tooling.md`, Vendored skills).

## Version caveat

The skill is written against the framework's main branch (5.5.0-SNAPSHOT at the pinned commit), while the project pins
Axon Framework 5.4.2 in `gradle/libs.versions.toml`. Where it disagrees with the `spike-findings-axon5-dcb-courses`
skill or an ADR, those win. Paths such as `examples/university-java/` refer to the Axon Framework repository.

## Verify

The blob ids that this command prints for every file but this README:

```shell
git ls-files -s -- .claude/skills/dynamic-consistency-boundaries
```

equal those of the pinned commit on GitHub:

```shell
gh api 'repos/AxonIQ/AxonFramework/git/trees/f521585912e2915d086158f62e1e6e2c351ae39b:.claude/skills/dynamic-consistency-boundaries?recursive=1' --jq '.tree[] | select(.type == "blob") | [.sha, .path] | @tsv'
```

## Update

1. Find the newest upstream commit of the skill. If it equals the pinned commit, there is nothing to do.

   ```shell
   gh api 'repos/AxonIQ/AxonFramework/commits?path=.claude/skills/dynamic-consistency-boundaries&sha=main&per_page=1' --jq '.[0].sha'
   ```

2. Read the change at `https://github.com/AxonIQ/AxonFramework/compare/<pin>...<new>` against the review criteria
   (Markdown only; no hooks, scripts, MCP or LSP configuration) and get the author's approval of the new commit in the
   pull request.
3. Replace `SKILL.md` and `references/` with the files of the new commit byte for byte, for example with `git archive`
   of that commit from a clone of AxonIQ/AxonFramework, extracted at the repository root, and delete the files that
   upstream removed.
4. Update the commit and date in this README. If upstream added files outside `references/`, extend the annotation in
   `REUSE.toml` and the entry in `NOTICE`.
5. Run the verification above, `reuse lint` and `bash scripts/test-doctor.sh`.

Check whenever Renovate proposes an Axon Framework update and before planning a phase that designs DCB decisions. No
Renovate manager tracks this skill; the reasons are in `docs/tooling.md`, Vendored skills.
