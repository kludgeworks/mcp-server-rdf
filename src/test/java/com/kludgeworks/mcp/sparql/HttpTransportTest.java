package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"sparql-mcp.transport=http", "sparql-mcp.http.port=0"})
class HttpTransportTest {

  private static final String TOOLS_LIST =
      """
      {"jsonrpc":"2.0","id":1,"method":"tools/list"}\
      """;

  private static final String SELECT_CALL =
      """
      {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"select",
       "arguments":{"serviceUrl":"https://example.org/sparql","query":"SELECT * {}"}}}\
      """;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Autowired private McpServerRunner runner;

  @MockitoBean private RdfBackendClient rdfBackendClient;

  @Test
  void listsToolsWithOptionalPagingParameters() throws Exception {
    HttpResponse<String> response = postMcp(TOOLS_LIST, null);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode tools = jsonMapper.readTree(response.body()).path("result").path("tools");
    assertThat(tools.findValuesAsString("name")).contains("select", "describe");
    for (JsonNode tool : tools) {
      assertThat(tool.path("inputSchema").path("required").toString())
          .contains("serviceUrl", "query")
          .doesNotContain("maxChars", "offset");
    }
  }

  @Test
  void callsToolAndRecordsMetrics() throws Exception {
    given(rdfBackendClient.executeQuery(anyString(), anyString(), any(MediaType.class)))
        .willReturn("{\"results\":{\"bindings\":[]}}");

    HttpResponse<String> response = postMcp(SELECT_CALL, null);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode result = jsonMapper.readTree(response.body()).path("result");
    assertThat(result.path("isError").asBoolean()).isFalse();
    assertThat(result.path("content").get(0).path("text").asString()).contains("pagination");

    String metrics = get("/metrics").body();
    assertThat(metrics)
        .containsPattern("mcp_tool_calls_seconds_count\\{[^}]*tool=\"select\"[^}]*\\}");
    assertThat(metrics).contains("jvm_memory_used_bytes");
  }

  @Test
  void rejectsForeignOrigin() throws Exception {
    HttpResponse<String> response = postMcp(TOOLS_LIST, "https://evil.example");

    assertThat(response.statusCode()).isEqualTo(403);
  }

  @Test
  void exposesHealthProbes() throws Exception {
    for (String path : new String[] {"/health", "/health/liveness", "/health/readiness"}) {
      HttpResponse<String> response = get(path);
      assertThat(response.statusCode()).as(path).isEqualTo(200);
      assertThat(response.body()).as(path).contains("\"UP\"");
    }
  }

  private HttpResponse<String> postMcp(String body, @Nullable String origin)
      throws IOException, InterruptedException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri("/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(body));
    if (origin != null) {
      request.header("Origin", origin);
    }
    return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    return httpClient.send(
        HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + runner.httpPort() + path);
  }
}
