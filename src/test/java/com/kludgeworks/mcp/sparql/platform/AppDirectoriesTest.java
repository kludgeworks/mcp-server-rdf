package com.kludgeworks.mcp.sparql.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AppDirectoriesTest {

  // Paths are built from the filesystem root so they are absolute on every OS. A path such as
  // "/home/user" has no drive letter, so it is not absolute on Windows.
  private static final Path ROOT = Path.of("").toAbsolutePath().getRoot();
  private static final Path HOME = path("home/user");

  @ParameterizedTest(name = "{0}")
  @MethodSource("platforms")
  void resolvesPlatformDirectories(
      String platform, String osName, Map<String, String> env, Path cache, Path logs) {
    AppDirectories dirs = new AppDirectories(osName, env, HOME);

    assertThat(dirs.cacheDir("app", "author")).isEqualTo(cache);
    assertThat(dirs.logDir("app", "author")).isEqualTo(logs);
  }

  static Stream<Arguments> platforms() {
    return Stream.of(
        Arguments.of(
            "macOS",
            "Mac OS X",
            Map.of(),
            path("home/user/Library/Caches/app"),
            path("home/user/Library/Logs/app")),
        Arguments.of(
            "Linux defaults",
            "Linux",
            Map.of(),
            path("home/user/.cache/app"),
            path("home/user/.local/state/app/logs")),
        Arguments.of(
            "Linux with XDG variables",
            "Linux",
            Map.of(
                "XDG_CACHE_HOME", path("xdg/cache").toString(),
                "XDG_STATE_HOME", path("xdg/state").toString()),
            path("xdg/cache/app"),
            path("xdg/state/app/logs")),
        Arguments.of(
            "Linux ignores relative XDG variables",
            "Linux",
            Map.of("XDG_CACHE_HOME", "relative/cache", "XDG_STATE_HOME", "relative/state"),
            path("home/user/.cache/app"),
            path("home/user/.local/state/app/logs")),
        Arguments.of(
            "Windows with LOCALAPPDATA",
            "Windows 11",
            Map.of("LOCALAPPDATA", path("appdata/local").toString()),
            path("appdata/local/author/app/Cache"),
            path("appdata/local/author/app/Logs")),
        Arguments.of(
            "Windows without LOCALAPPDATA",
            "Windows 11",
            Map.of(),
            path("home/user/AppData/Local/author/app/Cache"),
            path("home/user/AppData/Local/author/app/Logs")));
  }

  private static Path path(String relativeToRoot) {
    return ROOT.resolve(relativeToRoot);
  }
}
