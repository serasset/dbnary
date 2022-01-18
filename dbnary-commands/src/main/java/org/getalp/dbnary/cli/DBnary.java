package org.getalp.dbnary.cli;

import java.nio.file.Path;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.slf4j.impl.SimpleLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(name = "dbnary",
    subcommands = {CheckWiktionarySyntaxQuality.class, ExtractWiktionary.class, HelpCommand.class,
        UpdateAndExtractDumps.class},
    mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
    description = "DBnary is a set of tools used to extract lexical data from several "
        + "editions of wiktionaries. All extracted data is made available as Linked Open Data, "
        + "using ontolex, lexinfo, olia and several other specialized vocabularies.",
    commandListHeading = "%nCommands:%n%nThe dbnary commands are:%n", showAtFileInUsageHelp = true)
public class DBnary {

  @Spec
  private CommandSpec spec;

  @Option(names = "--dir", scope = ScopeType.INHERIT, defaultValue = ".")
  public Path dbnaryDir;

  @Option(names = {"-v"}, scope = ScopeType.INHERIT, description = "Print extra information.")
  private boolean verbose;

  @Option(names = "--trace", scope = ScopeType.INHERIT) // option is shared with subcommands
  public void setTrace(String[] classes) {
    // Configure the slf4j-simple logger level for the specified parameters
    for (String clazz : classes) {
      spec.commandLine().getOut().println("Enhabling trace for " + clazz);
      System.setProperty(SimpleLogger.LOG_FILE_KEY + "org.getalp.dbnary." + clazz, "trace");
      System.setProperty(SimpleLogger.LOG_FILE_KEY + clazz, "trace");
    }
  }

  @Option(names = "--debug", scope = ScopeType.INHERIT) // option is shared with subcommands
  public void setDebug(String[] classes) {
    // Configure the slf4j-simple logger level for the specified parameters
    for (String clazz : classes) {
      spec.commandLine().getOut().println("Enhabling debug for " + clazz);
      System.setProperty(SimpleLogger.LOG_FILE_KEY + "org.getalp.dbnary." + clazz, "debug");
      System.setProperty(SimpleLogger.LOG_FILE_KEY + clazz, "debug");
    }
  }

  public Path getDbnaryDir() {
    return dbnaryDir;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new DBnary());
    int exitCode = cmd.execute(args);
  }

}
