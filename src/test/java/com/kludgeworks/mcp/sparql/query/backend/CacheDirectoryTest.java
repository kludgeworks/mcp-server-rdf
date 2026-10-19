package com.kludgeworks.mcp.sparql.query.backend;

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

  // Not a JUnit @TempDir: Ehcache memory-maps its disk store, and Windows cannot delete a mapped
  // file until the JVM releases it. Under target/, `mvn clean` removes it instead.
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

    assertThat(Files.isDirectory(CUSTOM_DIRECTORY)).isTrue();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("cache", CacheConfig.CACHE_NAME)
        .containsEntry("storage", "persistent");
  }
}
