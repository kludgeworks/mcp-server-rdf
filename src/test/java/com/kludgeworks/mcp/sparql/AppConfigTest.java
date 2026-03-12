package com.kludgeworks.mcp.sparql;

import net.harawata.appdirs.AppDirs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.cache.CacheManager;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigTest {

	@Mock
	AppDirs appDirs;

	@TempDir
	Path tempDir;

	@Test
	void usesAppDirsDirectoryWhenNoOverrideIsConfigured() {
		when(appDirs.getUserDataDir("mcp-sparql", null, "kludgeworks")).thenReturn(tempDir.toString());

		AppConfig appConfig = new AppConfig();
		try (CacheManager cacheManager = appConfig.jCacheCacheManager(appDirs)) {
			assertThat(cacheManager.getCache("rdfBackendResponses")).isNotNull();
		}
	}
}
