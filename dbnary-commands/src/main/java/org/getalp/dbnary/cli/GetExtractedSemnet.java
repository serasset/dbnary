package org.getalp.dbnary.cli;

import com.github.jsonldjava.shaded.com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.jena.rdf.model.Model;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.cli.mixins.Extractor;
import org.getalp.dbnary.cli.utils.NoWiktionaryExtractorException;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.getalp.wiktionary.WiktionaryIndexerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "sample", mixinStandardHelpOptions = true,
    header = "extract the specified pages from a dump and write resulting RDF files to stdout.",
    description = "Process all specified pages and extract lexical data according to options that "
        + "are passed to the program. The extracted lexical data is encoded as RDF graphs using "
        + "ontolex, lexinfo, olia and other standard vocabularies.")
public class GetExtractedSemnet extends Extractor implements Callable<Integer> {
  private static final Logger log = LoggerFactory.getLogger(GetExtractedSemnet.class);

  @Parameters(index = "1..*", description = "The entries to be extracted.", arity = "0..*")
  String[] entries;

  String[] entriesFromFile = null;

  @Option(names = {"--entries"},
      description = "take the list of entries from the specified file instead of remaining arguments.",
      arity = "1")
  protected void getEntriesFromFile(Path entriesFile) {
    try {
      entriesFromFile =
          Files.readLines(entriesFile.toFile(), Charset.defaultCharset()).toArray(new String[0]);
    } catch (IOException e) {
      throw new RuntimeException("Could not read the list of entries.", e);
    }
  }

  protected Integer prepareExtraction() throws NoWiktionaryExtractorException {
    return setupHandlers(null);
  }

  private Integer extract() {

    if (null != entriesFromFile) {
      entries = entriesFromFile;
    }
    for (String entry : entries) {
      String pageContent = wi.getTextOfPage(entry);
      try {
        we.extractData(entry, pageContent);
      } catch (RuntimeException | StackOverflowError e) {
        spec.commandLine().getErr().println(
            "Runtime exception while extracting  page<<" + entry + ">>, proceeding to next pages.");
        if (log.isDebugEnabled())
          e.printStackTrace(spec.commandLine().getErr());
        spec.commandLine().getErr().println(e.getMessage());
      }
    }

    postProcessAfterExtraction(VersionProvider.getDumpVersion(wi.getDumpFile().getName()));

    for (ExtractionFeature f : features.getEndolexFeatures()) {
      if (!f.equals(ExtractionFeature.HDT)) {
        System.out.println("----------- " + f + " ----------");
        dumpBox(f, false, System.out);
      }
    }

    if (null != features.getExolexFeatures()) {
      for (ExtractionFeature f : features.getExolexFeatures()) {
        if (!f.equals(ExtractionFeature.HDT)) {
          System.out.println("----------- EXOLEX: " + f + " ----------");
          dumpBox(f, true, System.out);
        }
      }
    }
    cleanupHandlers();
    return 0;
  }

  public void dumpBox(ExtractionFeature f, boolean isExolex, PrintStream ostream) {
    Model model = isExolex ? wdh.getExolexFeatureBox(f) : wdh.getEndolexFeatureBox(f);
    try {
      wdh.dump(model, ostream, features.getOutputFormat());
    } finally {
      if (null != ostream) {
        ostream.flush();
      }
    }
  }


  public Integer call() {
    Integer returnCode;
    try {
      returnCode = prepareExtraction();
    } catch (NoWiktionaryExtractorException e) {
      spec.commandLine().getErr().println("Could not read dump.");
      // e.printStackTrace(spec.commandLine().getErr());
      return -1;
    }
    if (returnCode != 0)
      return returnCode;
    return extract();
  }

}
