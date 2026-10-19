package com.kludgeworks.mcp.sparql.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kludgeworks.mcp.sparql.query.backend.TestBackendClients;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SparqlQueriesTest {

  private static final String ALLOWED = "https://publications.europa.eu/webapi/rdf/sparql";
  private static final String METADATA_SERVICE = "http://169.254.169.254/latest";

  @Test
  void allowlistPermitsEverythingWhenEmptyAndMatchesByPrefixOtherwise() {
    QueryProperties restricted =
        TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED));

    assertThat(TestProperties.defaults().isEndpointAllowed(METADATA_SERVICE)).isTrue();
    assertThat(restricted.isEndpointAllowed(ALLOWED)).isTrue();
    assertThat(restricted.isEndpointAllowed(METADATA_SERVICE)).isFalse();
  }

  @Test
  void rejectsDisallowedEndpointBeforeCallingBackend() {
    QueryProperties properties =
        TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED));
    // Reaching the backend would fail with a connection error, not IllegalArgumentException.
    SparqlQueries queries = new SparqlQueries(TestBackendClients.create(), properties);

    assertThatThrownBy(() -> queries.select(METADATA_SERVICE, "SELECT * {}", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not allowed");
  }
}
