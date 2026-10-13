package com.kludgeworks.mcp.sparql;

import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

// The paged envelope is serialised by Jackson, so the native image needs reflection metadata for
// it.
@RegisterReflectionForBinding({RdfService.PagedResult.class, RdfService.Pagination.class})
@Service
class RdfService {

  private static final int DEFAULT_PAGE_SIZE_CHARS = 5000;
  private static final MediaType SPARQL_RESULTS_JSON_MEDIA_TYPE =
      MediaType.parseMediaType("application/sparql-results+json");
  private static final MediaType RDF_JSON_MEDIA_TYPE =
      MediaType.parseMediaType("application/rdf+json");

  public record Pagination(
      int totalChars,
      int pageSizeChars,
      int totalPages,
      int pageIndex,
      int offset,
      int returnedChars) {}

  public record PagedResult(Pagination pagination, String result) {}

  private final RdfBackendClient rdfBackendClient;
  private final ObjectMapper objectMapper;
  private final SparqlMcpProperties properties;

  RdfService(
      RdfBackendClient rdfBackendClient,
      ObjectMapper objectMapper,
      SparqlMcpProperties properties) {
    this.rdfBackendClient = rdfBackendClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Tool(
      description =
          "Run a SELECT query against a RDF datastore. Returns a paged JSON envelope with"
              + " pagination metadata and a result chunk.")
  String select(
      @McpToolParam(description = "SPARQL endpoint URL to execute the query against")
          String serviceUrl,
      @McpToolParam(description = "SPARQL SELECT query text") String query,
      @McpToolParam(
              description =
                  "Maximum number of characters to return in this page: defaults to "
                      + DEFAULT_PAGE_SIZE_CHARS,
              required = false)
          @Nullable Integer maxChars,
      @McpToolParam(
              description = "Character offset for paging through the full response",
              required = false)
          @Nullable Integer offset)
      throws IOException {
    return paginate(
        rdfBackendClient.executeQuery(
            checkEndpointAllowed(serviceUrl), query, SPARQL_RESULTS_JSON_MEDIA_TYPE),
        maxChars,
        offset);
  }

  @Tool(
      description =
          "Run a DESCRIBE query against a RDF datastore. Returns a paged JSON envelope with"
              + " pagination metadata and a result chunk.")
  String describe(
      @McpToolParam(description = "SPARQL endpoint URL to execute the query against")
          String serviceUrl,
      @McpToolParam(description = "SPARQL DESCRIBE query text") String query,
      @McpToolParam(
              description = "Maximum number of characters to return in this page",
              required = false)
          @Nullable Integer maxChars,
      @McpToolParam(
              description = "Character offset for paging through the full response",
              required = false)
          @Nullable Integer offset)
      throws IOException {
    return paginate(
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

  private String paginate(
      String responseBody, @Nullable Integer maxChars, @Nullable Integer offset) {
    int pageSizeChars = (maxChars == null || maxChars <= 0) ? DEFAULT_PAGE_SIZE_CHARS : maxChars;
    int effectiveOffset = (offset == null || offset < 0) ? 0 : offset;
    int totalChars = responseBody.length();
    int totalPages = totalChars == 0 ? 1 : (int) Math.ceil((double) totalChars / pageSizeChars);
    int pageIndex = effectiveOffset / pageSizeChars;
    int start = Math.min(effectiveOffset, totalChars);
    int end = Math.min(start + pageSizeChars, totalChars);
    String resultChunk = responseBody.substring(start, end);

    PagedResult payload =
        new PagedResult(
            new Pagination(
                totalChars,
                pageSizeChars,
                totalPages,
                pageIndex,
                effectiveOffset,
                resultChunk.length()),
            resultChunk);

    return objectMapper.writeValueAsString(payload);
  }
}
