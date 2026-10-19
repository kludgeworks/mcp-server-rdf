package com.kludgeworks.mcp.sparql.platform;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Per-user cache and log directories, following each platform's conventions.
 *
 * <ul>
 *   <li>macOS: {@code ~/Library/Caches/<app>} and {@code ~/Library/Logs/<app>}
 *   <li>Windows: {@code %LOCALAPPDATA%\<author>\<app>\Cache} and {@code ...\Logs}
 *   <li>Linux and others: {@code $XDG_CACHE_HOME/<app>} and {@code $XDG_STATE_HOME/<app>/logs},
 *       defaulting to {@code ~/.cache} and {@code ~/.local/state}
 * </ul>
 *
 * <p>Windows folders come from {@code %LOCALAPPDATA%} rather than the Win32 known-folder API, which
 * avoids a native-access dependency in the native image. Both locations can be overridden through
 * configuration.
 *
 * @param osName value of the {@code os.name} system property
 * @param environment process environment variables
 * @param home the user's home directory
 */
public record AppDirectories(String osName, Map<String, String> environment, Path home) {

  public static AppDirectories current() {
    return new AppDirectories(
        System.getProperty("os.name", ""),
        System.getenv(),
        Path.of(System.getProperty("user.home")));
  }

  public Path cacheDir(String appName, String appAuthor) {
    return switch (platform()) {
      case MAC -> home.resolve("Library/Caches").resolve(appName);
      case WINDOWS -> localAppData().resolve(appAuthor).resolve(appName).resolve("Cache");
      case UNIX -> xdg("XDG_CACHE_HOME", ".cache").resolve(appName);
    };
  }

  public Path logDir(String appName, String appAuthor) {
    return switch (platform()) {
      case MAC -> home.resolve("Library/Logs").resolve(appName);
      case WINDOWS -> localAppData().resolve(appAuthor).resolve(appName).resolve("Logs");
      case UNIX -> xdg("XDG_STATE_HOME", ".local/state").resolve(appName).resolve("logs");
    };
  }

  private Platform platform() {
    String os = osName.toLowerCase(Locale.ROOT);
    if (os.startsWith("mac") || os.startsWith("darwin")) {
      return Platform.MAC;
    }
    return os.startsWith("windows") ? Platform.WINDOWS : Platform.UNIX;
  }

  private Path localAppData() {
    String configured = environment.get("LOCALAPPDATA");
    return configured == null || configured.isBlank()
        ? home.resolve("AppData").resolve("Local")
        : Path.of(configured);
  }

  /** The XDG spec says relative values must be ignored. */
  private Path xdg(String variable, String defaultUnderHome) {
    Path configured = absolutePath(environment.get(variable));
    return configured != null ? configured : home.resolve(defaultUnderHome);
  }

  private static @Nullable Path absolutePath(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    Path path = Path.of(value);
    return path.isAbsolute() ? path : null;
  }

  private enum Platform {
    MAC,
    WINDOWS,
    UNIX
  }
}
