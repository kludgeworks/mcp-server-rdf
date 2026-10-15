package com.kludgeworks.mcp.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(Application.BuildInfoHints.class)
public class Application {

  private static final String BUILD_INFO = "META-INF/build-info.properties";

  static void main(String[] args) {
    // Handled before Spring starts, so it is instant and never opens the stdio transport.
    if (Arrays.asList(args).contains("--version")) {
      System.out.println("sparql-mcp " + version());
      return;
    }
    SpringApplication.run(Application.class, args);
  }

  /** The project version from the build-info file the Spring Boot plugin generates. */
  static String version() {
    Properties buildInfo = new Properties();
    try (InputStream stream = Application.class.getClassLoader().getResourceAsStream(BUILD_INFO)) {
      if (stream != null) {
        buildInfo.load(stream);
      }
    } catch (IOException e) {
      // Fall through to "unknown".
    }
    return buildInfo.getProperty("build.version", "unknown");
  }

  /** Includes the build-info file in the native image so {@code --version} works there too. */
  static class BuildInfoHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.resources().registerPattern(BUILD_INFO);
    }
  }
}
