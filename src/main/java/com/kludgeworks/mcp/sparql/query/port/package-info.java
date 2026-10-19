/**
 * What the query application ring ({@code SparqlQueries}) needs from the outside world. The
 * outbound adapters in {@code backend} and {@code cache} implement these, so they depend inwards
 * and never on each other.
 */
@org.jspecify.annotations.NullMarked
package com.kludgeworks.mcp.sparql.query.port;
