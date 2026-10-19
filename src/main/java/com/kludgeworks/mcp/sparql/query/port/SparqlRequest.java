package com.kludgeworks.mcp.sparql.query.port;

import org.springframework.http.MediaType;

/** A SPARQL query to run against an endpoint, asking for a response in {@code acceptType}. */
public record SparqlRequest(String serviceUrl, String query, MediaType acceptType) {}
