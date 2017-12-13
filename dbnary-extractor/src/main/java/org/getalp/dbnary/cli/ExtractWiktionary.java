package org.getalp.dbnary.cli;

import static org.getalp.dbnary.IWiktionaryDataHandler.Feature;
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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.LangTools;
import org.getalp.dbnary.DbnaryModel;
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

public class ExtractWiktionary {

  private static Options options = null; // Command line options
  private Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  private static final String LANGUAGE_OPTION = "l";
  private static final String DEFAULT_LANGUAGE = "en";

  private static final String OUTPUT_FORMAT_OPTION = "f";
  private static final String DEFAULT_OUTPUT_FORMAT = "ttl";

  private static final String MODEL_OPTION = "m";
  private static final String DEFAULT_MODEL = "ontolex";

  private static final String TDB_OPTION = "tdb";

  private static final String OUTPUT_FILE_OPTION = "o";
  private static final String DEFAULT_OUTPUT_FILE = "extract";

  private static final String SUFFIX_OUTPUT_FILE_OPTION = "s";

  private static final String COMPRESS_OPTION = "z";
  private static final String DEFAULT_COMPRESS = "no";

  private static final String FOREIGN_EXTRACTION_OPTION = "x";

  private static final String MORPHOLOGY_OUTPUT_FILE_LONG_OPTION = "morpho";
  private static final String MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION = "M";

  private static final String ETYMOLOGY_OUTPUT_FILE_LONG_OPTION = "etymology";
  private static final String ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION = "E";

  protected static final String URI_PREFIX_LONG_OPTION = "prefix";
  protected static final String URI_PREFIX_SHORT_OPTION = "p";

  private static final String FROM_PAGE_LONG_OPTION = "frompage";
  private static final String FROM_PAGE_SHORT_OPTION = "F";

  private static final String TO_PAGE_LONG_OPTION = "topage";
  private static final String TO_PAGE_SHORT_OPTION = "T";

  public static final XMLInputFactory2 xmlif;


  private CommandLine cmd = null; // Command Line arguments

  private String outputFile = DEFAULT_OUTPUT_FILE;
  private String morphoOutputFile = null;
  private String etymologyOutputFile = null;
  private String outputFormat = DEFAULT_OUTPUT_FORMAT;
  private String model = DEFAULT_MODEL;
  private boolean compress;
  private String tdbDir = null;
  private String language = DEFAULT_LANGUAGE;
  private File dumpFile;
  private String outputFileSuffix = "";
  private int fromPage = 0;
  private int toPage = Integer.MAX_VALUE;

  WiktionaryIndex wi;
  IWiktionaryExtractor we;

  private IWiktionaryDataHandler wdh;


  static {
    options = new Options();
    options.addOption("h", "help", false, "Prints usage and exits. ");
    options.addOption(SUFFIX_OUTPUT_FILE_OPTION, false, "Add a unique suffix to output file. ");
    options.addOption(LANGUAGE_OPTION, true,
        "Language (fra, eng, deu or por). " + DEFAULT_LANGUAGE + " by default.");
    options.addOption(OUTPUT_FORMAT_OPTION, true,
        "Output format  (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). "
            + DEFAULT_OUTPUT_FORMAT + " by default.");
    options.addOption(COMPRESS_OPTION, true,
        "Compress the output using bzip2 (value: yes/no or true/false). " + DEFAULT_COMPRESS
            + " by default.");
    options.addOption(MODEL_OPTION, true, "(Deprecated) the model will always be " + DEFAULT_MODEL);
    options.addOption(OUTPUT_FILE_OPTION, true,
        "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");
    options.addOption(OptionBuilder.withLongOpt(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION)
        .withDescription("Output file for morphology data. Undefined by default.").hasArg()
        .withArgName("file").create(MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION));
    options.addOption(OptionBuilder.withLongOpt(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION)
        .withDescription("extract etymology data.").hasArg().withArgName("file")
        .create(ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION));
    options.addOption(OptionBuilder.withLongOpt(URI_PREFIX_LONG_OPTION)
        .withDescription("set the URI prefix used in the extracted dataset. Default: "
            + DbnaryModel.DBNARY_NS_PREFIX)
        .hasArg().withArgName("uri").create(URI_PREFIX_SHORT_OPTION));
    options.addOption(FOREIGN_EXTRACTION_OPTION, false, "Extract foreign Languages");
    options.addOption(OptionBuilder.withLongOpt(FROM_PAGE_LONG_OPTION)
        .withDescription("Do not process pages before the nth one. 0 by default.").hasArg()
        .withArgName("num").create(FROM_PAGE_SHORT_OPTION));
    options.addOption(OptionBuilder.withLongOpt(TO_PAGE_LONG_OPTION)
        .withDescription("Do not process pages after the nth one. MAXINT by default.").hasArg()
        .withArgName("num").create(TO_PAGE_SHORT_OPTION));
    options.addOption(OptionBuilder.withLongOpt(TDB_OPTION).withDescription(
        "Use the specified dir as a TDB to back the extractors models (use only for big extractions).")
        .create());
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


  /**
   * @param args arguments
   * @throws IOException ...
   * @throws WiktionaryIndexerException ...
   */
  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    ExtractWiktionary cliProg = new ExtractWiktionary();
    cliProg.loadArgs(args);
    cliProg.extract();
  }

