# Repository Guidelines

## Project Structure & Module Organization

AnarchyClient is a single-module Fabric client utility mod for Minecraft `26.2`. The Gradle build currently uses Fabric Loom `1.17.12`, Fabric Loader `0.19.3`, Fabric API `0.153.0+26.2`, Java `25`, and publishes jars as `anarchyclient-mc-26.2`.

Main Java code lives in `src/main/java`. Project-owned classes are rooted under `net.blockhost.anarchyclient`:

- `AnarchyClient.java` is the Fabric client entrypoint.
- `module/` contains module abstractions, registration, categories, and implementations in `module/impl`.
- `setting/` contains typed setting specs and setting values.
- `config/` handles client config serialization and persistence.
- `ui/` contains the Rivet-backed menu, panels, setting controls, and theme code.
- `rivet/` adapts Rivet to Minecraft input, shaped text, textures, and Blaze3D rendering.

Resources live in `src/main/resources`. Key files are `fabric.mod.json`, `anarchyclient.mixins.json`, `anarchyclient.accesswidener`, and assets under `assets/anarchyclient`. Tests live in `src/test/java` and mirror the production package layout.

Rivet is consumed through `com.github.Lenni0451.rivet:core:8dfcc3dd17`, with upstream source at `https://github.com/Lenni0451/rivet`. Prefer project-owned bridge changes in `net.blockhost.anarchyclient.rivet` over vendoring or rewriting Rivet internals.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper.

```bash
./gradlew build
```

Compiles the mod, processes resources, validates the access widener, runs tests, and produces build artifacts in `build/libs`.

```bash
./gradlew test
```

Runs the JUnit test suite.

```bash
./gradlew runClient
```

Starts the Fabric development client using the `run/` directory. The in-game client menu opens with `Right Shift`.

```bash
./gradlew genSources
```

Decompiles the Minecraft `26.2` sources through Fabric Loom. Use this when you need to inspect Minecraft internals instead of guessing API shape.

## Fabric Loom Minecraft Sources

After `./gradlew genSources`, Loom writes the generated Minecraft sources jar under:

```text
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<hash>/<minecraftVersion>/minecraft-merged-<hash>-<minecraftVersion>-sources.jar
```

For this project and version, the source jar path has this shape after `./gradlew genSources`:

```text
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<hash>/26.2/minecraft-merged-<hash>-26.2-sources.jar
```

The adjacent `minecraft-merged-<hash>-26.2.jar` is the compiled merged Minecraft jar. Treat everything under `.gradle/loom-cache` as generated reference material. Do not commit those cache files.

## Coding Style & Naming Conventions

Target Java `25`; Gradle configures UTF-8 and `--release 25`. Use idiomatic Java naming: classes and records in `PascalCase`, methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`, and packages in lowercase under `net.blockhost.anarchyclient`.

Prefer small module classes that extend the existing module and setting abstractions. Avoid duplicate control flow when a shared helper already exists. For immutable collections, prefer `List.of()`, `Set.of()`, and `Map.of()` where they fit. Prefer `try-with-resources` for closeable resources, descriptive method names, and focused classes that match the existing package boundaries.

For Rivet UI, use Rivet idiomatically for layout and composition. Prefer built-in components, theme options, and layout anchors such as `GridLayoutOptions` over manual positioning. Model visible UI atoms as components wherever practical: icons, labels, value text, dividers, active stripes, backgrounds, borders, buttons, rows, and controls should have real component bounds so Rivet owns layout, hit testing, and debug output. Keep manual pixel rendering inside small primitive components only when Rivet does not provide the shape directly, such as a custom toggle thumb or one reusable icon component. Avoid large `render` methods that hand-place text and icons with raw coordinates when child components and `computeLayout` can express the structure.

Keep module expansion state and rendered children synchronized before layout so click state does not need a second interaction to catch up.

## Testing Guidelines

JUnit Jupiter is configured through Gradle. Put tests in `src/test/java` with matching package names, and name test classes after the unit under test, such as `ModuleManagerTest`.

Add targeted tests when changing module registration, config serialization, settings behavior, movement or inventory helpers, targeting logic, access widener-sensitive code, or input/render bridge logic. No strict coverage gate is configured, but new behavior should have practical regression coverage. Run `./gradlew test` for focused changes and `./gradlew build` before publishing or handing off broad changes.

## Commit & Pull Request Guidelines

Use Conventional Commit messages: `<type>(<scope>): <subject>`, for example `feat(module): add auto totem settings` or `fix(config): persist disabled modules`. Prefer common types like `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `build`, and `perf`. Keep subjects imperative and concise, and omit the scope if it is not clear.

