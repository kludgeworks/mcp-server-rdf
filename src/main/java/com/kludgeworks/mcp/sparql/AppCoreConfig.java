package com.kludgeworks.mcp.sparql;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppCoreConfig {

  @Bean
  public ToolCallbackProvider rdfTools(RdfService rdfService) {
    return MethodToolCallbackProvider.builder().toolObjects(rdfService).build();
  }

  @Bean
  public RestClient restClient(RestClient.Builder restClientBuilder) {
    return restClientBuilder.build();
  }

  @Bean
  public AppMetadata appMetadata(
      @Value("${spring.application.name}") String applicationName,
      ObjectProvider<BuildProperties> buildPropertiesProvider) {
    @Nullable BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
    String author =
        buildProperties != null
            ? Objects.requireNonNullElse(buildProperties.getGroup(), "kludgeworks")
            : "kludgeworks";
    String version =
        buildProperties != null
            ? Objects.requireNonNullElse(buildProperties.getVersion(), "unknown")
            : "unknown";
    return new AppMetadata(applicationName, author, version);
  }
}
