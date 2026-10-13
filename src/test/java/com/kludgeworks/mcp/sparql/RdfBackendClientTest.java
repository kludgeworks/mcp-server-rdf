package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RdfBackendClientTest {

  private static final String ENDPOINT = "https://sparql.example/query";
  private static final MediaType RESULTS_JSON =
      MediaType.parseMediaType("application/sparql-results+json");

  @Test
  void servesRepeatedQueryFromCacheAndCountsHitsAndMisses() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(ExpectedCount.once(), requestTo(ENDPOINT))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"results\":{}}", RESULTS_JSON));
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    RdfBackendClient client = TestBackendClients.create(builder.build(), meterRegistry);

    String first = client.executeQuery(ENDPOINT, "SELECT * {}", RESULTS_JSON);
    String second = client.executeQuery(ENDPOINT, "SELECT * {}", RESULTS_JSON);

    assertThat(second).isEqualTo(first);
    server.verify();
    assertThat(cacheRequests(meterRegistry, "miss")).isEqualTo(1.0);
    assertThat(cacheRequests(meterRegistry, "hit")).isEqualTo(1.0);
  }

  private static double cacheRequests(SimpleMeterRegistry meterRegistry, String result) {
    return meterRegistry
        .get(RdfBackendClient.CACHE_REQUESTS_METRIC)
        .tag("result", result)
        .counter()
        .count();
  }
}
