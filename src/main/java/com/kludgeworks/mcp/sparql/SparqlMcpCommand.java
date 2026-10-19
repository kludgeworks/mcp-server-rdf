package com.kludgeworks.mcp.sparql;

import com.kludgeworks.mcp.sparql.server.ServerProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

/**
 * Command-line options. Help and version are answered before Spring starts, so they are instant and
 * never open the stdio transport.
 */
@Command(
    name = "sparql-mcp",
    mixinStandardHelpOptions = true,
    versionProvider = SparqlMcpCommand.VersionProvider.class,
    description = "MCP server that runs SPARQL queries against RDF endpoints.",
    footer = {
      "",
      "Any other --<property>=<value> argument is passed to Spring Boot, for example",
      "--sparql-mcp.http.port=9090. See the README for all properties."
    })
class SparqlMcpCommand implements Callable<Integer> {

  // Spring properties are dotted (sparql-mcp.http.port, logging.level.root, ...). Requiring a dot
  // lets typos such as --transprot=http fail instead of being silently ignored.
  private static final Pattern SPRING_PROPERTY = Pattern.compile("--[\\w-]+(\\.[\\w\\[\\]-]+)+=.*");

  @Option(
      names = {"-t", "--transport"},
      paramLabel = "<transport>",
      description = "stdio (default) or http. Overrides SPARQL_MCP_TRANSPORT.")
  private ServerProperties.@Nullable Transport transport;

  @Unmatched private List<String> unmatched = new ArrayList<>();

  @SuppressWarnings("NullAway.Init") // injected by picocli
  @Spec
  private CommandSpec spec;

  private @Nullable List<String> springArguments;

  static CommandLine commandLine(SparqlMcpCommand command) {
    return new CommandLine(command).setCaseInsensitiveEnumValuesAllowed(true);
  }

  /**
   * Runs after successful parsing, but only records the Spring arguments. {@link Application}
   * starts Spring outside picocli, so Spring's own exceptions propagate unchanged, including the
   * one its AOT processing uses to end the run during the native build.
   */
  @Override
  public Integer call() {
    List<String> arguments = new ArrayList<>();
    if (transport != null) {
      arguments.add("--sparql-mcp.transport=" + transport.name().toLowerCase(Locale.ROOT));
    }
    for (String argument : unmatched) {
      if (!SPRING_PROPERTY.matcher(argument).matches()) {
        throw new ParameterException(spec.commandLine(), "Unknown option: '" + argument + "'");
      }
      arguments.add(argument);
    }
    springArguments = arguments;
    return 0;
  }

  /**
   * The arguments to start Spring with: the typed options as properties plus pass-through ones.
   * Null when the server should not start, for example after {@code --help} or {@code --version}.
   */
  @Nullable List<String> springArguments() {
    return springArguments;
  }

  static class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[] {"sparql-mcp " + Application.version()};
    }
  }
}
