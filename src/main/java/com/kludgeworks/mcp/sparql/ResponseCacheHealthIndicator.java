package com.kludgeworks.mcp.sparql;

import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Reports whether the SPARQL response cache is open and whether it persists to disk. */
@Component("responseCache")
class ResponseCacheHealthIndicator implements HealthIndicator {

  private final CacheManager cacheManager;

  ResponseCacheHealthIndicator(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Override
  public Health health() {
    if (cacheManager.getStatus() != Status.AVAILABLE) {
      return Health.down().withDetail("cache", CacheConfig.CACHE_NAME).build();
    }
    boolean persistent = cacheManager instanceof PersistentCacheManager;
    return Health.up()
        .withDetail("cache", CacheConfig.CACHE_NAME)
        .withDetail("storage", persistent ? "persistent" : "heap-only")
        .build();
  }
}
