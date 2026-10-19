package com.kludgeworks.mcp.sparql.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts the MCP server on the transport selected by {@code sparql-mcp.transport}, serving the
 * tools of every {@link ToolCallbackProvider} bean.
 *
 * <p>The transport is chosen here at runtime instead of through Spring AI's conditional
 * auto-configuration. AOT processing freezes bean conditions at build time, but it does not freeze
 * what a lifecycle bean does when it starts, so one native image supports both transports.
 */
@Component
class McpServerRunner implements SmartLifecycle {

  private static final Logger log = LoggerFactory.getLogger(McpServerRunner.class);
  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

  private final ServerProperties properties;
  private final List<ToolCallbackProvider> toolCallbackProviders;
  private final MeterRegistry meterRegistry;
  private final HealthEndpoint healthEndpoint;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final JsonMapper jsonMapper;
  private final ApplicationEventPublisher eventPublisher;

  private volatile boolean running;
  private @Nullable McpSyncServer stdioServer;
  private @Nullable McpStatelessSyncServer httpServer;
  private @Nullable WebServer webServer;

  McpServerRunner(
      ServerProperties properties,
      List<ToolCallbackProvider> toolCallbackProviders,
      MeterRegistry meterRegistry,
      HealthEndpoint healthEndpoint,
      PrometheusMeterRegistry prometheusMeterRegistry,
      JsonMapper jsonMapper,
      ApplicationEventPublisher eventPublisher) {
    this.properties = properties;
    this.toolCallbackProviders = toolCallbackProviders;
    this.meterRegistry = meterRegistry;
    this.healthEndpoint = healthEndpoint;
    this.prometheusMeterRegistry = prometheusMeterRegistry;
    this.jsonMapper = jsonMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void start() {
    switch (properties.transport()) {
      case STDIO -> startStdio();
      case HTTP -> startHttp();
    }
    // Spring Boot publishes ReadinessState.ACCEPTING_TRAFFIC once startup completes.
    running = true;
  }

  private void startStdio() {
    stdioServer =
        McpServer.sync(new StdioServerTransportProvider(mcpJsonMapper()))
            .serverInfo(properties.server().name(), properties.server().version())
            .capabilities(capabilities())
            .tools(toolCallbacks().stream().map(McpToolUtils::toSyncToolSpecification).toList())
            .build();
    log.info(
        "MCP server '{}' {} listening on stdio",
        properties.server().name(),
        properties.server().version());
  }

  private void startHttp() {
    ServerProperties.Http http = properties.http();
    HttpServletStatelessServerTransport transport =
        HttpServletStatelessServerTransport.builder()
            .jsonMapper(mcpJsonMapper())
            .messageEndpoint(http.endpoint())
            .securityValidator(
                DefaultServerTransportSecurityValidator.builder()
                    .allowedOrigins(http.allowedOrigins())
                    .allowedHosts(http.allowedHosts())
                    .build())
            .build();
    httpServer =
        McpServer.sync(transport)
            .serverInfo(properties.server().name(), properties.server().version())
            .capabilities(capabilities())
            .tools(
                toolCallbacks().stream()
                    .map(callback -> McpToolUtils.toStatelessSyncToolSpecification(callback, null))
                    .toList())
            // Tool calls block on the SPARQL backend; run them on the servlet request thread.
            .immediateExecution(true)
            .build();

    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(http.port());
    factory.setAddress(resolveAddress(http.address()));
    factory.setShutdown(Shutdown.GRACEFUL);
    DiagnosticsServlet diagnostics =
        new DiagnosticsServlet(healthEndpoint, prometheusMeterRegistry, jsonMapper);
    WebServer server =
        factory.getWebServer(
            servletContext -> {
              servletContext.addServlet("mcp", transport).addMapping(http.endpoint());
              servletContext
                  .addServlet("diagnostics", diagnostics)
                  .addMapping(
                      DiagnosticsServlet.HEALTH_PATH,
                      DiagnosticsServlet.HEALTH_PATH + "/*",
                      DiagnosticsServlet.METRICS_PATH);
            });
    server.start();
    webServer = server;
    log.info(
        "MCP server '{}' {} listening on http://{}:{}{}",
        properties.server().name(),
        properties.server().version(),
        http.address(),
        server.getPort(),
        http.endpoint());
  }

  @Override
  public void stop() {
    // Fail readiness first so load balancers stop routing before requests are drained.
    AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
    WebServer server = webServer;
    if (server != null) {
      CountDownLatch drained = new CountDownLatch(1);
      server.shutDownGracefully(result -> drained.countDown());
      try {
        // Bounded like Spring Boot's default shutdown phase, so a hung SPARQL backend cannot
        // block shutdown; Tomcat aborts whatever is still in flight when it stops.
        if (!drained.await(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
          log.warn("In-flight requests did not complete within {}", SHUTDOWN_TIMEOUT);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      server.stop();
      webServer = null;
    }
    if (httpServer != null) {
      httpServer.closeGracefully();
      httpServer = null;
    }
    if (stdioServer != null) {
      stdioServer.closeGracefully();
      stdioServer = null;
    }
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  /** The bound HTTP port, or -1 when running on stdio. */
  int httpPort() {
    WebServer server = webServer;
    return server == null ? -1 : server.getPort();
  }

  private List<ToolCallback> toolCallbacks() {
    String transport = properties.transport().name().toLowerCase(Locale.ROOT);
    return toolCallbackProviders.stream()
        .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
        .<ToolCallback>map(callback -> new MeteredToolCallback(callback, meterRegistry, transport))
        .toList();
  }

  private static McpSchema.ServerCapabilities capabilities() {
    return McpSchema.ServerCapabilities.builder().tools(false).build();
  }

  /** Same settings Spring AI's MCP server auto-configuration applies to its JSON mapper. */
  private static McpJsonMapper mcpJsonMapper() {
    return new JacksonMcpJsonMapper(
        JsonMapper.builder()
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModules(JacksonUtils.instantiateAvailableModules())
            .changeDefaultPropertyInclusion(
                inclusion ->
                    JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
            .build());
  }

  private static InetAddress resolveAddress(String address) {
    try {
      return InetAddress.getByName(address);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Invalid sparql-mcp.http.address: " + address, e);
    }
  }
}
