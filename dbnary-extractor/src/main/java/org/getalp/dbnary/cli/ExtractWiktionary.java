package org.getalp.dbnary.cli;

import org.getalp.dbnary.ExtractionFeature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.cli.Option;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.LangTools;
import org.getalp.dbnary.model.DbnaryModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.WiktionaryExtractorFactory;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexer;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractWiktionary extends DBnaryCommandLine {

  private Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  private static final String LANGUAGE_OPTION = "l";
  private static final String DEFAULT_LANGUAGE = "en";

  private static final String OUTPUT_FORMAT_OPTION = "f";
  private static final String DEFAULT_OUTPUT_FORMAT = "ttl";

  @Deprecated(since = "2.3.5", forRemoval = true)
  private static final String MODEL_OPTION = "m";
  private static final String DEFAULT_MODEL = "ontolex";

  private static final String TDB_OPTION = "tdb";

  private static final String OUTPUT_FILE_OPTION = "o";
  private static final String DEFAULT_OUTPUT_FILE = "extract";

  private static final String SUFFIX_OUTPUT_FILE_OPTION = "s";

  private static final String COMPRESS_OPTION = "z";
  private static final String DEFAULT_COMPRESS = "no";

  private static final String FOREIGN_EXTRACTION_OPTION = "x";

  private static final String MORPHOLOGY_OUTPUT_FILE_LONG_OPTION = "morphology";
  private static final String MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION = "M";

  private static final String ETYMOLOGY_OUTPUT_FILE_LONG_OPTION = "etymology";
  private static final String ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION = "E";

  private static final String METADATA_OUTPUT_FILE_LONG_OPTION = "lime";
  private static final String METADATA_OUTPUT_FILE_SHORT_OPTION = "L";

  private static final String ENHANCEMENT_OUTPUT_FILE_LONG_OPTION = "enhancement";
  private static final String ENHANCEMENT_OUTPUT_FILE_SHORT_OPTION = "X";

  private static final String STATS_OUTPUT_FILE_LONG_OPTION = "statistics";
  private static final String STATS_OUTPUT_FILE_SHORT_OPTION = "S";

  private static final String FOREIGN_LANGUAGES_OUTPUT_FILE_LONG_OPTION = "foreign";
  private static final String FOREIGN_LANGUAGES_OUTPUT_FILE_SHORT_OPTION = "";

  protected static final String URI_PREFIX_LONG_OPTION = "prefix";
  protected static final String URI_PREFIX_SHORT_OPTION = "p";

  private static final String FROM_PAGE_LONG_OPTION = "frompage";
  private static final String FROM_PAGE_SHORT_OPTION = "F";

  private static final String TO_PAGE_LONG_OPTION = "topage";
  private static final String TO_PAGE_SHORT_OPTION = "T";

  public static final XMLInputFactory2 xmlif;


  private String outputFile = DEFAULT_OUTPUT_FILE;
  private String morphoOutputFile = null;
  private String etymologyOutputFile = null;
  private String limeOutputFile = null;
  private String enhancementOutputFile = null;
  private String statsOutputFile = null;
  private String foreignDataOutputFile = null;
  private String outputFormat = DEFAULT_OUTPUT_FORMAT;
  private String model = DEFAULT_MODEL;
  private boolean compress;
  private String tdbDir = null;
  private String language = DEFAULT_LANGUAGE;
  private File dumpFile;
  private String outputFileSuffix = "";
  private int fromPage = 0;
  private int toPage = Integer.MAX_VALUE;
  private String extractorVersion;

  WiktionaryIndex wi;
  IWiktionaryExtractor we;

  private IWiktionaryDataHandler wdh;


  static {
    options.addOption(SUFFIX_OUTPUT_FILE_OPTION, false, "Add a unique suffix to output file. ");
    options.addOption(LANGUAGE_OPTION, true,
        "Language (fra, eng, deu or por). " + DEFAULT_LANGUAGE + " by default.");
    options.addOption(OUTPUT_FORMAT_OPTION, true,
        "Output format  (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). "
            + DEFAULT_OUTPUT_FORMAT + " by default.");
    options.addOption(COMPRESS_OPTION, true,
        "Compress the output using bzip2 (value: yes/no or true/false). " + DEFAULT_COMPRESS
            + " by default.");
    options.addOption(OUTPUT_FILE_OPTION, true,
        "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");
    options.addOption(Option.builder(MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION)
        .desc("Output file for morphology data. Undefined by default.").hasArg().argName("file")
        .build());
    options.addOption(Option.builder(ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION).desc("extract etymology data.").hasArg()
        .argName("file").build());
    options.addOption(
        Option.builder(METADATA_OUTPUT_FILE_SHORT_OPTION).longOpt(METADATA_OUTPUT_FILE_LONG_OPTION)
            .desc("Output file for LIME metadata. Undefined by default.").hasArg().argName("file")
            .build());
    options.addOption(Option.builder(ENHANCEMENT_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(ENHANCEMENT_OUTPUT_FILE_LONG_OPTION)
        .desc("Output file for ENHANCED (disambiguated) data. Undefined by default.").hasArg()
        .argName("file").build());
    options.addOption(
        Option.builder(STATS_OUTPUT_FILE_SHORT_OPTION).longOpt(STATS_OUTPUT_FILE_LONG_OPTION)
            .desc("Output file for statistics on data. Undefined by default.").hasArg()
            .argName("file").build());
    options.addOption(Option.builder(URI_PREFIX_SHORT_OPTION).longOpt(URI_PREFIX_LONG_OPTION)
        .desc("set the URI prefix used in the extracted dataset. Default: "
            + DbnaryModel.DBNARY_NS_PREFIX)
        .hasArg().argName("uri").build());
    options.addOption(Option.builder(FOREIGN_LANGUAGES_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(FOREIGN_LANGUAGES_OUTPUT_FILE_LONG_OPTION)
        .desc("Output file for foreign languages data. Undefined by default.").hasArg()
        .argName("file").build());
    options.addOption(FOREIGN_EXTRACTION_OPTION, false, "Extract foreign Languages");
    options.addOption(Option.builder(FROM_PAGE_SHORT_OPTION).longOpt(FROM_PAGE_LONG_OPTION)
        .desc("Do not process pages before the nth one. 0 by default.").hasArg().argName("num")
        .build());
    options.addOption(Option.builder(TO_PAGE_SHORT_OPTION).longOpt(TO_PAGE_LONG_OPTION)
        .desc("Do not process pages after the nth one. MAXINT by default.").hasArg().argName("num")
        .build());
    options.addOption(Option.builder().longOpt(TDB_OPTION)
        .desc("Use a temporary TDB to back the extractors models (use only for big extractions).")
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

  public ExtractWiktionary(String[] args) throws WiktionaryIndexerException {
    super(args);
    this.loadArgs();
  }

  /**
   * @param args arguments
   * @throws IOException ...
   * @throws WiktionaryIndexerException ...
   */
  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    ExtractWiktionary cli = new ExtractWiktionary(args);
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

    if (cmd.hasOption(SUFFIX_OUTPUT_FILE_OPTION)) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
      outputFileSuffix = df.format(new Date());
    }

    // TODO: TDB_DIR should be empty or non existant... check this
    if (cmd.hasOption(TDB_OPTION)) {
      try {
        Path temp = Files.createTempDirectory("dbnary");
        temp.toFile().deleteOnExit();
        tdbDir = temp.toAbsolutePath().toString();
        if (verbose) {
          System.err.println("Using temp TDB at " + tdbDir);
        }
        log.debug("Using TDB in {}", tdbDir);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            FileUtils.deleteDirectory(temp.toFile());
          } catch (IOException e) {
            System.err.println("Caught " + e.getClass()
                + " when attempting to delete the temporary TDB directory " + tdbDir);
            System.err.println(e.getLocalizedMessage());
          }
        }));
      } catch (IOException e) {
        System.err.println("Could not create temporary TDB directory. Exiting...");
        System.exit(-1);
      }
    }

    if (cmd.hasOption(OUTPUT_FORMAT_OPTION)) {
      outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
    }
    outputFormat = outputFormat.toUpperCase();

    if (cmd.hasOption(URI_PREFIX_LONG_OPTION)) {
      DbnaryModel.setGlobalDbnaryPrefix(cmd.getOptionValue(URI_PREFIX_SHORT_OPTION));
    }

    String compress_value = cmd.getOptionValue(COMPRESS_OPTION, DEFAULT_COMPRESS);
    compress = "true".startsWith(compress_value) || "yes".startsWith(compress_value);

    if (cmd.hasOption(OUTPUT_FILE_OPTION)) {
      outputFile = cmd.getOptionValue(OUTPUT_FILE_OPTION);
    }

    if (cmd.hasOption(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION)) {
      morphoOutputFile = cmd.getOptionValue(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION);
    }

    if (cmd.hasOption(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION)) {
      etymologyOutputFile = cmd.getOptionValue(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION);
    }

    if (cmd.hasOption(METADATA_OUTPUT_FILE_LONG_OPTION)) {
      limeOutputFile = cmd.getOptionValue(METADATA_OUTPUT_FILE_LONG_OPTION);
    }

    if (cmd.hasOption(ENHANCEMENT_OUTPUT_FILE_LONG_OPTION)) {
      enhancementOutputFile = cmd.getOptionValue(ENHANCEMENT_OUTPUT_FILE_LONG_OPTION);
    }

    if (cmd.hasOption(STATS_OUTPUT_FILE_LONG_OPTION)) {
      statsOutputFile = cmd.getOptionValue(STATS_OUTPUT_FILE_LONG_OPTION);
    }

    if (cmd.hasOption(FOREIGN_LANGUAGES_OUTPUT_FILE_LONG_OPTION)) {
      foreignDataOutputFile = cmd.getOptionValue(FOREIGN_LANGUAGES_OUTPUT_FILE_LONG_OPTION);
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

    we = null;
    if (!outputFormat.equals("RDF") && !outputFormat.equals("TURTLE")
        && !outputFormat.equals("NTRIPLE") && !outputFormat.equals("N3")
        && !outputFormat.equals("TTL") && !outputFormat.equals("RDFABBREV")) {
      System.err.println("unsupported format :" + outputFormat);
      System.exit(1);
    }

    if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)) {
      wdh = WiktionaryDataHandlerFactory.getForeignDataHandler(language, tdbDir);
      we = WiktionaryExtractorFactory.getForeignExtractor(language, wdh);
    } else {
      wdh = WiktionaryDataHandlerFactory.getDataHandler(language, tdbDir);
      we = WiktionaryExtractorFactory.getExtractor(language, wdh);
    }


    if (morphoOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.MORPHOLOGY);
    }
    if (etymologyOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.ETYMOLOGY);
    }
    if (limeOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.LIME);
    }
    if (enhancementOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.ENHANCEMENT);
    }

    if (statsOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.STATISTICS);
    }

    if (foreignDataOutputFile != null) {
      wdh.enableFeature(ExtractionFeature.FOREIGN_LANGUAGES);
    }

    if (null == we) {
      System.err
          .println("Wiktionary Extraction not yet available for " + LangTools.inEnglish(language));
      System.exit(1);
    }

    wi = new WiktionaryIndex(remainingArgs[0]);
    we.setWiktionaryIndex(wi);

    outputFile = outputFile + outputFileSuffix;

    dumpFile = new File(remainingArgs[0]);

    if (verbose) {
      System.err.println("Extracting Wiktionary Dump:");
      System.err.println("  Language: " + language);
      System.err.println("  Dump: " + dumpFile);
      System.err.println("  TDB : " + tdbDir);
      System.err.println("  Ontolex : " + outputFile);
      System.err.println("  Etymology : " + etymologyOutputFile);
      System.err.println("  Morphology : " + morphoOutputFile);
      System.err.println("  LIME : " + limeOutputFile);
      System.err.println("  Enhancement : " + enhancementOutputFile);
      System.err.println("  Statistics : " + statsOutputFile);
      System.err.println("  Foreign languages : " + foreignDataOutputFile);
      System.err.println("  Format : " + outputFormat);
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
              nbPages++;
              int nbnodes = wdh.nbEntries();
              if (nbPages < fromPage) {
                continue;
              }
              if (nbPages > toPage) {
                break;
              }
              try {
                we.extractData(title, page);
              } catch (RuntimeException e) {
                System.err.println("Runtime exception while extracting  page<<" + title
                    + ">>, proceeding to next pages.");
                System.err.println(e.getMessage());
                e.printStackTrace();
              }
              if (nbnodes != wdh.nbEntries()) {
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
        }
        System.err.println("Extracted " + nbRelevantPages + " pages in: "
            + formatHMS(totalRelevantTime) + " (" + nbPages + " scanned Pages)");

        // TODO : enable post processing after extraction ?
        if (verbose)
          System.out.println("Postprocessing extracted entries.");
        we.postProcessData(getDumpVersion(dumpFile.getName()));
        we.computeStatistics(getDumpVersion(dumpFile.getName()));
        we.populateMetadata(getDumpVersion(dumpFile.getName()), extractorVersion);

        saveBox(ExtractionFeature.MAIN, outputFile);

        if (null != morphoOutputFile) {
          saveBox(ExtractionFeature.MORPHOLOGY, morphoOutputFile);
        }
        if (null != etymologyOutputFile) {
          saveBox(ExtractionFeature.ETYMOLOGY, etymologyOutputFile);
        }
        if (null != limeOutputFile) {
          saveBox(ExtractionFeature.LIME, limeOutputFile);
        }
        if (null != enhancementOutputFile) {
          saveBox(ExtractionFeature.ENHANCEMENT, enhancementOutputFile);
        }
        if (null != statsOutputFile) {
          saveBox(ExtractionFeature.STATISTICS, statsOutputFile);
        }
        if (null != foreignDataOutputFile) {
          saveBox(ExtractionFeature.FOREIGN_LANGUAGES, foreignDataOutputFile);
        }

      } catch (XMLStreamException ex) {
        System.out.println(ex.getMessage());

        if (ex.getNestedException() != null) {
          ex.getNestedException().printStackTrace();
        }
        throw new IOException("XML Stream Exception while reading dump", ex);
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        wdh.closeDataset();
        try {
          if (xmlr != null) {
            xmlr.close();
          }
        } catch (XMLStreamException ex) {
          ex.printStackTrace();
        }
      }
    } finally {
      // Force TDB dir deletion after language extraction to avoid disk exhaustion when the main
      // method is called by UpdateAndExtractDumps.
      if (null != tdbDir) {
        try {
          FileUtils.deleteDirectory(new File(tdbDir));
        } catch (IOException e) {
          System.err.println("Caught " + e.getClass()
              + " when attempting to delete the temporary TDB directory " + tdbDir);
          System.err.println(e.getLocalizedMessage());
        }
      }
      // cleanup fields
      wi = null;
      we = null;
      wdh = null;
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

  public void saveBox(ExtractionFeature f, String of) throws IOException {
    try (OutputStream ostream = compress ? new BZip2CompressorOutputStream(new FileOutputStream(of))
        : new FileOutputStream(of)) {
      System.err.println("Dumping " + outputFormat + " representation of " + f.name() + ".");
      if (outputFormat.equals("RDF")) {
        wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), null);
      } else {
        wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), outputFormat);
      }
    } catch (IOException e) {
      System.err.println(
          "Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      throw e;
    }
  }

}
