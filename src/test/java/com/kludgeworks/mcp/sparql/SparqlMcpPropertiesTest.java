package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SparqlMcpPropertiesTest {

  private static final String ALLOWED = "https://publications.europa.eu/webapi/rdf/sparql";
  private static final String METADATA_SERVICE = "http://169.254.169.254/latest";

  @Test
  void allowlistPermitsEverythingWhenEmptyAndMatchesByPrefixOtherwise() {
    SparqlMcpProperties restricted =
        TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED));

    assertThat(TestProperties.defaults().isEndpointAllowed(METADATA_SERVICE)).isTrue();
    assertThat(restricted.isEndpointAllowed(ALLOWED)).isTrue();
    assertThat(restricted.isEndpointAllowed(METADATA_SERVICE)).isFalse();
  }

  @Test
  void rdfServiceRejectsDisallowedEndpointBeforeCallingBackend() {
    SparqlMcpProperties properties =
        TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED));
    // Reaching the backend would fail with a connection error, not IllegalArgumentException.
    RdfService service =
        new RdfService(TestBackendClients.create(), JsonMapper.builder().build(), properties);

    assertThatThrownBy(() -> service.select(METADATA_SERVICE, "SELECT * {}", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not allowed");
  }
}
