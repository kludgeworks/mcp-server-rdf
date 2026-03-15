package com.kludgeworks.mcp.sparql;

import net.harawata.appdirs.AppDirs;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;

abstract class AppDirsTestConfig {

	@TempDir(cleanup = CleanupMode.NEVER)
	protected static Path tempDir;

	@TestConfiguration
	static class Config {

		@Bean
		@Primary
		AppDirs testAppDirs() {
			return new AppDirsTestStub(tempDir.resolve("spring-tests"));
		}
	}
}
