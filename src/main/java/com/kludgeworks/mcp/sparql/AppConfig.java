package com.kludgeworks.mcp.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.File;
import java.time.Duration;

@Configuration
@EnableCaching
@ImportRuntimeHints(AppConfig.AppRuntimeHints.class)
public class AppConfig {

	private static final String APP_AUTHOR = "kludgeworks";
	private static final String APP_NAME = "mcp-sparql";
	private static final String CACHE_NAME = "rdfBackendResponses";
	private static final int HEAP_ENTRIES = 1000;
	private static final int DISK_MB = 200;
	private static final Duration TTL = Duration.ofHours(24);

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

	@Bean
	public AppDirs appDirs() {
		return AppDirsFactory.getInstance();
	}

	@Bean(destroyMethod = "close")
	public CacheManager jCacheCacheManager(AppDirs appDirs) {
		EhcacheCachingProvider cachingProvider =
			(EhcacheCachingProvider) Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
		File persistenceDirectory = resolvePersistenceDirectory(appDirs);
		CacheManager cacheManager = createCacheManager(cachingProvider, persistenceDirectory);
		if (cacheManager.getCache(CACHE_NAME) == null) {
			cacheManager.createCache(CACHE_NAME, responseCacheConfiguration());
		}
		return cacheManager;
	}

	private CacheManager createCacheManager(EhcacheCachingProvider cachingProvider, File persistenceDirectory) {
		DefaultConfiguration configuration = new DefaultConfiguration(
			cachingProvider.getDefaultClassLoader(),
			new DefaultPersistenceConfiguration(persistenceDirectory)
		);
		java.net.URI uri = cachingProvider.getDefaultURI();
		return cachingProvider.getCacheManager(uri, configuration);
	}

	private javax.cache.configuration.Configuration<String, String> responseCacheConfiguration() {
		org.ehcache.config.CacheConfiguration<String, String> ehcacheConfig =
			CacheConfigurationBuilder.newCacheConfigurationBuilder(
				String.class,
				String.class,
				ResourcePoolsBuilder.newResourcePoolsBuilder()
					.heap(HEAP_ENTRIES, EntryUnit.ENTRIES)
					.disk(DISK_MB, MemoryUnit.MB, true)
			)
			.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(TTL))
			.build();
		return Eh107Configuration.fromEhcacheCacheConfiguration(ehcacheConfig);
	}

	private File resolvePersistenceDirectory(AppDirs appDirs) {
		String appDirsPath = appDirs.getUserDataDir(APP_NAME, null, APP_AUTHOR);
		return new File(appDirsPath, "ehcache");
	}

	static class AppRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(
				RdfService.PagedResult.class,
				MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
				MemberCategory.INVOKE_PUBLIC_METHODS
			);
			hints.reflection().registerType(
				RdfService.Pagination.class,
				MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
				MemberCategory.INVOKE_PUBLIC_METHODS
			);
		}
	}
}
