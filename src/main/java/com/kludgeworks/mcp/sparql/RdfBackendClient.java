package com.kludgeworks.mcp.sparql;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Service
class RdfBackendClient {

    private static final MediaType SPARQL_QUERY_MEDIA_TYPE = MediaType.parseMediaType("application/sparql-query");

    private final RestClient restClient;

    RdfBackendClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(cacheNames = "rdfBackendResponses", key = "T(java.lang.String).join('||', #serviceUrl, #acceptType.toString(), #query)")
    public String executeQuery(String serviceUrl, String query, MediaType acceptType) throws IOException {
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
}
