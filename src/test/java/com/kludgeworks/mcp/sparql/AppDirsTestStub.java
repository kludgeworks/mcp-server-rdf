package com.kludgeworks.mcp.sparql;

import net.harawata.appdirs.AppDirs;

import java.nio.file.Path;

final class AppDirsTestStub extends AppDirs {

	private final Path rootPath;

	AppDirsTestStub(Path rootPath) {
		this.rootPath = rootPath;
	}

	@Override
	public String getUserDataDir(String appName, String appVersion, String appAuthor, boolean roaming) {
		return rootPath.toString();
	}

	@Override
	public String getUserConfigDir(String appName, String appVersion, String appAuthor, boolean roaming) {
		return rootPath.resolve("config").toString();
	}

	@Override
	public String getUserCacheDir(String appName, String appVersion, String appAuthor) {
		return rootPath.resolve("cache").toString();
	}

	@Override
	public String getSiteDataDir(String appName, String appVersion, String appAuthor, boolean multipath) {
		return rootPath.resolve("site-data").toString();
	}

	@Override
	public String getSiteConfigDir(String appName, String appVersion, String appAuthor, boolean multipath) {
		return rootPath.resolve("site-config").toString();
	}

	@Override
	public String getUserLogDir(String appName, String appVersion, String appAuthor) {
		return rootPath.resolve("log").toString();
	}

	@Override
	public String getUserDownloadsDir(String appName, String appVersion, String appAuthor) {
		return rootPath.resolve("downloads").toString();
	}

	@Override
	public String getSharedDir(String appName, String appVersion, String appAuthor) {
		return rootPath.resolve("shared").toString();
	}
}
