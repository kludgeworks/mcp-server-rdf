package com.kludgeworks.mcp.sparql;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serves health probes and the Prometheus scrape on the embedded Tomcat used by the HTTP transport.
 *
 * <p>Spring Boot only exposes actuator endpoints over HTTP when it owns the web server. Here the
 * MCP server owns Tomcat, so the endpoint beans are bridged onto it directly.
 */
final class DiagnosticsServlet extends HttpServlet {

  static final String HEALTH_PATH = "/health";
  static final String METRICS_PATH = "/metrics";

  private static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

  private final HealthEndpoint healthEndpoint;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final JsonMapper jsonMapper;

  DiagnosticsServlet(
      HealthEndpoint healthEndpoint,
      PrometheusMeterRegistry prometheusMeterRegistry,
      JsonMapper jsonMapper) {
    this.healthEndpoint = healthEndpoint;
    this.prometheusMeterRegistry = prometheusMeterRegistry;
    this.jsonMapper = jsonMapper;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String path =
        request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
    switch (path) {
      case HEALTH_PATH -> writeHealth(response, healthEndpoint.health());
      case HEALTH_PATH + "/liveness" ->
          writeHealth(response, healthEndpoint.healthForPath("liveness"));
      case HEALTH_PATH + "/readiness" ->
          writeHealth(response, healthEndpoint.healthForPath("readiness"));
      case METRICS_PATH -> writeMetrics(response);
      default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void writeHealth(HttpServletResponse response, @Nullable HealthDescriptor health)
      throws IOException {
    // healthForPath returns null for an unknown group; treat that as a missing resource.
    if (health == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    Status status = health.getStatus();
    response.setStatus(
        Status.UP.equals(status)
            ? HttpServletResponse.SC_OK
            : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getOutputStream(), Map.of("status", status.getCode()));
  }

  private void writeMetrics(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(PROMETHEUS_CONTENT_TYPE);
    response
        .getOutputStream()
        .write(prometheusMeterRegistry.scrape().getBytes(StandardCharsets.UTF_8));
  }
}
