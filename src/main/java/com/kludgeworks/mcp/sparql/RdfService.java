package com.kludgeworks.mcp.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
class RdfService {

    private static final int DEFAULT_PAGE_SIZE_CHARS = 5000;
    private static final MediaType SPARQL_RESULTS_JSON_MEDIA_TYPE = MediaType.parseMediaType("application/sparql-results+json");
    private static final MediaType RDF_JSON_MEDIA_TYPE = MediaType.parseMediaType("application/rdf+json");

    record Pagination(
        int totalChars,
        int pageSizeChars,
        int totalPages,
        int pageIndex,
        int offset,
        int returnedChars
    ) {}

    record PagedResult(
        Pagination pagination,
        String result
    ) {}

    private final RdfBackendClient rdfBackendClient;
    private final ObjectMapper objectMapper;

    RdfService(RdfBackendClient rdfBackendClient, ObjectMapper objectMapper) {
        this.rdfBackendClient = rdfBackendClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Run a SELECT query against a RDF datastore. Returns a paged JSON envelope with pagination metadata and a result chunk.")
    String select(
    @McpToolParam(description = "SPARQL endpoint URL to execute the query against") String serviceUrl,
    @McpToolParam(description = "SPARQL SELECT query text") String query,
    @McpToolParam(description = "Maximum number of characters to return in this page: defaults to " + DEFAULT_PAGE_SIZE_CHARS, required = false) Integer maxChars,
    @McpToolParam(description = "Character offset for paging through the full response", required = false) Integer offset) throws IOException {
        return paginate(rdfBackendClient.executeQuery(serviceUrl, query, SPARQL_RESULTS_JSON_MEDIA_TYPE), maxChars, offset);
    }

    @Tool(description = "Run a DESCRIBE query against a RDF datastore. Returns a paged JSON envelope with pagination metadata and a result chunk.")
    String describe(
    @McpToolParam(description = "SPARQL endpoint URL to execute the query against") String serviceUrl,
    @McpToolParam(description = "SPARQL DESCRIBE query text") String query,
    @McpToolParam(description = "Maximum number of characters to return in this page", required = false) Integer maxChars,
    @McpToolParam(description = "Character offset for paging through the full response", required = false) Integer offset) throws IOException {
        return paginate(rdfBackendClient.executeQuery(serviceUrl, query, RDF_JSON_MEDIA_TYPE), maxChars, offset);
    }

    private String paginate(String responseBody, Integer maxChars, Integer offset) throws IOException {
        int pageSizeChars = (maxChars == null || maxChars <= 0) ? DEFAULT_PAGE_SIZE_CHARS : maxChars;
        int effectiveOffset = (offset == null || offset < 0) ? 0 : offset;
        int totalChars = responseBody.length();
        int totalPages = totalChars == 0 ? 1 : (int) Math.ceil((double) totalChars / pageSizeChars);
        int pageIndex = effectiveOffset / pageSizeChars;
        int start = Math.min(effectiveOffset, totalChars);
        int end = Math.min(start + pageSizeChars, totalChars);
        String resultChunk = responseBody.substring(start, end);

        PagedResult payload = new PagedResult(
            new Pagination(
                totalChars,
                pageSizeChars,
                totalPages,
                pageIndex,
                effectiveOffset,
                resultChunk.length()
            ),
            resultChunk
        );

        return objectMapper.writeValueAsString(payload);
    }
}
