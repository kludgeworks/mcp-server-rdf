package com.kludgeworks.mcp.sparql;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
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

}
