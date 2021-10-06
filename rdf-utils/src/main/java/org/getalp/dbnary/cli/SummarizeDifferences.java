package org.getalp.dbnary.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

public class SummarizeDifferences extends VerboseCommand {

  public SummarizeDifferences(String[] args) {
    this.loadArgs(args);
  }

  @Override
  protected void loadArgs(CommandLine cmd) {
    if (remainingArgs.length != 1) {
      printUsage();
      System.exit(1);
    }
  }

  @Override
  protected void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar "
            + this.getClass().getCanonicalName() + " [OPTIONS] diffFolder",
        "With OPTIONS in:", options,
        "diffFolder should contains turtle files describing the differences computed. "
            + "Each file should be named <lg>_{gain,lost}_<model>.ttl "
            + "  - where <lg> is the 2 letter language code"
            + "  - and model is one of: ontolex, morphology, etymology, etc.",
        false);
  }

  public static void main(String[] args) {
    SummarizeDifferences cli = new SummarizeDifferences(args);
    cli.summarize();
  }


  private void summarize() {
    try (Stream<Path> stream = Files.list(Paths.get(remainingArgs[0]))) {
      stream.map(String::valueOf).filter(path -> path.endsWith(".ttl"))
          .forEach(System.out::println);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
