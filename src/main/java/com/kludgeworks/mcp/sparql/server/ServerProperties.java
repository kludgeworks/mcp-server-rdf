package com.kludgeworks.mcp.sparql.server;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Runtime configuration for the MCP server.
 *
 * <p>Everything here is bound at startup rather than evaluated as a bean condition, so the same
 * native image can run in either transport mode.
 */
@ConfigurationProperties("sparql-mcp")
public record ServerProperties(
    @DefaultValue("stdio") Transport transport,
    @DefaultValue Server server,
    @DefaultValue Http http) {

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
}