  /**
   * Validate and set command line arguments. Exit after printing usage if anything is astray
   *
   * @param args String[] args as featured in public static void main()
   * @throws WiktionaryIndexerException ..
   */
  private void loadArgs(String[] args) throws WiktionaryIndexerException {
    CommandLineParser parser = new DefaultParser();
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
      printUsage();
      System.exit(1);
    }

    // Check for args
    if (cmd.hasOption("h")) {
      printUsage();
      System.exit(0);
    }

    if (cmd.hasOption(SUFFIX_OUTPUT_FILE_OPTION)) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
      outputFileSuffix = df.format(new Date());
    }

    // TODO: TDB_DIR should be empty of non existant... check this
    if (cmd.hasOption(TDB_OPTION)) {
      try {
        Path temp = Files.createTempDirectory("dbnary");
        temp.toFile().deleteOnExit();
        tdbDir = temp.toAbsolutePath().toString();
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
    if (cmd.hasOption(MODEL_OPTION)) {
      System.err.println("WARN: the " + MODEL_OPTION
          + " option is now deprecated. Forcibly using model: " + DEFAULT_MODEL);
      // model = cmd.getOptionValue(MODEL_OPTION);
    }
    model = model.toUpperCase();

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
    if (cmd.hasOption(LANGUAGE_OPTION)) {
      language = cmd.getOptionValue(LANGUAGE_OPTION);
      language = LangTools.getCode(language);
    }

    if (cmd.hasOption(FROM_PAGE_LONG_OPTION)) {
      fromPage = Integer.valueOf(cmd.getOptionValue(FROM_PAGE_LONG_OPTION));
    }

    if (cmd.hasOption(TO_PAGE_LONG_OPTION)) {
      toPage = Integer.valueOf(cmd.getOptionValue(TO_PAGE_LONG_OPTION));
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
    } else {
      wdh = WiktionaryDataHandlerFactory.getDataHandler(language, tdbDir);
    }
    if (morphoOutputFile != null) {
      wdh.enableFeature(Feature.MORPHOLOGY);
    }
    if (etymologyOutputFile != null) {
      wdh.enableFeature(Feature.ETYMOLOGY);
    }

    if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)) {
      we = WiktionaryExtractorFactory.getForeignExtractor(language, wdh);
    } else {
      we = WiktionaryExtractorFactory.getExtractor(language, wdh);
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
  }

  public void extract() throws IOException {

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
      we.postProcessData();

      saveBox(Feature.MAIN, outputFile);

      if (null != morphoOutputFile) {
        saveBox(Feature.MORPHOLOGY, morphoOutputFile);
      }
      if (null != etymologyOutputFile) {
        saveBox(Feature.ETYMOLOGY, etymologyOutputFile);
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
      try {
        if (xmlr != null) {
          xmlr.close();
        }
      } catch (XMLStreamException ex) {
        ex.printStackTrace();
      }
    }


  }

  private String formatHMS(long durationInMillis) {
    Duration d = Duration.ofMillis(durationInMillis);
    StringBuffer b = new StringBuffer();
    long h = d.toHours();
    long m = d.toMinutes();
    long s = d.getSeconds();
    return String.format("%d:%2d:%2d", h, m, s);
  }

  public void saveBox(IWiktionaryDataHandler.Feature f, String of) throws IOException {
    OutputStream ostream;
    if (compress) {
      // outputFile = outputFile + ".bz2";
      ostream = new BZip2CompressorOutputStream(new FileOutputStream(of));
    } else {
      ostream = new FileOutputStream(of);
    }
    try {
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
    } finally {

      ostream.flush();
      ostream.close();
    }
  }


  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar org.getalp.dbnary.cli.ExtractWiktionary [OPTIONS] dumpFile",
        "With OPTIONS in:", options,
        "dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index.",
        false);
  }

}
