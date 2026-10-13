package com.kludgeworks.mcp.sparql;

import java.io.File;
import java.time.Duration;
import java.util.Locale;
import org.ehcache.CacheManager;
import org.ehcache.StateTransitionException;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SPARQL response cache. It persists to disk because stdio clients start a new process per session,
 * so an in-memory cache would rarely be hit.
 */
@Configuration
public class CacheConfig {

  private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

  static final String CACHE_NAME = "rdfBackendResponses";
  private static final int HEAP_ENTRIES = 1000;
  private static final int DISK_MB = 200;
  private static final Duration TTL = Duration.ofHours(24);

  @Bean(destroyMethod = "close")
  public CacheManager responseCacheManager(
      AppMetadata appMetadata, SparqlMcpProperties properties) {
    File directory = resolveDirectory(appMetadata, properties);
    ResourcePoolsBuilder heap =
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(HEAP_ENTRIES, EntryUnit.ENTRIES);
    try {
      return CacheManagerBuilder.newCacheManagerBuilder()
          .with(CacheManagerBuilder.persistence(directory))
          .withCache(CACHE_NAME, cacheConfiguration(heap.disk(DISK_MB, MemoryUnit.MB, true)))
          .build(true);
    } catch (StateTransitionException e) {
      // Ehcache locks its persistence directory, so a second process pointed at the same
      // directory cannot open it. Keep serving with an in-memory cache instead of failing.
      log.warn(
          "Could not open persistent cache at {} ({}); falling back to an in-memory cache",
          directory,
          e.getMessage());
      return CacheManagerBuilder.newCacheManagerBuilder()
          .withCache(CACHE_NAME, cacheConfiguration(heap))
          .build(true);
    }
  }

  private static CacheConfiguration<String, String> cacheConfiguration(
      ResourcePoolsBuilder resourcePools) {
    return CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class, String.class, resourcePools)
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(TTL))
        .build();
  }

  /**
   * Uses {@code sparql-mcp.cache.directory} when set. Otherwise each transport gets its own
   * directory under the platform cache directory, so a long-running HTTP service and stdio clients
   * on the same machine do not contend for the same Ehcache lock.
   */
  private static File resolveDirectory(AppMetadata appMetadata, SparqlMcpProperties properties) {
    String configured = properties.cache().directory();
    if (configured != null && !configured.isBlank()) {
      return new File(configured);
    }
    return AppDirectories.current()
        .cacheDir(appMetadata.name(), appMetadata.author())
        .resolve("ehcache-" + properties.transport().name().toLowerCase(Locale.ROOT))
        .toFile();
  }
}
