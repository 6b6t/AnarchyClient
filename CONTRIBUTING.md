# Contributing

6b6t utility mod. For build commands, code style, and agent instructions, see [AGENTS.md](AGENTS.md).

## Setup

Clone the repo, then: `./gradlew build`.

## Contribution Policy

**Scope.** One logical change per PR: a single feature, fix, or plugin. A PR may span several plugins or modules only when they implement one coherent cross-plugin feature; state that explicitly in the PR description. Unrelated changes belong in separate PRs.

**Base branch.** Every PR targets the default branch. PRs stacked on unmerged branches are rejected.

**Design first.** New shared libraries, cross-plugin systems, or architecture decisions need an approved issue before code is written.

**Upstream first.** Org-owned projects are not vendored. Changes to them go to the original project; a PR here must link the upstream change or issue.

**AI assistance.** Disclose AI-generated or AI-assisted code in the PR: which tool, and the approved issue it implements.

**Commit style.** Conventional Commits: `type(scope): description` (feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert). CI enforces this on every PR.

**Evidence.** Build and test locally, and paste the evidence (commands run, logs, screenshots) in the PR.

## Pull requests

Open PRs against the default branch and fill in the PR template. CI validates commit messages. Reviews are routed via [CODEOWNERS](.github/CODEOWNERS).
