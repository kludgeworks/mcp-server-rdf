package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VersionFlagTest {

  @Test
  void printsVersionAndExitsWithoutStartingTheServer() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream original = System.out;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    try {
      // Returns instead of blocking on stdin, which is what the Homebrew formula test relies on.
      Application.main(new String[] {"--version"});
    } finally {
      System.setOut(original);
    }

    assertThat(Application.version()).isNotEqualTo("unknown");
    assertThat(out.toString(StandardCharsets.UTF_8).trim())
        .isEqualTo("sparql-mcp " + Application.version());
  }
}
