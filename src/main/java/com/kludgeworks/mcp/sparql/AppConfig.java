package com.kludgeworks.mcp.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableCaching
public class AppConfig {

	@Bean
	public ToolCallbackProvider rdfTools(RdfService rdfService) {
		return MethodToolCallbackProvider.builder().toolObjects(rdfService).build();
	}

	@Bean
	public RestClient restClient() {
		return RestClient.create();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
