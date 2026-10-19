package com.kludgeworks.mcp.sparql.query.cache;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Purges the on-disk store off the request thread, so a query never waits for it. */
@Component
class ResponseStorePurger {

  private static final Logger log = LoggerFactory.getLogger(ResponseStorePurger.class);

  private final TwoLevelResponseCache cache;
  private final AtomicBoolean purging = new AtomicBoolean();

  ResponseStorePurger(TwoLevelResponseCache cache) {
    this.cache = cache;
  }

  @Async
  @EventListener
  void on(ResponseStorePurgeDue event) {
    SqliteResponseStore store = cache.store();
    // A purge that is already running covers this request too.
    if (store == null || !purging.compareAndSet(false, true)) {
      return;
    }
    try {
      store.purge();
    } catch (SQLException e) {
      log.warn("Could not purge the response cache at {}: {}", store.file(), e.getMessage());
    } finally {
      purging.set(false);
    }
  }
}
