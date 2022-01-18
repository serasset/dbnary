package org.getalp.dbnary.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import org.apache.jena.rdf.model.Model;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.mixins.Extractor;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "sample", mixinStandardHelpOptions = true,
    header = "extract the specified pages from a dump and write resulting RDF files to stdout.",
    description = "Process all specified pages and extract lexical data according to options that "
        + "are passed to the program. The extracted lexical data is encoded as RDF graphs using "
        + "ontolex, lexinfo, olia and other standard vocabularies.")
public class GetExtractedSemnet extends Extractor implements Callable<Integer> {

  private static final Logger log = LoggerFactory.getLogger(GetExtractedSemnet.class);

  @Parameters(index = "1..*", description = "The entries to be extracted.", arity = "1..*")
  String[] entries;

  WiktionaryIndex wi;
  IWiktionaryExtractor we;
  IWiktionaryDataHandler wdh;

  protected Integer prepareExtraction() throws WiktionaryIndexerException {
    return setupHandlers(null);
  }

  /**
   * @param args arguments
   * @throws IOException ...
   * @throws WiktionaryIndexerException ...
   */
  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    new CommandLine(new GetExtractedSemnet()).execute(args);
  }


  private Integer extract() {

    for (String entry : entries) {
      String pageContent = wi.getTextOfPage(entry);
      we.extractData(entry, pageContent);
    }

    postProcessAfterExtraction(VersionProvider.getDumpVersion(dumpFile.getName()));

    for (ExtractionFeature f : features.getEndolexFeatures()) {
      if (!f.equals(ExtractionFeature.HDT)) {
        spec.commandLine().getErr().println("----------- " + f + " ----------");
        dumpBox(f, false);
      }
    }

    if (null != features.getExolexFeatures()) {
      for (ExtractionFeature f : features.getExolexFeatures()) {
        if (!f.equals(ExtractionFeature.HDT)) {
          spec.commandLine().getErr().println("----------- EXOLEX: " + f + " ----------");
          dumpBox(f, true);
        }
      }
    }
    return 0;
  }

  public void dumpBox(ExtractionFeature f, boolean isExolex) {
    OutputStream ostream = System.out;
    Model model = isExolex ? wdh.getExolexFeatureBox(f) : wdh.getEndolexFeatureBox(f);
    try {
      wdh.dump(model, new PrintStream(ostream, false, "UTF-8"), features.getOutputFormat());
    } catch (IOException e) {
      System.err.println(
          "Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
      e.printStackTrace(System.err);
    } finally {
      if (null != ostream) {
        try {
          ostream.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  public Integer call() {
    Integer returnCode = null;
    try {
      returnCode = prepareExtraction();
    } catch (WiktionaryIndexerException e) {
      spec.commandLine().getErr().println("Could not read dump.");
      e.printStackTrace(spec.commandLine().getErr());
      return -1;
    }
    if (returnCode != 0)
      return returnCode;
    return extract();
  }

}