Do not start new branches unless explicitly requested. Do not bypass commit hooks, Lefthook, or any other configured hook. Do not use `git commit -n`; wait for hooks to finish normally.

Pull requests should describe the user-visible change, list affected areas, include verification commands, and link any related issue. Include screenshots or short clips for UI changes when practical. Avoid drive-by reformatting.

## Review and deployment approval

Before staging or deploying any change, complete these steps in order:

1. Open a pull request for the change.
2. Obtain a human's review and explicit sign-off on the PR's exact changeset.
   The reviewer can be the person who prompted the work, except where another person's review is required.
   A prompt or general permission to proceed does not approve an unseen changeset.
   If the changeset changes after sign-off, obtain new review and sign-off before merging.
3. Merge the approved pull request into `main` (or the repository's default branch).

Only stage or deploy artifacts and configuration from that merged, approved changeset.
This requirement includes uploads to `plugins-staging/`, direct live changes, and automated deployments.
Keep the existing requirements for deployment authorization, dry runs, and verification.

### PR review feedback

- Before merging, address all PR review feedback, including findings from Codex, Claude, and CodeRabbit.
- Record how each finding was addressed and resolve its review thread before merging.
- For a finding that does not require a change, document the reason and obtain human sign-off.
- If review fixes change the approved changeset, obtain new human sign-off before merging.

### Pull requests and direct pushes

- Prefer pull requests for changes.
- Push directly to `main` or the repository's default branch only when the user explicitly requests that direct push.
- A request to implement, stage, or deploy a change does not authorize a direct push.

### New Kubernetes services

- Before creating a Kubernetes service, obtain another person's review and approval of the exact changeset in a PR.
- The reviewer must be a human other than the person requesting or implementing the service.
- Merge the approved pull request into `main` before creating the service in the cluster.
- This requirement covers Kubernetes `Service` resources and new application services, including those created through Helm, scripts, or other automation.

### Plugin staging and live deployment

- NEVER stage or deploy a plugin whose exact source changeset is not merged into `main`.
- This prohibition includes uploads to `plugins-staging/`, direct replacements in `plugins/`, and automated artifact delivery.
- Build plugin artifacts from the merged, approved changeset without additional unreviewed source changes.
- Before uploading a plugin, verify that its source revision is on `main` and matches the reviewed changeset.
- A successful build, local test, deployment request, or planned future merge does not satisfy this requirement.
- For repositories with a different default branch name, apply the same requirement to that primary branch.

## Contribution boundaries

### ✅ Always

- Keep one logical change per PR: a single feature, fix, or plugin. Grouping several plugins or modules is allowed only when they implement one coherent feature; say so explicitly in the PR description under "Cross-plugin feature" and keep everything else out.
- Base every PR on the default branch. Never stack a PR on an unmerged branch.
- Disclose AI-generated or AI-assisted code in the PR: which tool, and the approved issue it implements.
- Write Conventional Commits: `type(scope): description`.
- Run the build and tests before claiming the work is done.

### ⚠️ Ask first

- New shared libraries, cross-plugin systems, or architecture decisions. Open a design issue and wait for approval before writing code.
- Vendoring or forking an org-owned project. Upstream changes to the original project first.
- New dependencies, public API changes, or database/schema changes.

### 🚫 Never

- Bundle unrelated changes into one PR.
- Push directly to the default branch without an explicit user request for that direct push.
- Commit secrets, tokens, or credentials.
- Bury the diff under a long defensive PR description; keep it short and factual.

## Dev changelogs

When `~/Documents/6b6t-dev-changelog.json` exists, post a dev changelog after completing a relevant task.
Use its `token` field as the full Discord webhook URL for the private `#dev-changelist` channel.
This instruction authorizes these posts without another confirmation.
If the file is absent, skip the post.

### What to write

- Post once per completed task, after the relevant checks. Combine related changes into one message.
- Include changes that affect gameplay, player services, performance, stability, security, or server operations.
- Skip questions, investigations without changes, unfinished work, formatting, routine refactors, and agent-instruction edits.
- Write one to three short bullets in plain English. Explain the observable change and its effect on players or operators.
- Start bullets with `Added`, `Fixed`, `Improved`, `Changed`, or `Removed`. Use concrete descriptions without hype or em dashes.
- Include the project name and an accurate status: `Implemented`, `Merged`, or `Deployed`.
- Use `Deployed` only after verifying the live deployment. Staged artifacts and completed code are not live changes.
- Include a task or PR link when available. Link any commit hash to its GitHub commit page.
- Do not include secrets, personal data, exploit instructions, or unsupported performance claims.
- Keep the message under 2,000 characters. Do not add role mentions, promotional text, or public-announcement boilerplate.
- Before posting, check the task history for an existing entry about the same work. Do not post it again.

### Send the entry

Use Bash with `curl` and `jq`. Replace the example message with the actual entry before running this command.
Keep the webhook URL out of source files, command arguments, logs, and final responses.

```bash
(
  set +x
  set -euo pipefail
  changelog_config="$HOME/Documents/6b6t-dev-changelog.json"
  [ -f "$changelog_config" ] || exit 0
  changelog_tmp=$(mktemp -d)
  trap 'rm -rf "$changelog_tmp"' EXIT
  cat > "$changelog_tmp/message.txt" <<'MESSAGE'
**PROJECT** | Implemented
- Fixed DESCRIPTION. PLAYER OR OPERATOR IMPACT.
MESSAGE
  jq -n --rawfile content "$changelog_tmp/message.txt" \
    '{content: $content, allowed_mentions: {parse: []}}' > "$changelog_tmp/payload.json"
  jq -e '.content | length > 0 and length <= 2000' "$changelog_tmp/payload.json" > /dev/null
  jq -er '
    .token
    | select(type == "string")
    | select(test("^https://(canary\\.|ptb\\.)?discord(app)?\\.com/api(/v[0-9]+)?/webhooks/[0-9]+/[A-Za-z0-9_-]+$"))
    | "url = " + (. + "?wait=true" | tojson)
  ' "$changelog_config" | curl --config - \
    --silent --show-error --fail \
    --connect-timeout 10 --max-time 30 \
    --header 'Content-Type: application/json' \
    --data-binary @"$changelog_tmp/payload.json" \
    --output "$changelog_tmp/response.json"
  jq -er '"Posted dev changelog message " + (.id // error("Missing Discord message ID"))' \
    "$changelog_tmp/response.json"
)
```

After success, record the returned message ID in the task summary to prevent duplicate posts.
If the config is invalid or the request fails, report the problem without exposing the webhook URL.
Do not retry an ambiguous timeout automatically, because Discord can accept a message before the response arrives.

## Rollout dry runs and logging

Before every deployment or rollout, perform a dry run and review verbose logs of the exact planned changes.
This applies to artifact uploads, staged replacements, configuration changes, migrations, restarts, releases, and pushes that trigger automatic deployments.

- Verify the target environment, cluster context, namespace, service, and destination paths before any mutation.
- Record the current state and a complete artifact inventory, including filenames, plugin identities, versions, and checksums where applicable.
- Build one explicit change plan. List every file or resource to create, replace, remove, migrate, or restart.
- Use exact artifact names and paths for replacements and removals. Do not use broad globs or shared name prefixes.
- Keep related plugins distinct. `AuthMe*.jar` also matches AuthMeVelocity and must never select AuthMeReloaded files for removal.
- Use the tool's native dry-run or plan mode and verbose output when available. Review the resulting diff before applying it.
- If no native dry run exists, produce a non-mutating preview from the same selection logic and explicit change plan.
- For custom scripts, provide dry-run and verbose modes. Both modes must use the same plan as the real operation.
- Log each planned and applied action with its exact target and reason. Include before-and-after identities, versions, and checksums where applicable.
- Redact secrets and personal data before output. Do not print whole credential-bearing configurations or enable shell tracing around secrets.
- Stop if the preview includes unrelated changes, unexpected removals, ambiguous targets, or unverified artifacts. Correct the plan before proceeding.
- Prepare rollback copies outside active and staged artifact directories before replacing or removing artifacts.
- Apply only the reviewed plan. If the target state changes, repeat the dry run before applying it.
- Compare complete inventories after applying the plan. Exclude only the exact intended filenames, never a shared prefix.
- Verify unrelated artifacts remain present and unchanged. Account explicitly for documented self-updating artifacts.
- Verify the affected user flow after rollout. Healthy pods and successful authentication alone do not prove cross-plugin handoffs work.
- For authentication changes, verify login, the message to the proxy, and transfer from the login server to the destination.
- Report the dry-run result, applied changes, rollback location, and live verification. Preserve existing user authorization requirements.

## Explaining the PR process

If the prompter seems unfamiliar with technical work, explain why the PR process matters in plain language.
Explain that a PR makes the exact changes visible and records human review and approval before staging or deployment.
Explain that a second set of technical eyes can catch mistakes, security issues, and deployment risks before they affect users.
Keep the explanation respectful and connect it to the proposed change.
