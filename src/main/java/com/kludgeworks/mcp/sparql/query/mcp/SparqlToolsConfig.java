package com.kludgeworks.mcp.sparql.query.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publishes the SPARQL tools; the MCP server picks up every {@link ToolCallbackProvider} bean. */
@Configuration
class SparqlToolsConfig {

  @Bean
  ToolCallbackProvider sparqlToolCallbacks(SparqlTools tools) {
    return MethodToolCallbackProvider.builder().toolObjects(tools).build();
  }
}
