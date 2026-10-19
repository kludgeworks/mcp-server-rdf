package com.kludgeworks.mcp.sparql.platform;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PlatformConfig {

  @Bean
  AppMetadata appMetadata(
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
