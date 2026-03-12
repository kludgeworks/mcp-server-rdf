package com.kludgeworks.mcp.sparql;

import net.harawata.appdirs.AppDirs;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.util.UUID;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(ApplicationTests.TestAppDirsConfiguration.class)
class ApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestAppDirsConfiguration {

		@Bean
		@Primary
		AppDirs appDirs() {
			AppDirs appDirs = Mockito.mock(AppDirs.class);
			String isolatedPath = new File(
				System.getProperty("java.io.tmpdir"),
				"mcp-sparql-test-" + UUID.randomUUID()
			).getAbsolutePath();
			Mockito.when(appDirs.getUserDataDir("mcp-sparql", null, "kludgeworks")).thenReturn(isolatedPath);
			return appDirs;
		}
	}

}
