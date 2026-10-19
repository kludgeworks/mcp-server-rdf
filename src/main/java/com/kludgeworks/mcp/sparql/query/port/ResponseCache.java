package com.kludgeworks.mcp.sparql.query.port;

import java.io.IOException;

/** Caches SPARQL responses so repeated queries do not hit the endpoint again. */
public interface ResponseCache {

  /**
   * Returns the cached response for {@code request}, or calls {@code loader} and caches its result.
   * Caching is best effort: when the cache cannot be read or written, the loader still runs.
   */
  String get(SparqlRequest request, Loader loader) throws IOException;

  @FunctionalInterface
  interface Loader {
    String load() throws IOException;
  }
}
