<!-- Suggested title: type(scope): one clear change, e.g. feat(NoBedExplosion): cancel bed explosion in end -->
<!-- PRs that bundle unrelated changes, stack on unmerged PRs, or skip checklist items may be closed without review. Keep it small and reviewable. -->

## What

<!-- 1-2 sentences: what changed and why. If this implements an issue, link it here. -->

Closes #

## Scope

- [ ] This PR is **one logical change** (a single feature, fix, or plugin). Unrelated changes belong in separate PRs.
- [ ] Cross-plugin feature grouping (optional): this PR spans several plugins/modules because they implement **one coherent feature**. Name the feature and what each part contributes: <!-- e.g. "PlayerLanguage system: core module adds the API, each plugin consumes it" -->. Remove this line if not applicable.
- [ ] This PR targets the **default branch** and does not stack on an unmerged PR.

## Design

- [ ] No new shared libraries, cross-plugin systems, or architecture decisions. If it does contain any, they were agreed in an issue **before** this PR: #
- [ ] Code for org-owned projects is upstreamed first. This PR does not vendor a fork of an org project. Upstream link (if applicable): #
- [ ] AI-generated or AI-assisted changes are disclosed, with the tool and the approved issue they implement: <!-- e.g. "Codex, implements #123" or "none" -->

## Evidence

- [ ] Commits follow Conventional Commits (`type(scope): description`). CI enforces this.
- [ ] Built and tested locally with this repo's standard commands. Paste evidence (commands run, logs, screenshots):

