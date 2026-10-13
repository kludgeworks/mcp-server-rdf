package com.kludgeworks.mcp.sparql;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Records call latency, outcome and response size for an MCP tool. */
final class MeteredToolCallback implements ToolCallback {

  static final String CALLS_METRIC = "mcp.tool.calls";
  static final String RESPONSE_METRIC = "mcp.tool.response";

  private final ToolCallback delegate;
  private final MeterRegistry meterRegistry;
  private final String transport;

  MeteredToolCallback(ToolCallback delegate, MeterRegistry meterRegistry, String transport) {
    this.delegate = delegate;
    this.meterRegistry = meterRegistry;
    this.transport = transport;
  }

  @Override
  public ToolDefinition getToolDefinition() {
    return delegate.getToolDefinition();
  }

  @Override
  public ToolMetadata getToolMetadata() {
    return delegate.getToolMetadata();
  }

  @Override
  public String call(String toolInput) {
    return record(() -> delegate.call(toolInput));
  }

  @Override
  public String call(String toolInput, @Nullable ToolContext toolContext) {
    return record(() -> delegate.call(toolInput, toolContext));
  }

  private String record(Supplier<String> call) {
    String tool = delegate.getToolDefinition().name();
    Timer.Sample sample = Timer.start(meterRegistry);
    String outcome = "error";
    try {
      String result = call.get();
      outcome = "success";
      DistributionSummary.builder(RESPONSE_METRIC)
          .description("Size of MCP tool responses")
          .baseUnit("chars")
          .tag("tool", tool)
          .tag("transport", transport)
          .register(meterRegistry)
          .record(result.length());
      return result;
    } finally {
      sample.stop(
          Timer.builder(CALLS_METRIC)
              .description("MCP tool invocations")
              .tag("tool", tool)
              .tag("outcome", outcome)
              .tag("transport", transport)
              .register(meterRegistry));
    }
  }
}
