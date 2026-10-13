package com.kludgeworks.mcp.sparql;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Objects;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class RdfBackendClient {

  static final String CACHE_REQUESTS_METRIC = "sparql.cache.requests";

  private static final MediaType SPARQL_QUERY_MEDIA_TYPE =
      MediaType.parseMediaType("application/sparql-query");

  private final RestClient restClient;
  private final Cache<String, String> responseCache;
  private final Counter cacheHits;
  private final Counter cacheMisses;

  /**
   * The cache is used directly rather than through {@code @Cacheable} so hits and misses can be
   * counted in Micrometer; JMX-based cache statistics are not available in the native image.
   */
  RdfBackendClient(RestClient restClient, CacheManager cacheManager, MeterRegistry meterRegistry) {
    this.restClient = restClient;
    this.responseCache =
        Objects.requireNonNull(
            cacheManager.getCache(CacheConfig.CACHE_NAME, String.class, String.class),
            "Cache not configured: " + CacheConfig.CACHE_NAME);
    this.cacheHits = cacheRequests(meterRegistry, "hit");
    this.cacheMisses = cacheRequests(meterRegistry, "miss");
  }

  public String executeQuery(String serviceUrl, String query, MediaType acceptType)
      throws IOException {
    // Same key format the previous @Cacheable used, so persisted entries stay valid.
    String key = serviceUrl + "||" + acceptType + "||" + query;
    String cached = responseCache.get(key);
    if (cached != null) {
      cacheHits.increment();
      return cached;
    }
    cacheMisses.increment();

    String responseBody =
        Objects.requireNonNull(
            restClient
                .post()
                .uri(serviceUrl)
                .contentType(SPARQL_QUERY_MEDIA_TYPE)
                .accept(acceptType)
                .body(query)
                .retrieve()
                .body(String.class),
            "RDF backend returned an empty response");

    if (responseBody.isBlank()) {
      throw new IOException("RDF backend returned an empty response");
    }

    responseCache.put(key, responseBody);
    return responseBody;
  }

  private static Counter cacheRequests(MeterRegistry meterRegistry, String result) {
    return Counter.builder(CACHE_REQUESTS_METRIC)
        .description("SPARQL response cache lookups")
        .tag("cache", CacheConfig.CACHE_NAME)
        .tag("result", result)
        .register(meterRegistry);
  }
}
