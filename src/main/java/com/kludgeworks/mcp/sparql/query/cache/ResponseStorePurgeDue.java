package com.kludgeworks.mcp.sparql.query.cache;

/**
 * Published after enough writes to the on-disk store that expired and surplus rows should be
 * removed. {@link ResponseStorePurger} handles it in the background.
 */
record ResponseStorePurgeDue() {}
