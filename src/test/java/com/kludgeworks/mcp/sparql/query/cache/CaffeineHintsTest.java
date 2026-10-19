package com.kludgeworks.mcp.sparql.query.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CaffeineHintsTest {

  /**
   * Reads Caffeine internals on purpose: if a Caffeine upgrade or a builder change selects other
   * generated classes, this fails on the JVM instead of the native image failing at startup.
   */
  @Test
  void hintsCoverTheGeneratedClassesTheCacheUses() throws Exception {
    TwoLevelResponseCache cache =
        new TwoLevelResponseCache(
            null, 10, Clock.systemUTC(), Duration.ofHours(1), new SimpleMeterRegistry());

    Object localCache = field(field(cache, "memory"), "cache");
    Object nodeFactory = field(localCache, "nodeFactory");

    assertThat(CaffeineHints.GENERATED_CLASSES)
        .containsExactly(localCache.getClass().getName(), nodeFactory.getClass().getName());
  }

  private static Object field(Object target, String name) throws ReflectiveOperationException {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException e) {
        // Declared on a superclass.
      }
    }
    throw new NoSuchFieldException(name);
  }
}
