package com.kludgeworks.mcp.sparql.query.mcp;

import com.kludgeworks.mcp.sparql.query.PagedResult;
import com.kludgeworks.mcp.sparql.query.SparqlQueries;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Exposes {@link SparqlQueries} as MCP tools. */
// The paged envelope is serialised by Jackson, so the native image needs reflection metadata for
// it.
@RegisterReflectionForBinding({PagedResult.class, PagedResult.Pagination.class})
@Component
class SparqlTools {

  private final SparqlQueries queries;
  private final ObjectMapper objectMapper;

  SparqlTools(SparqlQueries queries, ObjectMapper objectMapper) {
    this.queries = queries;
    this.objectMapper = objectMapper;
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
                      + PagedResult.DEFAULT_PAGE_SIZE_CHARS,
              required = false)
          @Nullable Integer maxChars,
      @McpToolParam(
              description = "Character offset for paging through the full response",
              required = false)
          @Nullable Integer offset)
      throws IOException {
    return objectMapper.writeValueAsString(queries.select(serviceUrl, query, maxChars, offset));
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
    return objectMapper.writeValueAsString(queries.describe(serviceUrl, query, maxChars, offset));
  }
}
