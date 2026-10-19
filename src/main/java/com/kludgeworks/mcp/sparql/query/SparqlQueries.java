package com.kludgeworks.mcp.sparql.query;

import com.kludgeworks.mcp.sparql.query.port.ResponseCache;
import com.kludgeworks.mcp.sparql.query.port.SparqlEndpoint;
import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/** Runs SPARQL queries against caller-supplied endpoints and pages the response. */
@Service
public class SparqlQueries {

  private static final MediaType SPARQL_RESULTS_JSON_MEDIA_TYPE =
      MediaType.parseMediaType("application/sparql-results+json");
  private static final MediaType RDF_JSON_MEDIA_TYPE =
      MediaType.parseMediaType("application/rdf+json");

  private final SparqlEndpoint endpoint;
  private final ResponseCache cache;
  private final QueryProperties properties;

  SparqlQueries(SparqlEndpoint endpoint, ResponseCache cache, QueryProperties properties) {
    this.endpoint = endpoint;
    this.cache = cache;
    this.properties = properties;
  }

  public PagedResult select(
      String serviceUrl, String query, @Nullable Integer maxChars, @Nullable Integer offset)
      throws IOException {
    return run(
        new SparqlRequest(checkEndpointAllowed(serviceUrl), query, SPARQL_RESULTS_JSON_MEDIA_TYPE),
        maxChars,
        offset);
  }

  public PagedResult describe(
      String serviceUrl, String query, @Nullable Integer maxChars, @Nullable Integer offset)
      throws IOException {
    return run(
        new SparqlRequest(checkEndpointAllowed(serviceUrl), query, RDF_JSON_MEDIA_TYPE),
        maxChars,
        offset);
  }

  private PagedResult run(
      SparqlRequest request, @Nullable Integer maxChars, @Nullable Integer offset)
      throws IOException {
    String responseBody = cache.get(request, () -> endpoint.execute(request));
    return PagedResult.of(responseBody, maxChars, offset);
  }

  /**
   * Checked before the cache, so a cached response is never served for an endpoint that has since
   * been removed from the allowlist.
   */
  private String checkEndpointAllowed(String serviceUrl) {
    if (!properties.isEndpointAllowed(serviceUrl)) {
      throw new IllegalArgumentException(
          "SPARQL endpoint is not allowed by sparql-mcp.allowed-endpoints: " + serviceUrl);
    }
    return serviceUrl;
  }
}
