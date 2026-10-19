package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ResponseStorePurgerTest {

  private static final Duration TTL = Duration.ofHours(24);

  // Under target/ rather than a JUnit @TempDir, matching the other cache tests; `mvn clean`
  // removes it.
  private final Path directory =
      Path.of("target", "test-tmp", "response-purger-" + UUID.randomUUID());
  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

  @Test
  void purgesExpiredResponsesWhenPurgeIsDue() throws Exception {
    SqliteResponseStore store = SqliteResponseStore.open(directory, clock, TTL, 100);
    store.put("old", "body");
    clock.advance(TTL.plusMinutes(1));
    store.put("new", "body");

    purger(store).on(new ResponseStorePurgeDue());

    assertThat(rowCount(store)).isEqualTo(1);
  }

  @Test
  void ignoresPurgeDueWithoutStore() {
    purger(null).on(new ResponseStorePurgeDue());
  }

  private ResponseStorePurger purger(@Nullable SqliteResponseStore store) {
    return new ResponseStorePurger(
        new TwoLevelResponseCache(store, 10, clock, TTL, new SimpleMeterRegistry(), event -> {}));
  }

  private static int rowCount(SqliteResponseStore store) throws Exception {
    try (var connection =
            java.sql.DriverManager.getConnection("jdbc:sqlite:" + store.file().toAbsolutePath());
        var statement = connection.createStatement();
        var result = statement.executeQuery("SELECT count(*) FROM responses")) {
      result.next();
      return result.getInt(1);
    }
  }
}
