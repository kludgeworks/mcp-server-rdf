package com.kludgeworks.mcp.sparql.query;

import com.kludgeworks.mcp.sparql.query.backend.RdfBackendClient;
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

  private final RdfBackendClient rdfBackendClient;
  private final QueryProperties properties;

  SparqlQueries(RdfBackendClient rdfBackendClient, QueryProperties properties) {
    this.rdfBackendClient = rdfBackendClient;
    this.properties = properties;
  }

  public PagedResult select(
      String serviceUrl, String query, @Nullable Integer maxChars, @Nullable Integer offset)
      throws IOException {
    return PagedResult.of(
        rdfBackendClient.executeQuery(
            checkEndpointAllowed(serviceUrl), query, SPARQL_RESULTS_JSON_MEDIA_TYPE),
        maxChars,
        offset);
  }

  public PagedResult describe(
      String serviceUrl, String query, @Nullable Integer maxChars, @Nullable Integer offset)
      throws IOException {
    return PagedResult.of(
        rdfBackendClient.executeQuery(checkEndpointAllowed(serviceUrl), query, RDF_JSON_MEDIA_TYPE),
        maxChars,
        offset);
  }

  /**
   * Checked before the cached backend call, so a cached response is never served for an endpoint
   * that has since been removed from the allowlist.
   */
  private String checkEndpointAllowed(String serviceUrl) {
    if (!properties.isEndpointAllowed(serviceUrl)) {
      throw new IllegalArgumentException(
          "SPARQL endpoint is not allowed by sparql-mcp.allowed-endpoints: " + serviceUrl);
    }
    return serviceUrl;
  }
}
