package com.kludgeworks.mcp.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RdfService {

    private static final int DEFAULT_PAGE_SIZE_CHARS = 5000;
    private static final MediaType SPARQL_QUERY_MEDIA_TYPE = MediaType.parseMediaType("application/sparql-query");
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

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    RdfService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Run a SELECT query against a RDF datastore. Returns a paged JSON envelope with pagination metadata and a result chunk.")
    public String select(
    @McpToolParam(description = "SPARQL endpoint URL to execute the query against") String serviceUrl,
    @McpToolParam(description = "SPARQL SELECT query text") String query,
    @McpToolParam(description = "Maximum number of characters to return in this page: defaults to " + DEFAULT_PAGE_SIZE_CHARS, required = false) Integer maxChars,
    @McpToolParam(description = "Character offset for paging through the full response", required = false) Integer offset) throws IOException {
        return paginate(executeQuery(serviceUrl, query, SPARQL_RESULTS_JSON_MEDIA_TYPE), maxChars, offset);
    }

    @Tool(description = "Run a DESCRIBE query against a RDF datastore. Returns a paged JSON envelope with pagination metadata and a result chunk.")
    public String describe(
    @McpToolParam(description = "SPARQL endpoint URL to execute the query against") String serviceUrl,
    @McpToolParam(description = "SPARQL DESCRIBE query text") String query,
    @McpToolParam(description = "Maximum number of characters to return in this page", required = false) Integer maxChars,
    @McpToolParam(description = "Character offset for paging through the full response", required = false) Integer offset) throws IOException {
        return paginate(executeQuery(serviceUrl, query, RDF_JSON_MEDIA_TYPE), maxChars, offset);
    }

    private String executeQuery(String serviceUrl, String query, MediaType acceptType) throws IOException {
        String responseBody = restClient
            .post()
            .uri(serviceUrl)
            .contentType(SPARQL_QUERY_MEDIA_TYPE)
            .accept(acceptType)
            .body(query)
            .retrieve()
            .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("RDF backend returned an empty response");
        }

        return responseBody;
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
