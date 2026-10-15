package com.kludgeworks.mcp.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class SparqlMcpCommandTest {

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  private final SparqlMcpCommand command = new SparqlMcpCommand();

  // execute() never starts Spring; it only parses, validates and records the Spring arguments.
  private int execute(String... args) {
    return SparqlMcpCommand.commandLine(command)
        .setOut(new PrintWriter(out))
        .setErr(new PrintWriter(err))
        .execute(args);
  }

  @Test
  void printsVersion() {
    assertThat(execute("--version")).isZero();
    assertThat(Application.version()).isNotEqualTo("unknown");
    assertThat(out.toString().trim()).isEqualTo("sparql-mcp " + Application.version());
    assertThat(command.springArguments()).isNull();
  }

  @Test
  void printsUsage() {
    assertThat(execute("--help")).isZero();
    assertThat(out.toString()).contains("--transport", "--help", "--version", "--sparql-mcp.");
    assertThat(command.springArguments()).isNull();
  }

  @Test
  void mapsTransportAndPassesSpringPropertiesThrough() {
    assertThat(execute("-t", "HTTP", "--sparql-mcp.http.port=9090")).isZero();
    assertThat(command.springArguments())
        .containsExactly("--sparql-mcp.transport=http", "--sparql-mcp.http.port=9090");
  }

  @Test
  void startsWithNoArgumentsByDefault() {
    assertThat(execute()).isZero();
    assertThat(command.springArguments()).isEmpty();
  }

  @Test
  void rejectsUnknownOption() {
    assertThat(execute("--transprot=http")).isEqualTo(2);
    assertThat(err.toString()).contains("Unknown option: '--transprot=http'");
  }

  @Test
  void rejectsInvalidTransport() {
    assertThat(execute("--transport=ftp")).isEqualTo(2);
    assertThat(err.toString()).contains("--transport");
  }
}
