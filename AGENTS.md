# Repository Guidelines

## Project Structure & Module Organization

AnarchyClient is a Fabric utility mod for Minecraft `26.1.2`. Main Java code lives in `src/main/java`. Project-owned classes are under `net.blockhost.anarchyclient`, with modules in `module/impl`, settings in `setting`, config in `config`, UI in `ui`, and Minecraft/Rivet bridge code in `rivet`. Vendored Rivet sources live under `src/main/java/net/lenni0451/rivet`; keep upstream licensing notes in `third_party/rivet`. Fabric metadata and assets live in `src/main/resources`, including `fabric.mod.json` and `assets/anarchyclient/lang/en_us.json`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper:

```bash
./gradlew build
```

Compiles the mod, processes resources, runs tests, and produces build artifacts.

```bash
./gradlew test
```

Runs the JUnit test suite.

```bash
./gradlew runClient
```

Starts the Fabric development client using the `run/` directory. The in-game client menu opens with `Right Shift`.

## Coding Style & Naming Conventions

Target Java `25`; Gradle configures UTF-8 and `--release 25`. Use idiomatic Java naming: classes and records in `PascalCase`, methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`. Keep packages lowercase and rooted under `net.blockhost.anarchyclient` for project code. Prefer small module classes that extend the existing module and setting abstractions instead of adding duplicate control flow. Treat vendored Rivet code as third-party code unless a change is required for integration.

## Testing Guidelines

JUnit Jupiter is configured through Gradle. Put tests in `src/test/java` with matching package names, and name test classes after the unit under test, such as `ModuleManagerTest`. Add targeted tests when changing module registration, config serialization, settings behavior, or input/render bridge logic. No strict coverage gate is configured, but new behavior should have practical regression coverage.

## Commit & Pull Request Guidelines

Use Conventional Commit messages: `<type>(<scope>): <subject>`, for example `feat(module): add auto totem settings` or `fix(config): persist disabled modules`. Keep subjects imperative and concise. Do not start new branches unless explicitly requested.

Pull requests should describe the user-visible change, list verification commands run, and link any related issue. Include screenshots or short clips for UI changes when practical. Do not bypass commit hooks; let hooks finish normally.

