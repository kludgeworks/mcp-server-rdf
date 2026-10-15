# sparql-mcp

Spring Boot MCP server that executes SPARQL queries against RDF backends.

## Install

### Homebrew (macOS and Linux)

```bash
brew tap kludgeworks/tap
brew trust kludgeworks/tap
brew install sparql-mcp
```

Upgrade with `brew upgrade sparql-mcp`.

### Other options

- Native binaries for Linux, macOS and Windows, and the JVM jar, are attached to each
  [GitHub release](https://github.com/kludgeworks/mcp-server-rdf/releases).
- Container image: `ghcr.io/kludgeworks/sparql-mcp:<version>` (see [Container](#container)).

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

## Running Modes

The same jar, native binary, and container image can run in either mode. Choose it at startup with
`-t`/`--transport=stdio|http` (or the environment variable `SPARQL_MCP_TRANSPORT`). Run
`sparql-mcp --help` for all options; any other `--<property>=<value>` argument is passed to Spring
Boot.

| Mode | How to start | Use it for |
| --- | --- | --- |
| `stdio` (default) | `sparql-mcp` | MCP clients that launch the server as a subprocess |
| `http` | `sparql-mcp --transport=http` | A long-running service, using stateless Streamable HTTP at `/mcp` |

In `http` mode the server also exposes:

- `GET /health`, `/health/liveness`, `/health/readiness`: probes (`200` when UP, `503` otherwise)
- `GET /metrics`: Prometheus scrape (tool calls, SPARQL backend requests, cache hits and misses, JVM)

### Container

```bash
docker run --rm -p 127.0.0.1:8080:8080 \
  -e SPARQL_MCP_TRANSPORT=http \
  -e SPARQL_MCP_HTTP_ADDRESS=0.0.0.0 \
  -e SPARQL_MCP_CACHE_DIRECTORY=/cache -v sparql-mcp-cache:/cache \
  ghcr.io/kludgeworks/sparql-mcp:<version>
```

If other containers reach the server by service name, add that name to
`SPARQL_MCP_HTTP_ALLOWED_HOSTS` (for example `localhost:*,sparql-mcp:*`).

### Cache

Query responses are cached for 24 hours in a persistent Ehcache store. Ehcache locks its directory,
so when a second process starts on the same directory (for example, two MCP clients each launching
the server), that process falls back to an in-memory cache and logs a warning.

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
- `build-native-image` -> `mise run build --profile native spring-boot:build-image`
- `build-native-test` -> `mise run build --profile nativeTest test`
- `release` -> `mise run release`

## Versioning

This repository uses `maven-git-versioning-extension` (`.mvn/extensions.xml`).

- `pom.xml` keeps a base version (`0.0.0-SNAPSHOT`)
- effective build version is derived from git refs/tags
- release tags follow `v<semver>` (for example `v0.1.0`)

## Release

Releases are currently triggered by:

```bash
mise release --push
```

Artifacts and publication are then handled by GitHub Actions + JReleaser:

- workflow: `.github/workflows/release.yml`
- platform builds: `.github/workflows/reusable-assemble.yml`
- distribution model: `BINARY` in `jreleaser.yml`

Artifacts are produced per platform with unique names, then attached to the GitHub release.
The assemble workflow publishes Linux container images to temporary registry tags and records their digests.
The final release job promotes those digests to:

- `ghcr.io/kludgeworks/sparql-mcp:<version>` (Linux multi-arch manifest for amd64 and arm64)
- `ghcr.io/kludgeworks/sparql-mcp:<version>-linux-amd64`
- `ghcr.io/kludgeworks/sparql-mcp:<version>-linux-arm64`

## Notes

- Default transport is MCP over STDIO (see [Running Modes](#running-modes)).
- Runtime logs are written to STDERR and a log file, never STDOUT, so they cannot corrupt the stdio protocol.
