# sparql-mcp

Spring Boot MCP server that executes SPARQL queries against RDF backends.

## Requirements

- [mise](https://mise.jdx.dev/)

## Quick Start

```bash
mise trust
mise install
run
```

> [!TIP]
> For task/flag autocompletion, enable `mise` shell completion and install `usage` (docs: https://mise.jdx.dev/cli/completion.html and https://usage.jdx.dev/cli/completions).

## Tasks

List all available tasks with:

```bash
mise tasks
```

Examples:

```bash
mise build --clean --profile native native:compile
mise build --clean --profile native --native-image-name sparql-mcp-linux-x86_64 native:compile
```

## Shell Aliases

After activating mise in your shell you can use these aliases:

- `build` -> `mise run build`
- `run` -> `mise run build spring-boot:run`
- `build-test` -> `mise run build test`
- `build-package` -> `mise run build package`
- `build-native` -> `mise run build --profile native native:compile`
- `build-native-test` -> `mise run build --profile nativeTest test`
- `release-tag` -> `mise run release:tag`

## Versioning

This repository uses `maven-git-versioning-extension` (`.mvn/extensions.xml`).

- `pom.xml` keeps a base version (`0.0.0-SNAPSHOT`)
- effective build version is derived from git refs/tags
- release tags follow `v<semver>` (for example `v0.1.0`)

## Release

Releases are currently triggered by:

```bash
mise tag --push
```

Artifacts and publication are then handled by GitHub Actions + JReleaser:

- workflow: `.github/workflows/release.yml`
- platform builds: `.github/workflows/reusable-assemble.yml`
- distribution model: `BINARY` in `jreleaser.yml`

Artifacts are produced per platform with unique names, then attached to the GitHub release.

> [!IMPORTANT]
> The `workflow_dispatch` entry point currently fails for our release process. We need artifact signing as part of the workflow, and for now the working path is via the CLI (`mise tag --push`).

## Notes

- Default transport is MCP over STDIO (see `application.yml`).
- Runtime logs should not be written to STDOUT when used as an MCP stdio server.
