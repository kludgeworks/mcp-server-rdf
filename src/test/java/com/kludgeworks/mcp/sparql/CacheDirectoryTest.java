package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Close this context (and Ehcache's open files) after the class, before JUnit deletes the
// @TempDir; Windows cannot delete files that are still open.
@DirtiesContext
@SpringBootTest
class CacheDirectoryTest {

  // Created under java.io.tmpdir, which Surefire points at target/; JUnit removes it afterwards.
  @TempDir static Path customDirectory;

  @DynamicPropertySource
  static void cacheDirectory(DynamicPropertyRegistry registry) {
    registry.add("sparql-mcp.cache.directory", () -> customDirectory.toString());
  }

  @Autowired private ResponseCacheHealthIndicator healthIndicator;

  @Test
  void persistsCacheInConfiguredDirectory() {
    Health health = healthIndicator.health();

    assertThat(Files.isDirectory(customDirectory)).isTrue();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("cache", CacheConfig.CACHE_NAME)
        .containsEntry("storage", "persistent");
  }
}
