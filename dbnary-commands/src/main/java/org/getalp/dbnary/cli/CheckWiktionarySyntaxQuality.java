package org.getalp.dbnary.cli;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.LangTools;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexer;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;


@Command(name = "check-wiki-syntax", mixinStandardHelpOptions = true, helpCommand = true,
    versionProvider = VersionProvider.class)
public class CheckWiktionarySyntaxQuality implements Callable<Integer> {

  private final Logger log = LoggerFactory.getLogger(CheckWiktionarySyntaxQuality.class);

  private static final XMLInputFactory2 xmlif;
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String DEFAULT_OUTPUT_FILE = "extract";

  @Spec
  CommandSpec spec; // injected by picocli

  @Option(names = {"-o", "--output"}, paramLabel = "OUTPUT-FILE",
      defaultValue = DEFAULT_OUTPUT_FILE,
      description = "file in which the result of the check is written; Default: ${DEFAULT-VALUE}")
  private String outputFile = DEFAULT_OUTPUT_FILE;

  private String language = DEFAULT_LANGUAGE;

  @Option(names = {"-l", "--language"}, paramLabel = "LANGUAGE", defaultValue = DEFAULT_LANGUAGE,
      description = "language code of the wiki to be checked; Default: ${DEFAULT-VALUE}")
  public void setLanguage(String language) {
    this.language = LangTools.getCode(language);
    if (null == this.language) {
      throw new ParameterException(spec.commandLine(), String.format(
          "Invalid language '%s' for option '--language': " + "unknown language code.", language));
    }
  }

  @Option(names = {"-F", "--frompage"}, paramLabel = "NUMBER",
      description = "Begin the check at the specified page number")
  private int fromPage = 0;

  @Option(names = {"-T", "--topage"}, paramLabel = "NUMBER",
      description = "Begin the check at the specified page number")
  private int toPage = Integer.MAX_VALUE;

  @Option(names = {"-v"}, description = "Print extra information before checking")
  private boolean verbose;

  @Parameters(index = "0", description = "The dump file of the wiki to be checked.")
  private File dumpFile;

  WiktionaryIndex wi;

  static {
    try {
      xmlif = (XMLInputFactory2) XMLInputFactory2.newInstance();
      xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
      xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
      xmlif.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
    } catch (Exception ex) {
      System.err.println("Cannot intialize XMLInputFactory while classloading WiktionaryIndexer.");
      throw new RuntimeException("Cannot initialize XMLInputFactory", ex);
    }
  }

  /**
   * @param args arguments
   * @throws IOException ...
   * @throws WiktionaryIndexerException ...
   */
  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    int exitCode = new CommandLine(new CheckWiktionarySyntaxQuality()).execute(args);
    System.exit(exitCode);
  }

  /**
   * Analyse command line arguments to prepare processing.
   *
   * @throws WiktionaryIndexerException ..
   */

  public Integer call() throws IOException, WiktionaryIndexerException {
    wi = new WiktionaryIndex(dumpFile);
    if (verbose) {
      System.err.println("Checking Syntax Quality on Wiktionary Dump:");
      System.err.println("  Language: " + language);
      System.err.println("  Dump: " + dumpFile);
      System.err.println("  Ontolex : " + outputFile);
    }

    // create new XMLStreamReader
    long startTime = System.currentTimeMillis();
    long totalRelevantTime = 0, relevantStartTime = 0, relevantTimeOfLastThousands;
    int nbPages = 0, nbRelevantPages = 0;
    relevantTimeOfLastThousands = System.currentTimeMillis();

    XMLStreamReader2 xmlr = null;
    try {
      // pass the file name. all relative entity references will be
      // resolved against this as base URI.
      xmlr = xmlif.createXMLStreamReader(dumpFile);

      // check if there are more events in the input stream
      String title = "";
      String page = "";
      while (xmlr.hasNext()) {
        xmlr.next();
        if (xmlr.isStartElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
          title = "";
          page = "";
        } else if (xmlr.isStartElement()
            && xmlr.getLocalName().equals(WiktionaryIndexer.titleTag)) {
          title = xmlr.getElementText();
        } else if (xmlr.isStartElement() && xmlr.getLocalName().equals("text")) {
          page = xmlr.getElementText();
        } else if (xmlr.isEndElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
          if (!title.equals("")) {
            if (title.contains(":"))
              continue;
            nbPages++;
            if (nbPages < fromPage) {
              continue;
            }
            if (nbPages > toPage) {
              break;
            }
            try {
              // System.err.println(title);
              WikiText text = new WikiText(title, page);
              text.content();
            } catch (RuntimeException e) {
              System.err.println("Runtime exception while extracting  page<<" + title
                  + ">>, proceeding to next pages.");
              System.err.println(e.getMessage());
              e.printStackTrace();
            }
            totalRelevantTime = (System.currentTimeMillis() - startTime);
            nbRelevantPages++;
            if (nbRelevantPages % 1000 == 0) {
              System.err.println("Extracted: " + nbRelevantPages + " pages in: "
                  + formatHMS(totalRelevantTime) + " / Average = "
                  + (totalRelevantTime / nbRelevantPages) + " ms/extracted page ("
                  + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000 + " ms) ("
                  + nbPages + " processed Pages)");
              // System.err.println(" NbNodes = " + s.getNbNodes());
              relevantTimeOfLastThousands = System.currentTimeMillis();
            }
          }
        }
      }
      System.err.println("Extracted " + nbRelevantPages + " pages in: "
          + formatHMS(totalRelevantTime) + " (" + nbPages + " scanned Pages)");

    } catch (XMLStreamException ex) {
      System.out.println(ex.getMessage());

      if (ex.getNestedException() != null) {
        ex.getNestedException().printStackTrace();
      }
      throw new IOException("XML Stream Exception while reading dump", ex);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (xmlr != null) {
          xmlr.close();
        }
      } catch (XMLStreamException ex) {
        ex.printStackTrace();
      }

    }
    return 0;
  }

  private String formatHMS(long durationInMillis) {
    Duration d = Duration.ofMillis(durationInMillis);
    StringBuffer b = new StringBuffer();
    long h = d.toHours();
    long m = d.toMinutes() % 60;
    long s = d.getSeconds() % 60;
    return String.format("%d:%2d:%2d", h, m, s);
  }

}
