package com.kludgeworks.mcp.sparql;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Runtime configuration for the MCP server.
 *
 * <p>Everything here is bound at startup rather than evaluated as a bean condition, so the same
 * native image can run in either transport mode.
 */
@ConfigurationProperties("sparql-mcp")
public record SparqlMcpProperties(
    @DefaultValue("stdio") Transport transport,
    @DefaultValue Server server,
    @DefaultValue Http http,
    @DefaultValue Cache cache,
    @DefaultValue List<String> allowedEndpoints) {

  public enum Transport {
    STDIO,
    HTTP
  }

  public record Server(
      @DefaultValue("rdf-sparql") String name, @DefaultValue("0.0.0-SNAPSHOT") String version) {}

  public record Http(
      @DefaultValue("127.0.0.1") String address,
      @DefaultValue("8080") int port,
      @DefaultValue("/mcp") String endpoint,
      @DefaultValue List<String> allowedOrigins,
      @DefaultValue({"localhost:*", "127.0.0.1:*", "[::1]:*"}) List<String> allowedHosts) {}

  /**
   * @param directory Ehcache persistence directory; defaults to a per-transport directory under the
   *     user data dir
   */
  public record Cache(@Nullable String directory) {}

  /** Whether {@code serviceUrl} may be queried; an empty allowlist permits every endpoint. */
  public boolean isEndpointAllowed(String serviceUrl) {
    return allowedEndpoints.isEmpty() || allowedEndpoints.stream().anyMatch(serviceUrl::startsWith);
  }
}
