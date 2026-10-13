package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AppDirectoriesTest {

  private static final Path HOME = Path.of("/home/user");

  @ParameterizedTest(name = "{0}")
  @MethodSource("platforms")
  void resolvesPlatformDirectories(
      String platform, String osName, Map<String, String> env, String cache, String logs) {
    AppDirectories dirs = new AppDirectories(osName, env, HOME);

    assertThat(dirs.cacheDir("app", "author")).isEqualTo(Path.of(cache));
    assertThat(dirs.logDir("app", "author")).isEqualTo(Path.of(logs));
  }

  static Stream<Arguments> platforms() {
    return Stream.of(
        Arguments.of(
            "macOS",
            "Mac OS X",
            Map.of(),
            "/home/user/Library/Caches/app",
            "/home/user/Library/Logs/app"),
        Arguments.of(
            "Linux defaults",
            "Linux",
            Map.of(),
            "/home/user/.cache/app",
            "/home/user/.local/state/app/logs"),
        Arguments.of(
            "Linux with XDG variables",
            "Linux",
            Map.of("XDG_CACHE_HOME", "/xdg/cache", "XDG_STATE_HOME", "/xdg/state"),
            "/xdg/cache/app",
            "/xdg/state/app/logs"),
        Arguments.of(
            "Linux ignores relative XDG variables",
            "Linux",
            Map.of("XDG_CACHE_HOME", "relative/cache", "XDG_STATE_HOME", "relative/state"),
            "/home/user/.cache/app",
            "/home/user/.local/state/app/logs"),
        Arguments.of(
            "Windows with LOCALAPPDATA",
            "Windows 11",
            Map.of("LOCALAPPDATA", "/appdata/local"),
            "/appdata/local/author/app/Cache",
            "/appdata/local/author/app/Logs"),
        Arguments.of(
            "Windows without LOCALAPPDATA",
            "Windows 11",
            Map.of(),
            "/home/user/AppData/Local/author/app/Cache",
            "/home/user/AppData/Local/author/app/Logs"));
  }
}
