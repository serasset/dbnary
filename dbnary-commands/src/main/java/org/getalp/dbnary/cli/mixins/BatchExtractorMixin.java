package org.getalp.dbnary.cli.mixins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.getalp.dbnary.cli.DBnary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

public class BatchExtractorMixin {
  private static final Logger log = LoggerFactory.getLogger(BatchExtractorMixin.class);

  @Spec(Spec.Target.MIXEE)
  CommandSpec mixee;
  @ParentCommand
  private DBnary parent;

  @Option(names = {"--no-compress"}, negatable = true,
      description = "Compress the resulting extracted files using BZip2. set by default.")
  private boolean compress = true;

  @Option(names = {"-F", "--frompage"}, paramLabel = "NUMBER", defaultValue = "-1",
      description = "Begin the extraction at the specified page number.")
  private int fromPage = 0;

  @Option(names = {"-T", "--topage"}, paramLabel = "NUMBER",
      description = "Stop the extraction at the specified page number.")
  private int toPage = Integer.MAX_VALUE;

  @Option(names = {"--no-tdb"}, negatable = true,
      description = "Use TDB2 (temporary file storage for extracted models, usefull/necessary for big dumps. set by default.")
  private boolean useTdb = true;

  // non parameters
  private String tdbDir = null;

  public void createTDBTempDirectory() throws IOException {
    try {
      Path temp = Files.createTempDirectory("dbnary");
      temp.toFile().deleteOnExit();
      tdbDir = temp.toAbsolutePath().toString();
      if (parent.isVerbose()) {
        mixee.commandLine().getErr().println("Using temp TDB at " + tdbDir);
      }
      log.debug("Using TDB in {}", tdbDir);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          FileUtils.deleteDirectory(temp.toFile());
        } catch (IOException e) {
          mixee.commandLine().getErr().println("Caught " + e.getClass()
              + " when attempting to delete the temporary TDB directory " + tdbDir);
          mixee.commandLine().getErr().println(e.getLocalizedMessage());
        }
      }));
    } catch (IOException e) {
      mixee.commandLine().getErr().println("Could not create temporary TDB directory. Exiting...");
      e.printStackTrace(mixee.commandLine().getErr());
      throw e;
    }
  }

  public boolean doCompress() {
    return compress;
  }

  public int fromPage() {
    return fromPage;
  }

  public int toPage() {
    return toPage;
  }

  public boolean useTdb() {
    return useTdb;
  }

  public String tdbDir() {
    return tdbDir;
  }
}
