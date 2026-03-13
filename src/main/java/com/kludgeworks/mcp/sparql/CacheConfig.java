package com.kludgeworks.mcp.sparql;

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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.File;
import java.time.Duration;

@Configuration
@EnableCaching
@ImportRuntimeHints(CacheConfig.CacheRuntimeHints.class)
public class CacheConfig {

	private static final String CACHE_NAME = "rdfBackendResponses";
	private static final int HEAP_ENTRIES = 1000;
	private static final int DISK_MB = 200;
	private static final Duration TTL = Duration.ofHours(24);

	@Bean
	public AppDirs appDirs() {
		return AppDirsFactory.getInstance();
	}

	@Bean(destroyMethod = "close")
	public CacheManager jCacheCacheManager(AppDirs appDirs, AppMetadata appMetadata) {
		EhcacheCachingProvider cachingProvider =
			(EhcacheCachingProvider) Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
		File persistenceDirectory = resolvePersistenceDirectory(appDirs, appMetadata);
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

	private File resolvePersistenceDirectory(AppDirs appDirs, AppMetadata appMetadata) {
		String appDirsPath = appDirs.getUserDataDir(appMetadata.name(), appMetadata.version(), appMetadata.author());
		return new File(appDirsPath, "ehcache");
	}

	static class CacheRuntimeHints implements RuntimeHintsRegistrar {

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
