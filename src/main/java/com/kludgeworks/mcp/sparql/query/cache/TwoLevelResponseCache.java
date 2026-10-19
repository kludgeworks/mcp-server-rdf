package com.kludgeworks.mcp.sparql.query.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.kludgeworks.mcp.sparql.query.cache.SqliteResponseStore.StoredResponse;
import com.kludgeworks.mcp.sparql.query.port.ResponseCache;
import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caffeine in memory in front of a SQLite store on disk. A memory miss falls back to the store, and
 * a store miss calls the loader and writes the response to both.
 *
 * <p>Without a store, for example when the cache directory cannot be opened, responses are only
 * cached in memory.
 */
final class TwoLevelResponseCache implements ResponseCache {

  static final String CACHE_NAME = "rdfBackendResponses";
  static final String STORE_REQUESTS_METRIC = "sparql.cache.store.requests";

  private static final Logger log = LoggerFactory.getLogger(TwoLevelResponseCache.class);

  private final Cache<String, StoredResponse> memory;
  private final @Nullable SqliteResponseStore store;
  private final Clock clock;
  private final Duration ttl;
  private final Counter storeHits;
  private final Counter storeMisses;

  TwoLevelResponseCache(
      @Nullable SqliteResponseStore store,
      int memoryEntries,
      Clock clock,
      Duration ttl,
      MeterRegistry meterRegistry) {
    this.store = store;
    this.clock = clock;
    this.ttl = ttl;
    // Entries loaded from the store keep their original expiry instead of starting a new TTL.
    this.memory =
        CaffeineCacheMetrics.monitor(
            meterRegistry,
            Caffeine.newBuilder()
                .maximumSize(memoryEntries)
                .expireAfter(new UntilExpiresAt(clock))
                .recordStats()
                .<String, StoredResponse>build(),
            CACHE_NAME);
    this.storeHits = storeRequests(meterRegistry, "hit");
    this.storeMisses = storeRequests(meterRegistry, "miss");
  }

  @Override
  public String get(SparqlRequest request, Loader loader) throws IOException {
    try {
      return memory.get(key(request), key -> load(key, loader)).body();
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /** The on-disk store, or null when responses are only cached in memory. */
  @Nullable SqliteResponseStore store() {
    return store;
  }

  private StoredResponse load(String key, Loader loader) {
    StoredResponse stored = readStore(key);
    if (stored != null) {
      return stored;
    }
    String body;
    try {
      body = loader.load();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    writeStore(key, body);
    return new StoredResponse(body, clock.instant().plus(ttl));
  }

  private @Nullable StoredResponse readStore(String key) {
    if (store == null) {
      return null;
    }
    try {
      StoredResponse stored = store.get(key);
      (stored != null ? storeHits : storeMisses).increment();
      return stored;
    } catch (SQLException e) {
      log.warn("Could not read the response cache at {}: {}", store.file(), e.getMessage());
      return null;
    }
  }

  private void writeStore(String key, String body) {
    if (store == null) {
      return;
    }
    try {
      store.put(key, body);
    } catch (SQLException e) {
      log.warn("Could not write the response cache at {}: {}", store.file(), e.getMessage());
    }
  }

  /** A fixed-length key, so long queries do not bloat the store's index. */
  private static String key(SparqlRequest request) {
    String raw = request.serviceUrl() + '\0' + request.acceptType() + '\0' + request.query();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static Counter storeRequests(MeterRegistry meterRegistry, String result) {
    return Counter.builder(STORE_REQUESTS_METRIC)
        .description("SPARQL response lookups in the on-disk store")
        .tag("cache", CACHE_NAME)
        .tag("result", result)
        .register(meterRegistry);
  }

  private record UntilExpiresAt(Clock clock) implements Expiry<String, StoredResponse> {

    @Override
    public long expireAfterCreate(String key, StoredResponse value, long currentTime) {
      return Math.max(0, Duration.between(clock.instant(), value.expiresAt()).toNanos());
    }

    @Override
    public long expireAfterUpdate(
        String key, StoredResponse value, long currentTime, long currentDuration) {
      return expireAfterCreate(key, value, currentTime);
    }

    @Override
    public long expireAfterRead(
        String key, StoredResponse value, long currentTime, long currentDuration) {
      return currentDuration;
    }
  }
}
