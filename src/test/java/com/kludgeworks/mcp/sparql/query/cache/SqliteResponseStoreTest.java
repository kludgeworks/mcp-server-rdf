package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SqliteResponseStoreTest {

  private static final Duration TTL = Duration.ofHours(24);

  // Under target/ rather than a JUnit @TempDir, matching the other cache tests; `mvn clean`
  // removes it.
  private final Path directory =
      Path.of("target", "test-tmp", "response-store-" + UUID.randomUUID());
  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

  @Test
  void purgesOldestEntriesBeyondCapacity() throws Exception {
    SqliteResponseStore store = SqliteResponseStore.open(directory, clock, TTL, 2);
    for (String key : new String[] {"a", "b", "c"}) {
      store.put(key, key);
      clock.advance(Duration.ofSeconds(1));
    }

    store.purge();

    assertThat(store.get("a")).isNull();
    assertThat(store.get("c")).isNotNull();
  }

  @Test
  void reportsPurgeDueEveryPurgeInterval() throws Exception {
    SqliteResponseStore store = SqliteResponseStore.open(directory, clock, TTL, 1000);

    for (int i = 1; i < SqliteResponseStore.PURGE_INTERVAL; i++) {
      assertThat(store.put("key-" + i, "body")).isFalse();
    }
    assertThat(store.put("last", "body")).isTrue();
    assertThat(store.put("next", "body")).isFalse();
  }

  @Test
  void enablesAutoVacuumOnNewDatabase() throws Exception {
    SqliteResponseStore.open(directory, clock, TTL, 100);

    assertThat(pragma("auto_vacuum")).isEqualTo(SqliteResponseStore.AUTO_VACUUM_FULL);
  }

  @Test
  void convertsAnExistingDatabaseToAutoVacuum() throws Exception {
    // A database created before auto_vacuum was enabled.
    java.nio.file.Files.createDirectories(directory);
    try (Statement statement = connect().createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE responses ("
              + "key TEXT PRIMARY KEY, body TEXT NOT NULL, created_at INTEGER NOT NULL)");
    }
    assertThat(pragma("auto_vacuum")).isZero();

    SqliteResponseStore.open(directory, clock, TTL, 100);

    assertThat(pragma("auto_vacuum")).isEqualTo(SqliteResponseStore.AUTO_VACUUM_FULL);
  }

  private int pragma(String name) throws Exception {
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("PRAGMA " + name)) {
      return result.next() ? result.getInt(1) : -1;
    }
  }

  private Connection connect() throws Exception {
    return DriverManager.getConnection(
        "jdbc:sqlite:" + directory.resolve(SqliteResponseStore.FILE_NAME).toAbsolutePath());
  }
}
