package org.getalp.dbnary.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class VerboseCommand {

  protected static final String VERBOSE_OPTION = "v";
  protected static final Options options = new Options(); // Command line options
  protected boolean verbose = false;
  protected String[] remainingArgs;

  static {
    options.addOption("h", false, "Prints usage and exits. ");
    options.addOption(VERBOSE_OPTION, false, "ask the command to be verbose in its progress.");
  }

  /**
   * Validate and set command line arguments. Exit after printing usage if anything is astray
   *
   * @param args
   */
  protected void loadArgs(String[] args) {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
      remainingArgs = cmd.getArgs();
      if (cmd.hasOption("h")) {
        printUsage();
        System.exit(0);
      }
      this.verbose = cmd.hasOption(VERBOSE_OPTION);
      this.loadArgs(cmd);
    } catch (ParseException e) {
      System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
      printUsage();
      System.exit(1);
    }
  }

  /**
   * Load additional command arguments.
   * 
   * @param cmd The parsed command options
   */
  protected abstract void loadArgs(CommandLine cmd);

  protected abstract void printUsage();
}
