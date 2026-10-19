package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TwoLevelResponseCacheTest {

  private static final Duration TTL = Duration.ofHours(24);
  private static final SparqlRequest REQUEST =
      new SparqlRequest(
          "https://sparql.example/query",
          "SELECT * {}",
          MediaType.parseMediaType("application/sparql-results+json"));

  // Under target/ rather than a JUnit @TempDir, matching the other cache tests; `mvn clean`
  // removes it.
  private final Path directory =
      Path.of("target", "test-tmp", "response-cache-" + UUID.randomUUID());
  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
  private final AtomicInteger loads = new AtomicInteger();
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Test
  void servesRepeatedRequestsFromMemory() throws Exception {
    TwoLevelResponseCache cache = cache(store());

    assertThat(cache.get(REQUEST, this::load)).isEqualTo("response 1");
    assertThat(cache.get(REQUEST, this::load)).isEqualTo("response 1");

    assertThat(loads).hasValue(1);
    assertThat(meterRegistry.get("cache.gets").tag("result", "hit").functionCounter().count())
        .isEqualTo(1.0);
  }

  @Test
  void survivesRestartAndIsSharedBetweenProcesses() throws Exception {
    cache(store()).get(REQUEST, this::load);

    // A second instance on the same directory stands in for a restarted or concurrent process.
    TwoLevelResponseCache other = cache(store());

    assertThat(other.get(REQUEST, this::load)).isEqualTo("response 1");
    assertThat(loads).hasValue(1);
    assertThat(
            meterRegistry
                .get(TwoLevelResponseCache.STORE_REQUESTS_METRIC)
                .tag("result", "hit")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void reloadsExpiredResponses() throws Exception {
    cache(store()).get(REQUEST, this::load);
    clock.advance(TTL.plusMinutes(1));

    assertThat(cache(store()).get(REQUEST, this::load)).isEqualTo("response 2");
  }

  @Test
  void cachesInMemoryWithoutStore() throws Exception {
    TwoLevelResponseCache cache = cache(null);

    cache.get(REQUEST, this::load);
    cache.get(REQUEST, this::load);

    assertThat(loads).hasValue(1);
  }

  @Test
  void propagatesLoaderFailureWithoutCachingIt() throws Exception {
    TwoLevelResponseCache cache = cache(store());

    assertThatThrownBy(
            () ->
                cache.get(
                    REQUEST,
                    () -> {
                      throw new IOException("backend down");
                    }))
        .isInstanceOf(IOException.class)
        .hasMessage("backend down");
    assertThat(cache.get(REQUEST, this::load)).isEqualTo("response 1");
  }

  private String load() {
    return "response " + loads.incrementAndGet();
  }

  private SqliteResponseStore store() throws Exception {
    return SqliteResponseStore.open(directory, clock, TTL, 100);
  }

  private TwoLevelResponseCache cache(@Nullable SqliteResponseStore store) {
    return new TwoLevelResponseCache(store, 10, clock, TTL, meterRegistry);
  }
}
