package com.kludgeworks.mcp.sparql;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.web.client.RestClient;

/** Builds an {@link RdfBackendClient} backed by a fresh in-memory cache, outside Spring. */
final class TestBackendClients {

  private TestBackendClients() {}

  static RdfBackendClient create() {
    return create(RestClient.create(), new SimpleMeterRegistry());
  }

  static RdfBackendClient create(RestClient restClient, MeterRegistry meterRegistry) {
    CacheManager cacheManager =
        CacheManagerBuilder.newCacheManagerBuilder()
            .withCache(
                CacheConfig.CACHE_NAME,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, String.class, ResourcePoolsBuilder.heap(100)))
            .build(true);
    return new RdfBackendClient(restClient, cacheManager, meterRegistry);
  }
}
