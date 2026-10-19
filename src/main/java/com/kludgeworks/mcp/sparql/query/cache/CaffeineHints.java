package com.kludgeworks.mcp.sparql.query.cache;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * Caffeine picks a generated cache and entry class by name from the builder's features, and reaches
 * its constructor and static factory field reflectively. The published GraalVM metadata does not
 * cover every combination, so the classes our builder uses are registered here. {@code
 * CaffeineHintsTest} fails when a Caffeine upgrade or a builder change selects different classes.
 */
class CaffeineHints implements RuntimeHintsRegistrar {

  /** Strong keys and values, stats, maximum size and per-entry expiry. */
  static final List<String> GENERATED_CLASSES =
      List.of(
          "com.github.benmanes.caffeine.cache.SSSMSA", "com.github.benmanes.caffeine.cache.PSWMS");

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    for (String className : GENERATED_CLASSES) {
      hints
          .reflection()
          .registerType(
              TypeReference.of(className),
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.ACCESS_DECLARED_FIELDS);
    }
  }
}
