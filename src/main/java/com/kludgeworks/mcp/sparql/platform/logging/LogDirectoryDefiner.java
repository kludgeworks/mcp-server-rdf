package com.kludgeworks.mcp.sparql.platform.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.kludgeworks.mcp.sparql.platform.AppDirectories;
import java.io.File;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the log directory for {@code logback-spring.xml}: Spring Boot's {@code
 * logging.file.path} when set, otherwise the platform's per-user log directory.
 */
public class LogDirectoryDefiner extends PropertyDefinerBase {

  private @Nullable String appName;
  private @Nullable String appAuthor;
  private @Nullable String override;

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setAppAuthor(String appAuthor) {
    this.appAuthor = appAuthor;
  }

  /** Spring Boot's {@code logging.file.path}, exposed to logback as {@code LOG_PATH}. */
  public void setOverride(String override) {
    this.override = override;
  }

  @Override
  public String getPropertyValue() {
    String configured = override;
    File logDir =
        configured == null || configured.isBlank()
            ? AppDirectories.current()
                .logDir(
                    valueOrDefault(appName, "mcp-sparql"), valueOrDefault(appAuthor, "kludgeworks"))
                .toFile()
            : new File(configured);
    if (!logDir.exists() && !logDir.mkdirs()) {
      throw new IllegalStateException(
          "Could not create log directory: " + logDir.getAbsolutePath());
    }
    return logDir.getAbsolutePath();
  }

  private static String valueOrDefault(@Nullable String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
