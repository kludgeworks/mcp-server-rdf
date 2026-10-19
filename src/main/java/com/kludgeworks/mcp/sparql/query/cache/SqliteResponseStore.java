package com.kludgeworks.mcp.sparql.query.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * Responses persisted in a SQLite database, so they survive restarts. Stdio clients start a new
 * process per session, so without this the cache would rarely be hit.
 *
 * <p>The database runs in WAL mode, so several processes can share it: two MCP clients each
 * launching the server, or stdio clients next to a long-running HTTP service.
 */
final class SqliteResponseStore {

  static final String FILE_NAME = "responses.db";

  /** Expired and surplus rows are removed at startup and after this many writes. */
  private static final int PURGE_INTERVAL = 100;

  private static final int BUSY_TIMEOUT_MILLIS = 5000;

  private final Path file;
  private final SQLiteDataSource dataSource;
  private final Clock clock;
  private final Duration ttl;
  private final int maxEntries;
  private final AtomicInteger writesSincePurge = new AtomicInteger();

  private SqliteResponseStore(
      Path file, SQLiteDataSource dataSource, Clock clock, Duration ttl, int maxEntries) {
    this.file = file;
    this.dataSource = dataSource;
    this.clock = clock;
    this.ttl = ttl;
    this.maxEntries = maxEntries;
  }

  static SqliteResponseStore open(Path directory, Clock clock, Duration ttl, int maxEntries)
      throws IOException, SQLException {
    Files.createDirectories(directory);
    Path file = directory.resolve(FILE_NAME);
    SQLiteConfig config = new SQLiteConfig();
    config.setJournalMode(SQLiteConfig.JournalMode.WAL);
    config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
    config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
    SQLiteDataSource dataSource = new SQLiteDataSource(config);
    dataSource.setUrl("jdbc:sqlite:" + file.toAbsolutePath());

    SqliteResponseStore store = new SqliteResponseStore(file, dataSource, clock, ttl, maxEntries);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS responses ("
              + "key TEXT PRIMARY KEY, body TEXT NOT NULL, created_at INTEGER NOT NULL)");
      statement.executeUpdate(
          "CREATE INDEX IF NOT EXISTS responses_created_at ON responses (created_at)");
    }
    store.purge();
    return store;
  }

  Path file() {
    return file;
  }

  /** The unexpired response for {@code key}, or null. */
  @Nullable StoredResponse get(String key) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT body, created_at FROM responses WHERE key = ? AND created_at > ?")) {
      statement.setString(1, key);
      statement.setLong(2, cutoff());
      try (ResultSet result = statement.executeQuery()) {
        return result.next()
            ? new StoredResponse(
                result.getString(1), Instant.ofEpochMilli(result.getLong(2)).plus(ttl))
            : null;
      }
    }
  }

  void put(String key, String body) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT OR REPLACE INTO responses (key, body, created_at) VALUES (?, ?, ?)")) {
      statement.setString(1, key);
      statement.setString(2, body);
      statement.setLong(3, clock.millis());
      statement.executeUpdate();
    }
    if (writesSincePurge.incrementAndGet() >= PURGE_INTERVAL) {
      writesSincePurge.set(0);
      purge();
    }
  }

  /** Whether the database can currently be queried. */
  boolean isAvailable() {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("SELECT 1");
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  /** Removes expired rows, then the oldest rows beyond {@code maxEntries}. */
  void purge() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement expired =
            connection.prepareStatement("DELETE FROM responses WHERE created_at <= ?");
        PreparedStatement surplus =
            connection.prepareStatement(
                "DELETE FROM responses WHERE key NOT IN "
                    + "(SELECT key FROM responses ORDER BY created_at DESC LIMIT ?)")) {
      expired.setLong(1, cutoff());
      expired.executeUpdate();
      surplus.setInt(1, maxEntries);
      surplus.executeUpdate();
    }
  }

  private long cutoff() {
    return clock.millis() - ttl.toMillis();
  }

  /** A stored response and the moment it expires. */
  record StoredResponse(String body, Instant expiresAt) {}
}
