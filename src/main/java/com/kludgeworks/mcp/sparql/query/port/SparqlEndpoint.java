package com.kludgeworks.mcp.sparql.query.port;

import java.io.IOException;

/** Runs SPARQL queries against remote endpoints. */
public interface SparqlEndpoint {

  /** Returns the response body; never blank. */
  String execute(SparqlRequest request) throws IOException;
}
