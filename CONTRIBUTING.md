# Contributing to Nexora

Thanks for contributing. This project is a small, focused ORM for Bukkit/Spigot, so contributions should stay minimal, clear, and well-scoped.

## How to Contribute
1. Fork the repo and create a feature branch.
2. Keep changes focused. Avoid bundling unrelated refactors with functional changes.
3. Update docs when you change behavior.
4. Add tests when behavior changes and a test is feasible.

## Development
- Java 17 is required.
- Build with `./gradlew build`.

## Code Style
- Prefer small, readable methods.
- Avoid reflection outside the existing metadata and mapping system.
- Avoid blocking calls on Bukkit main thread.

## Submitting
- Open a pull request with a clear description of what changed and why.
- Mention any known limitations or follow-up tasks.
