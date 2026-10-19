package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ResponseCacheConfigTest {

  // Under target/ rather than a JUnit @TempDir: the Spring context outlives the test method, and
  // `mvn clean` removes it.
  private static final Path CUSTOM_DIRECTORY =
      Path.of("target", "test-tmp", "custom-cache-" + UUID.randomUUID());

  @DynamicPropertySource
  static void cacheDirectory(DynamicPropertyRegistry registry) {
    registry.add("sparql-mcp.cache.directory", CUSTOM_DIRECTORY::toString);
  }

  @Autowired private ResponseCacheHealthIndicator healthIndicator;
  @Autowired private ApplicationEventPublisher events;

  @Test
  void persistsCacheInConfiguredDirectory() {
    Health health = healthIndicator.health();

    assertThat(Files.isRegularFile(CUSTOM_DIRECTORY.resolve(SqliteResponseStore.FILE_NAME)))
        .isTrue();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("cache", TwoLevelResponseCache.CACHE_NAME)
        .containsEntry("storage", "persistent");
  }

  @Test
  void purgesInTheBackgroundWhenPurgeIsDue() throws Exception {
    // An expired row, as if written by an earlier run.
    execute("INSERT INTO responses (key, body, created_at) VALUES ('expired', 'body', 0)");

    events.publishEvent(new ResponseStorePurgeDue());

    await().atMost(Duration.ofSeconds(5)).until(() -> rowCount() == 0);
  }

  private static void execute(String sql) throws Exception {
    try (Connection connection = connect();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  private static int rowCount() throws Exception {
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT count(*) FROM responses")) {
      result.next();
      return result.getInt(1);
    }
  }

  private static Connection connect() throws Exception {
    return DriverManager.getConnection(
        "jdbc:sqlite:" + CUSTOM_DIRECTORY.resolve(SqliteResponseStore.FILE_NAME).toAbsolutePath());
  }
}
