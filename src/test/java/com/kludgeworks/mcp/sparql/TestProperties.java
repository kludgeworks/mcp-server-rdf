package com.kludgeworks.mcp.sparql;

import java.util.Map;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Binds {@link SparqlMcpProperties} the same way Spring Boot does, including defaults. */
final class TestProperties {

  private TestProperties() {}

  static SparqlMcpProperties defaults() {
    return of(Map.of());
  }

  static SparqlMcpProperties of(Map<String, String> properties) {
    return new Binder(new MapConfigurationPropertySource(properties))
        .bindOrCreate("sparql-mcp", SparqlMcpProperties.class);
  }
}
