package com.kludgeworks.mcp.sparql;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  /** Fails on dependency cycles between modules or on access to another module's internals. */
  @Test
  void verifiesModuleStructure() {
    ApplicationModules.of(Application.class).verify();
  }
}
