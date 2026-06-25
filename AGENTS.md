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

Rivet is consumed through `com.github.Lenni0451.rivet:core:40c6f6c3a7`, with upstream source at `https://github.com/Lenni0451/rivet`. Prefer project-owned bridge changes in `net.blockhost.anarchyclient.rivet` over vendoring or rewriting Rivet internals.

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
