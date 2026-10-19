package com.kludgeworks.mcp.sparql.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StdioTransportTest {

  @Autowired private McpServerRunner runner;

  @Test
  void runsOnStdioWithoutWebServerByDefault() {
    assertThat(runner.isRunning()).isTrue();
    assertThat(runner.httpPort()).isEqualTo(-1);
  }
}
