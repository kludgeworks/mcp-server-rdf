package com.kludgeworks.mcp.sparql.query.backend;

import com.kludgeworks.mcp.sparql.query.port.SparqlEndpoint;
import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Posts SPARQL queries to the endpoint over HTTP, using the SPARQL 1.1 protocol. */
@Component
class HttpSparqlEndpoint implements SparqlEndpoint {

  private static final MediaType SPARQL_QUERY_MEDIA_TYPE =
      MediaType.parseMediaType("application/sparql-query");

  private final RestClient restClient;

  HttpSparqlEndpoint(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public String execute(SparqlRequest request) throws IOException {
    String responseBody =
        restClient
            .post()
            .uri(request.serviceUrl())
            .contentType(SPARQL_QUERY_MEDIA_TYPE)
            .accept(request.acceptType())
            .body(request.query())
            .retrieve()
            .body(String.class);
    if (responseBody == null || responseBody.isBlank()) {
      throw new IOException("RDF backend returned an empty response");
    }
    return responseBody;
  }
}
