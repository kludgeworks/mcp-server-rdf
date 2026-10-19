package com.kludgeworks.mcp.sparql.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kludgeworks.mcp.sparql.query.port.ResponseCache;
import com.kludgeworks.mcp.sparql.query.port.SparqlEndpoint;
import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SparqlQueriesTest {

  private static final String ALLOWED = "https://publications.europa.eu/webapi/rdf/sparql";
  private static final String METADATA_SERVICE = "http://169.254.169.254/latest";

  private final List<SparqlRequest> executed = new ArrayList<>();
  private final SparqlEndpoint endpoint =
      request -> {
        executed.add(request);
        return "0123456789";
      };
  private final ResponseCache noCache = (request, loader) -> loader.load();

  @Test
  void allowlistPermitsEverythingWhenEmptyAndMatchesByPrefixOtherwise() {
    QueryProperties restricted =
        TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED));

    assertThat(TestProperties.defaults().isEndpointAllowed(METADATA_SERVICE)).isTrue();
    assertThat(restricted.isEndpointAllowed(ALLOWED)).isTrue();
    assertThat(restricted.isEndpointAllowed(METADATA_SERVICE)).isFalse();
  }

  @Test
  void rejectsDisallowedEndpointBeforeCheckingCacheOrEndpoint() {
    ResponseCache failingCache =
        (request, loader) -> {
          throw new AssertionError("cache must not be consulted");
        };
    SparqlQueries queries =
        new SparqlQueries(
            endpoint,
            failingCache,
            TestProperties.of(Map.of("sparql-mcp.allowed-endpoints[0]", ALLOWED)));

    assertThatThrownBy(() -> queries.select(METADATA_SERVICE, "SELECT * {}", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not allowed");
    assertThat(executed).isEmpty();
  }

  @Test
  void requestsSparqlResultsForSelectAndRdfForDescribeAndPagesTheResponse() throws Exception {
    SparqlQueries queries = new SparqlQueries(endpoint, noCache, TestProperties.defaults());

    PagedResult select = queries.select(ALLOWED, "SELECT * {}", 4, 4);
    queries.describe(ALLOWED, "DESCRIBE <x>", null, null);

    assertThat(select.result()).isEqualTo("4567");
    assertThat(select.pagination().totalPages()).isEqualTo(3);
    assertThat(executed)
        .extracting(request -> request.acceptType().toString())
        .containsExactly("application/sparql-results+json", "application/rdf+json");
  }
}
