package com.kludgeworks.mcp.sparql.query;

import java.util.Map;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Binds {@link QueryProperties} the same way Spring Boot does, including defaults. */
final class TestProperties {

  private TestProperties() {}

  static QueryProperties defaults() {
    return of(Map.of());
  }

  static QueryProperties of(Map<String, String> properties) {
    return new Binder(new MapConfigurationPropertySource(properties))
        .bindOrCreate("sparql-mcp", QueryProperties.class);
  }
}
