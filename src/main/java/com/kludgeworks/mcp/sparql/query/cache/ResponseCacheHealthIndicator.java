package com.kludgeworks.mcp.sparql.query.cache;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports where SPARQL responses are cached. Always UP: queries still work, just slower, when the
 * on-disk store is unavailable, so the cache must never take the service out of rotation.
 */
@Component("responseCache")
class ResponseCacheHealthIndicator implements HealthIndicator {

  private final TwoLevelResponseCache cache;

  ResponseCacheHealthIndicator(TwoLevelResponseCache cache) {
    this.cache = cache;
  }

  @Override
  public Health health() {
    SqliteResponseStore store = cache.store();
    Health.Builder health = Health.up().withDetail("cache", TwoLevelResponseCache.CACHE_NAME);
    if (store == null) {
      return health.withDetail("storage", "memory").build();
    }
    return health
        .withDetail("storage", store.isAvailable() ? "persistent" : "unavailable")
        .withDetail("file", store.file().toString())
        .build();
  }
}
