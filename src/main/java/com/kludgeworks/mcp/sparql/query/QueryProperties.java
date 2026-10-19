package com.kludgeworks.mcp.sparql.query;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for running SPARQL queries. Shares the {@code sparql-mcp} prefix with the server.
 */
@ConfigurationProperties("sparql-mcp")
public record QueryProperties(
    @DefaultValue Cache cache, @DefaultValue List<String> allowedEndpoints) {

  /**
   * @param directory Ehcache persistence directory; defaults to a per-transport directory under the
   *     user cache dir
   */
  public record Cache(@Nullable String directory) {}

  /** Whether {@code serviceUrl} may be queried; an empty allowlist permits every endpoint. */
  public boolean isEndpointAllowed(String serviceUrl) {
    return allowedEndpoints.isEmpty() || allowedEndpoints.stream().anyMatch(serviceUrl::startsWith);
  }
}
