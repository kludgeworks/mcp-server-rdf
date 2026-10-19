package com.kludgeworks.mcp.sparql.query.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kludgeworks.mcp.sparql.query.port.SparqlRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpSparqlEndpointTest {

  private static final String ENDPOINT = "https://sparql.example/query";
  private static final MediaType RESULTS_JSON =
      MediaType.parseMediaType("application/sparql-results+json");
  private static final SparqlRequest REQUEST =
      new SparqlRequest(ENDPOINT, "SELECT * {}", RESULTS_JSON);

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

  @Test
  void postsQueryUsingTheSparqlProtocol() throws Exception {
    server
        .expect(requestTo(ENDPOINT))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Content-Type", "application/sparql-query"))
        .andExpect(header("Accept", RESULTS_JSON.toString()))
        .andExpect(content().string("SELECT * {}"))
        .andRespond(withSuccess("{\"results\":{}}", RESULTS_JSON));

    assertThat(new HttpSparqlEndpoint(builder).execute(REQUEST)).isEqualTo("{\"results\":{}}");
    server.verify();
  }

  @Test
  void rejectsBlankResponse() {
    server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(" ", RESULTS_JSON));

    assertThatThrownBy(() -> new HttpSparqlEndpoint(builder).execute(REQUEST))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("empty response");
  }
}
