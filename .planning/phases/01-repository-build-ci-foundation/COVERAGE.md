# API Coverage — Phase 1 repository and CI tooling

> Full coverage by default. Opt-outs are explicit, reasoned decisions.

Phase 1 ships no product code and integrates no product API. The seal-time detector found an external-API
surface signal in the Phase 1 plans, so this matrix enumerates the external APIs the phase does touch. They are
repository and CI tooling, listed below one section per surface. Every INTEGRATE row names the file that
performs the call.

Repository settings, rulesets and labels change in only two places: the approval-gated `--apply` mode of
`scripts/apply-github-settings.sh` and the labels sync that runs on `main`. Apart from that, CI uploads results
(SARIF, artifacts, Scorecard) and Renovate opens pull requests.

Package registries that resolve build and tool dependencies are dependency sources, not integrations, and are
out of scope. These are Maven Central, the Gradle Plugin Portal, npm and PyPI.

The file was written at seal time by quick task 261121-wjt.

## GitHub REST API: pull requests (governance gate)

| capability        | decision  | reason                                                                                                                                  |
| ----------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| pulls.list-files  | INTEGRATE | GET repos/{owner}/{repo}/pulls/{n}/files with --paginate in scripts/governance/list-pr-files.sh. Read-only (pull-requests: read), fails closed. |
| pulls.read-labels | OPT-OUT   | Labels come from the pull_request event payload (PR_LABELS_JSON in governance.yml), so no API read is needed.                           |
| pulls.write       | OPT-OUT   | The gate never comments, reviews or merges, so its token stays read-only.                                                               |

## GitHub REST API: repository settings (scripts/apply-github-settings.sh)

| capability                      | decision  | reason                                                                                                                                      |
| ------------------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| repos.get                       | INTEGRATE | GET repos/{owner}/{repo} (also gh repo view) in every mode, including the scheduled --check run by settings-drift.yml.                      |
| repos.update                    | INTEGRATE | PATCH repos/{owner}/{repo} for the merge settings and security_and_analysis (secret scanning, push protection). Runs only in --apply, by the author. |
| vulnerability-alerts            | INTEGRATE | A GET probe reads 2xx as on and 404 as off. --apply sends PUT or DELETE.                                                                    |
| private-vulnerability-reporting | INTEGRATE | GET reads the enabled flag. --apply sends PUT or DELETE.                                                                                    |
| automated-security-fixes        | INTEGRATE | GET reads the enabled flag, --apply sends PUT or DELETE. Declared false in .github/repository-settings.json.                                |
| rulesets.read                   | INTEGRATE | GET repos/{owner}/{repo}/rulesets and rulesets/{id} for the drift comparison and the bypass-actor read-back.                                |
| rulesets.write                  | INTEGRATE | POST rulesets or PUT rulesets/{id} from .github/rulesets/main.json, only in --apply. Applying main stays deferred until checks are green (plan 01-14). |
| rulesets.delete                 | OPT-OUT   | The script never deletes a ruleset, so removing protection stays a manual, reviewed decision.                                               |
| branch-protection.classic       | OPT-OUT   | The repository is protected by the ruleset in .github/rulesets/main.json, not by classic branch protection.                                 |
| actions.secrets                 | OPT-OUT   | The author set GRADLE_ENCRYPTION_KEY and SETTINGS_READ_TOKEN once (plan 01-14). No script reads or writes secrets.                          |
| actions.variables               | OPT-OUT   | The author maintains SETTINGS_DRIFT_SECTIONS. settings-drift.yml reads it through the vars context, not the API.                            |

## GitHub REST API: labels (labels.yml)

| capability  | decision  | reason                                                                                                                                  |
| ----------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| labels.sync | INTEGRATE | crazy-max/ghaction-github-labeler syncs .github/labels.yml: a dry run on pull requests (issues: read), the sync on main (issues: write). |

## GitHub Actions platform (workflows)

| capability              | decision  | reason                                                                                                                                  |
| ----------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| actions.checkout        | INTEGRATE | actions/checkout with persist-credentials: false in every workflow, every action pinned by SHA (ADR-0001).                              |
| actions.toolchains      | INTEGRATE | actions/setup-java, actions/setup-node and jdx/mise-action in ci.yml, installing the tools locked in mise.lock.                         |
| actions.gradle-cache    | INTEGRATE | gradle/actions/setup-gradle in ci.yml. The cache is read-only off main and encrypted with GRADLE_ENCRYPTION_KEY.                        |
| actions.artifacts       | INTEGRATE | actions/upload-artifact in ci.yml keeps the build reports of a failed build for seven days.                                             |
| code-scanning.upload    | INTEGRATE | github/codeql-action init and analyze in codeql.yml (actions language, ADR-0002), upload-sarif in scorecard.yml, with security-events: write. |
| code-scanning.read-alerts | OPT-OUT | Findings are read in the GitHub UI. No workflow or script queries alerts.                                                               |
| releases.publish        | OPT-OUT   | Not needed yet. release-please owns tags and GitHub Releases from Phase 12 (ROADMAP Phase 12, criterion 5).                             |
| packages.publish        | OPT-OUT   | Not needed yet. Images go to GHCR from Phase 12 (ROADMAP Phase 12, criterion 5).                                                        |
| attestations.create     | OPT-OUT   | Not needed yet. Provenance attestations arrive with signed releases in Phase 12.                                                        |

## OpenSSF Scorecard API

| capability        | decision  | reason                                                                                                                  |
| ----------------- | --------- | ----------------------------------------------------------------------------------------------------------------------- |
| scorecard.publish | INTEGRATE | ossf/scorecard-action with publish_results: true and id-token: write (OIDC) in scorecard.yml.                           |
| scorecard.badge   | OPT-OUT   | Not needed yet. README badges, Scorecard included, are Phase 13 scope (ROADMAP Phase 13, criterion 1).                  |

## REUSE API (api.reuse.software)

| capability     | decision  | reason                                                                                                                 |
| -------------- | --------- | ---------------------------------------------------------------------------------------------------------------------- |
| reuse.badge    | INTEGRATE | The status badge image in README.md, GET badge/github.com/{owner}/{repo}.                                              |
| reuse.info     | INTEGRATE | The badge link target in README.md, GET info/github.com/{owner}/{repo}.                                                |
| reuse.register | OPT-OUT   | Registration is a one-time author step in the browser, an open item of plan 01-14. No tooling sends project data to the API. |

## Renovate (Mend Renovate GitHub App, D-11)

| capability                       | decision  | reason                                                                                                                       |
| -------------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------------- |
| renovate.version-updates         | INTEGRATE | renovate.json5 (config:best-practices) opens update pull requests for every version source.                                  |
| renovate.action-digest-pinning   | INTEGRATE | helpers:pinGitHubActionDigestsToSemver in renovate.json5.                                                                    |
| renovate.automerge               | INTEGRATE | automerge: true in renovate.json5 for exactly the three D-12 rules, with squash platform automerge. Everything else is merged by hand. |
| renovate.vulnerability-alerts    | INTEGRATE | renovate.json5 gives these pull requests the security label and never automerges them.                                       |
| renovate.config-validation       | INTEGRATE | renovate-config-validator from npm:renovate (mise.toml), run locally, in CI (ci.yml) and in the hook (docs/tooling.md).      |
| renovate.self-hosted             | OPT-OUT   | D-11 runs Renovate as the Mend GitHub App, so no workflow runs the renovate CLI against GitHub.                              |
