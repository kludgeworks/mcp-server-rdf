package com.kludgeworks.mcp.sparql;

import java.io.IOException;
import java.util.Objects;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class RdfBackendClient {

  private static final MediaType SPARQL_QUERY_MEDIA_TYPE =
      MediaType.parseMediaType("application/sparql-query");

  private final RestClient restClient;

  RdfBackendClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Cacheable(
      cacheNames = "rdfBackendResponses",
      key = "#serviceUrl + '||' + #acceptType + '||' + #query")
  public String executeQuery(String serviceUrl, String query, MediaType acceptType)
      throws IOException {
    String responseBody =
        Objects.requireNonNull(
            restClient
                .post()
                .uri(serviceUrl)
                .contentType(SPARQL_QUERY_MEDIA_TYPE)
                .accept(acceptType)
                .body(query)
                .retrieve()
                .body(String.class),
            "RDF backend returned an empty response");

    if (responseBody.isBlank()) {
      throw new IOException("RDF backend returned an empty response");
    }

    return responseBody;
  }
}
