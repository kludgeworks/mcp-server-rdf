package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class CacheDirectoryTest {

  // Under target/ rather than a JUnit @TempDir: the Spring context outlives the test method, and
  // `mvn clean` removes it.
  private static final Path CUSTOM_DIRECTORY =
      Path.of("target", "test-tmp", "custom-cache-" + UUID.randomUUID());

  @DynamicPropertySource
  static void cacheDirectory(DynamicPropertyRegistry registry) {
    registry.add("sparql-mcp.cache.directory", CUSTOM_DIRECTORY::toString);
  }

  @Autowired private ResponseCacheHealthIndicator healthIndicator;

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
}
