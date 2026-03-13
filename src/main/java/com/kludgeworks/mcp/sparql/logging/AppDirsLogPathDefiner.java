package com.kludgeworks.mcp.sparql.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppDirsLogPathDefiner extends PropertyDefinerBase {

	private String appName;
	private String appAuthor;
	private String appVersion;

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setAppAuthor(String appAuthor) {
		this.appAuthor = appAuthor;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	@Override
	public String getPropertyValue() {
		Properties buildInfo = loadBuildInfo();
		String effectiveAppName = valueOrDefault(appName, "mcp-sparql");
		String effectiveAppAuthor = valueOrDefault(appAuthor, buildInfo.getProperty("build.group", "kludgeworks"));
		String effectiveAppVersion = normalizeVersion(valueOrDefault(appVersion, buildInfo.getProperty("build.version")));

		AppDirs appDirs = AppDirsFactory.getInstance();
		String logDirPath = appDirs.getUserLogDir(effectiveAppName, effectiveAppVersion, effectiveAppAuthor);
		File logDir = new File(logDirPath);
		if (!logDir.exists() && !logDir.mkdirs()) {
			throw new IllegalStateException("Could not create log directory: " + logDir.getAbsolutePath());
		}
		return logDir.getAbsolutePath();
	}

	private Properties loadBuildInfo() {
		Properties properties = new Properties();
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/build-info.properties")) {
			if (stream != null) {
				properties.load(stream);
			}
		}
		catch (IOException ignored) {
			// Build metadata is optional in some test/dev flows.
		}
		return properties;
	}

	private String valueOrDefault(String value, String fallback) {
		return (value == null || value.isBlank()) ? fallback : value;
	}

	private String normalizeVersion(String version) {
		return (version == null || version.isBlank() || "unknown".equalsIgnoreCase(version)) ? null : version;
	}
}
