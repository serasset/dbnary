package org.getalp.dbnary.cli;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.cli.Option;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.LangTools;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexer;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckWiktionarySyntaxQuality extends DBnaryCommandLine {

  private final Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  private static final String LANGUAGE_OPTION = "l";
  private static final String DEFAULT_LANGUAGE = "en";

  private static final String OUTPUT_FILE_OPTION = "o";
  private static final String DEFAULT_OUTPUT_FILE = "extract";

  private static final String FROM_PAGE_LONG_OPTION = "frompage";
  private static final String FROM_PAGE_SHORT_OPTION = "F";

  private static final String TO_PAGE_LONG_OPTION = "topage";
  private static final String TO_PAGE_SHORT_OPTION = "T";

  public static final XMLInputFactory2 xmlif;


  private String outputFile = DEFAULT_OUTPUT_FILE;
  private String language = DEFAULT_LANGUAGE;
  private File dumpFile;
  private String outputFileSuffix = "";
  private int fromPage = 0;
  private int toPage = Integer.MAX_VALUE;
  private String extractorVersion;

  WiktionaryIndex wi;

  static {
    options.addOption(LANGUAGE_OPTION, true,
        "Language (fra, eng, deu or por). " + DEFAULT_LANGUAGE + " by default.");
    options.addOption(OUTPUT_FILE_OPTION, true,
        "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");
    options.addOption(Option.builder(FROM_PAGE_SHORT_OPTION).longOpt(FROM_PAGE_LONG_OPTION)
        .desc("Do not process pages before the nth one. 0 by default.").hasArg().argName("num")
        .build());
    options.addOption(Option.builder(TO_PAGE_SHORT_OPTION).longOpt(TO_PAGE_LONG_OPTION)
        .desc("Do not process pages after the nth one. MAXINT by default.").hasArg().argName("num")
        .build());
  }

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

  public CheckWiktionarySyntaxQuality(String[] args) throws WiktionaryIndexerException {
    super(args);
    this.loadArgs();
  }

  /**
   * @param args arguments
   * @throws IOException ...
   * @throws WiktionaryIndexerException ...
   */
  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    CheckWiktionarySyntaxQuality cli = new CheckWiktionarySyntaxQuality(args);
    cli.extract();
  }

  /**
   * Analyse command line arguments to prepare processing.
   *
   * @throws WiktionaryIndexerException ..
   */
  private void loadArgs() throws WiktionaryIndexerException {
    extractorVersion = "UNKNOWN";
    Manifest mf = new Manifest();
    try {
      mf.read(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("META-INF/MANIFEST.MF"));

      Attributes atts = mf.getMainAttributes();
      extractorVersion = atts.getValue("Extractor-Version");
    } catch (IOException e) {
      log.info("Could not retrieve extractor version.");
    }

    if (cmd.hasOption(OUTPUT_FILE_OPTION)) {
      outputFile = cmd.getOptionValue(OUTPUT_FILE_OPTION);
    }

    if (cmd.hasOption(LANGUAGE_OPTION)) {
      language = cmd.getOptionValue(LANGUAGE_OPTION);
      language = LangTools.getCode(language);
    }

    if (cmd.hasOption(FROM_PAGE_LONG_OPTION)) {
      fromPage = Integer.parseInt(cmd.getOptionValue(FROM_PAGE_LONG_OPTION));
    }

    if (cmd.hasOption(TO_PAGE_LONG_OPTION)) {
      toPage = Integer.parseInt(cmd.getOptionValue(TO_PAGE_LONG_OPTION));
    }
    String[] remainingArgs = cmd.getArgs();
    if (remainingArgs.length != 1) {
      printUsage();
      System.exit(1);
    }

    wi = new WiktionaryIndex(remainingArgs[0]);

    outputFile = outputFile + outputFileSuffix;

    dumpFile = new File(remainingArgs[0]);

    if (verbose) {
      System.err.println("Checking Syntax Quality on Wiktionary Dump:");
      System.err.println("  Language: " + language);
      System.err.println("  Dump: " + dumpFile);
      System.err.println("  Ontolex : " + outputFile);
    }
  }

  public void extract() throws IOException {

    try {
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
    } finally {

    }
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
