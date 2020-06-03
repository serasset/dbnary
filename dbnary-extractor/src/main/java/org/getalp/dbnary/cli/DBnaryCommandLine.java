package org.getalp.dbnary.cli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class DBnaryCommandLine {

  protected static final String VERBOSE_OPTION = "v";
  private static final Pattern DUMP_VERSION_PATTERN = Pattern.compile("(20\\d\\d\\d{4}|20\\d\\d_\\d{2}_\\d{2})");
  protected static final Options options = new Options(); // Command line options
  protected CommandLine cmd = null; // Command Line arguments
  protected boolean verbose;

  static {
    options.addOption("h", "help", false, "Prints usage and exits. ");
    options.addOption(VERBOSE_OPTION, false, "Be verbose on what I do... ");
  }

  public DBnaryCommandLine(String[] args) {
    CommandLineParser parser = new DefaultParser();
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
      printUsage();
      System.exit(1);
    }
    this.cmd = cmd;
    this.loadSharedArgs();
  }

  /**
   * Validate and parse arguments. Exits after printing usage if anything is astray.
   */
  private void loadSharedArgs() {
    // Check for args
    if (cmd.hasOption("h")) {
      printUsage();
      System.exit(0);
    }

    verbose = cmd.hasOption(VERBOSE_OPTION);
  }

  protected static String getDumpVersion(String dumpFileName) {
    Matcher m = DUMP_VERSION_PATTERN.matcher(dumpFileName);
    if (m.find()) {
      return m.group();
    } else {
      return dumpFileName;
    }
  }

  protected void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar " + this.getClass().getCanonicalName() + " [OPTIONS] dumpFile",
        "With OPTIONS in:", options,
        "dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index.",
        false);
  }
}
