package com.kludgeworks.mcp.sparql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CacheConfigTest {

	@Autowired
	private CacheManager cacheManager;

	@Test
	void registersRdfBackendResponsesCache() {
		assertThat(cacheManager.getCache("rdfBackendResponses")).isNotNull();
	}
}
