package org.getalp.dbnary.cli.utils;

import java.io.PrintWriter;
import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

  public int handleParseException(ParameterException ex, String[] args) {
    CommandLine cmd = ex.getCommandLine();
    PrintWriter err = cmd.getErr();

    // if tracing at DEBUG level, show the location of the issue
    if ("DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"))) {
      err.println(cmd.getColorScheme().stackTraceText(ex));
    }

    err.println(cmd.getColorScheme().errorText(ex.getMessage())); // bold red
    UnmatchedArgumentException.printSuggestions(ex, err);
    err.print(cmd.getHelp().fullSynopsis());

    CommandSpec spec = cmd.getCommandSpec();
    err.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

    return cmd.getExitCodeExceptionMapper() != null
        ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
        : spec.exitCodeOnInvalidInput();
  }
}
