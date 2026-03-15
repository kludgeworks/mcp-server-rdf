package com.kludgeworks.mcp.sparql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(AppDirsTestConfig.Config.class)
class CacheConfigTest extends AppDirsTestConfig {

	@Autowired
	private CacheManager cacheManager;

	@Test
	void registersRdfBackendResponsesCache() {
		assertThat(cacheManager.getCache("rdfBackendResponses")).isNotNull();
	}
}
