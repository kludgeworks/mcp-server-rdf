package com.kludgeworks.mcp.sparql.query.cache;

import com.kludgeworks.mcp.sparql.platform.AppDirectories;
import com.kludgeworks.mcp.sparql.platform.AppMetadata;
import com.kludgeworks.mcp.sparql.query.QueryProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(CaffeineHints.class)
class ResponseCacheConfig {

  private static final Logger log = LoggerFactory.getLogger(ResponseCacheConfig.class);

  private static final int MEMORY_ENTRIES = 1000;
  private static final int STORE_ENTRIES = 10_000;
  private static final Duration TTL = Duration.ofHours(24);

  @Bean
  TwoLevelResponseCache sparqlResponseCache(
      AppMetadata appMetadata,
      QueryProperties properties,
      MeterRegistry meterRegistry,
      ApplicationEventPublisher events) {
    Clock clock = Clock.systemUTC();
    return new TwoLevelResponseCache(
        openStore(resolveDirectory(appMetadata, properties), clock),
        MEMORY_ENTRIES,
        clock,
        TTL,
        meterRegistry,
        events);
  }

  /** The cache keeps working in memory when the store cannot be opened. */
  private static @Nullable SqliteResponseStore openStore(Path directory, Clock clock) {
    try {
      return SqliteResponseStore.open(directory, clock, TTL, STORE_ENTRIES);
    } catch (IOException | SQLException e) {
      log.warn(
          "Could not open the response cache in {} ({}); caching in memory only",
          directory,
          e.getMessage());
      return null;
    }
  }

  /**
   * Uses {@code sparql-mcp.cache.directory} when set, otherwise the platform cache directory. Every
   * process on the machine can share it, whichever transport it runs.
   */
  private static Path resolveDirectory(AppMetadata appMetadata, QueryProperties properties) {
    String configured = properties.cache().directory();
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured);
    }
    return AppDirectories.current().cacheDir(appMetadata.name(), appMetadata.author());
  }
}
