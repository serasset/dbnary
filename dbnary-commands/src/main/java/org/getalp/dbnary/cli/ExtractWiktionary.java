package org.getalp.dbnary.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.WiktionaryExtractorFactory;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexer;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.getalp.dbnary.model.DbnaryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "extract", mixinStandardHelpOptions = true,
    header = "extract all pages from a dump and write resulting RDF files.",
    description = "Process all pages and extract lescial data according to options that are passed "
        + "to the program. The extracted lexical data is encoded as RDF graphs using ontolex, "
        + "lexinfo, olia and other standard vocabularies.")
public class ExtractWiktionary implements Callable<Integer> {

  private static Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  // CommandLine specification
  @Spec
  private CommandSpec spec;
  @ParentCommand
  private DBnary parent; // picocli injects reference to parent command


  private static final String DEFAULT_LANGUAGE = "en";
  private static final String DEFAULT_OUTPUT_FORMAT = "ttl";
  private static final String DEFAULT_OUTPUT_FILE = "extract";

  public static final XMLInputFactory2 xmlif;

  // Options

  private String language = DEFAULT_LANGUAGE;

  @Option(names = {"-l", "--language"}, paramLabel = "LANGUAGE", defaultValue = DEFAULT_LANGUAGE,
      description = "language edition of the dump to be extracted; uses a 2 or 3 iso letter code;"
          + " Default: ${DEFAULT-VALUE}.")
  public void setLanguage(String language) {
    this.language = LangTools.getCode(language);
    if (null == this.language) {
      throw new ParameterException(spec.commandLine(), String.format(
          "Invalid language '%s' for option '--language': unknown language code.", language));
    }
  }

  private String outputFormat;

  @Option(names = {"-f", "--format"}, paramLabel = "OUTPUT-FORMAT",
      defaultValue = DEFAULT_OUTPUT_FORMAT,
      description = "format used for all models (valid values: ttl, turtle, rdf, ntriple, n3,"
          + " rdfabbrev); Default: ${DEFAULT-VALUE}.")
  public void setOutputFormat(String format) {
    this.outputFormat = format.toUpperCase(Locale.ROOT);
    if (!outputFormat.equals("RDF") && !outputFormat.equals("TURTLE")
        && !outputFormat.equals("NTRIPLE") && !outputFormat.equals("N3")
        && !outputFormat.equals("TTL") && !outputFormat.equals("RDFABBREV")) {
      throw new ParameterException(spec.commandLine(),
          String.format("Invalid format '%s' for option '--format': unknown format.", format));
    }
  }

  @Option(names = {"-o", "--output"}, paramLabel = "ONTOLEX-OUTPUT-FILE",
      defaultValue = DEFAULT_OUTPUT_FILE,
      description = "file in which extracted core (ontolex) model will be written; "
          + "Default: ${DEFAULT-VALUE}.")
  private File outputFile;

  @Option(names = {"-M", "--morphology"}, paramLabel = "MORPHOLOGY-OUTPUT-FILE",
      description = "file in which extracted morphology model will be written.")
  private File morphoOutputFile = null;

  @Option(names = {"-E", "--etymology"}, paramLabel = "ETYMOLOGY-OUTPUT-FILE",
      description = "file in which extracted etymology model will be written.")
  private File etymologyOutputFile = null;

  @Option(names = {"-L", "--lime"}, paramLabel = "LIME-OUTPUT-FILE",
      description = "file in which extracted lime model will be written.")
  private File limeOutputFile = null;

  @Option(names = {"-X", "--enhancement"}, paramLabel = "ENHANCEMENT-OUTPUT-FILE",
      description = "file in which extracted enhancement model will be written.")
  private File enhancementOutputFile = null;


  @Option(names = {"-S", "--statistics"}, paramLabel = "STATISTICS-OUTPUT-FILE",
      description = "file in which extracted statistics model will be written.")
  private File statsOutputFile = null;

  @Option(names = {"--foreign"}, paramLabel = "FOREIGN-OUTPUT-FILE",
      description = "file in which extracted foreign data model will be written.")
  private File foreignDataOutputFile = null;

  @Option(names = {"--hdt"}, paramLabel = "HDT-OUTPUT-FILE",
      description = "file in which the HDT version of all data models will be written.")
  private File hdtOutputFile = null;

  @Option(names = {"-p", "--prefix"}, paramLabel = "DBNARY-URI-PREFIX",
      description = "Use the specified prefix for all URIs. (use with care).")
  private void setDBnaryURIPrefix(String prefix) {
    DbnaryModel.setGlobalDbnaryPrefix(prefix);
  }

  // TODO: rationalize the foreign extraction logic (either boolean or as a model (prefered)
  @Option(names = {"-x"}, defaultValue = "false",
      description = "Extract foreign entries from the language dump. False by default.")
  private boolean extractForeignEntries = false;

  @Option(names = {"--tdb"}, negatable = true, defaultValue = "false",
      description = "Compress the resulting extracted files using BZip2. False by default.")
  private boolean useTdb = false;

  @Option(names = {"--no-compress"}, negatable = true,
      description = "Compress the resulting extracted files using BZip2. True by default.")
  private boolean compress = true;

  @Option(names = {"-F", "--frompage"}, paramLabel = "NUMBER",
      description = "Begin the extraction at the specified page number.")
  private int fromPage = 0;

  @Option(names = {"-T", "--topage"}, paramLabel = "NUMBER",
      description = "Stop the extraction at the specified page number.")
  private int toPage = Integer.MAX_VALUE;

  @Option(names = {"-v"}, description = "Print extra information before checking.")
  private boolean verbose;

  // Parameter
  @Parameters(index = "0", description = "The dump file of the wiki to be extracted.", arity = "1")
  private File dumpFile;

  // non parameters
  private String tdbDir = null;


  WiktionaryIndex wi;
  IWiktionaryExtractor we;

  private IWiktionaryDataHandler wdh;

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
    new CommandLine(new ExtractWiktionary()).execute(args);
  }

  @Override
  public Integer call() throws Exception {
    Integer returnCode = prepareExtraction();
    if (returnCode != 0)
      return returnCode;
    return extract();
  }

  /**
   * Analyse command line arguments to prepare processing.
   *
   * @throws WiktionaryIndexerException ..
   */
  private Integer prepareExtraction() throws WiktionaryIndexerException {

    // System.setProperty(LOG_KEY_PREFIX + "org.getalp.dbnary.ita", "trace");
    if (useTdb) {
      try {
        Path temp = Files.createTempDirectory("dbnary");
        temp.toFile().deleteOnExit();
        tdbDir = temp.toAbsolutePath().toString();
        if (verbose) {
          spec.commandLine().getErr().println("Using temp TDB at " + tdbDir);
        }
        log.debug("Using TDB in {}", tdbDir);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            FileUtils.deleteDirectory(temp.toFile());
          } catch (IOException e) {
            spec.commandLine().getErr().println("Caught " + e.getClass()
                + " when attempting to delete the temporary TDB directory " + tdbDir);
            spec.commandLine().getErr().println(e.getLocalizedMessage());
          }
        }));
      } catch (IOException e) {
        spec.commandLine().getErr().println("Could not create temporary TDB directory. Exiting...");
        return -1;
      }
    }

    we = null;

    if (extractForeignEntries) {
      wdh = WiktionaryDataHandlerFactory.getForeignDataHandler(language, tdbDir);
      we = WiktionaryExtractorFactory.getForeignExtractor(language, wdh);
    } else {
      wdh = WiktionaryDataHandlerFactory.getDataHandler(language, tdbDir);
      we = WiktionaryExtractorFactory.getExtractor(language, wdh);
    }
    if (null == we) {
      spec.commandLine().getErr()
          .println("Wiktionary Extraction not yet available for " + LangTools.inEnglish(language));
      return -1;
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
      wdh.enableFeature(ExtractionFeature.EXOLEXICON);
    }

    wi = new WiktionaryIndex(dumpFile);
    we.setWiktionaryIndex(wi);

    if (verbose) {
      PrintWriter err = spec.commandLine().getErr();
      err.println("Extracting Wiktionary Dump:");
      err.println("  Language: " + language);
      err.println("  Dump: " + dumpFile);
      err.println("  TDB : " + tdbDir);
      err.println("  Ontolex : " + outputFile);
      err.println("  Etymology : " + etymologyOutputFile);
      err.println("  Morphology : " + morphoOutputFile);
      err.println("  LIME : " + limeOutputFile);
      err.println("  Enhancement : " + enhancementOutputFile);
      err.println("  Statistics : " + statsOutputFile);
      err.println("  Foreign languages : " + foreignDataOutputFile);
      err.println("  Format : " + outputFormat);
      err.println("  HDT : " + hdtOutputFile);
    }
    return 0;
  }

  public Integer extract() throws IOException {

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
                spec.commandLine().getErr().println("Runtime exception while extracting  page<<"
                    + title + ">>, proceeding to next pages.");
                spec.commandLine().getErr().println(e.getMessage());
                e.printStackTrace();
              }
              if (nbnodes != wdh.nbEntries()) {
                totalRelevantTime = (System.currentTimeMillis() - startTime);
                nbRelevantPages++;
                if (nbRelevantPages % 1000 == 0) {
                  spec.commandLine().getErr()
                      .println("Extracted: " + nbRelevantPages + " pages in: "
                          + formatHMS(totalRelevantTime) + " / Average = "
                          + (totalRelevantTime / nbRelevantPages) + " ms/extracted page ("
                          + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000
                          + " ms) (" + nbPages + " processed Pages)");
                  // System.err.println(" NbNodes = " + s.getNbNodes());
                  relevantTimeOfLastThousands = System.currentTimeMillis();
                }
              }
            }
          }
        }
        spec.commandLine().getErr().println("Extracted " + nbRelevantPages + " pages in: "
            + formatHMS(totalRelevantTime) + " (" + nbPages + " scanned Pages)");

        // TODO : enable post processing after extraction ?
        if (verbose)
          spec.commandLine().getErr().println("Postprocessing extracted entries.");
        we.postProcessData(VersionProvider.getDumpVersion(dumpFile.getName()));
        we.computeStatistics(VersionProvider.getDumpVersion(dumpFile.getName()));
        we.populateMetadata(VersionProvider.getDumpVersion(dumpFile.getName()),
            new VersionProvider().getExtractorVersion());

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
          saveBox(ExtractionFeature.EXOLEXICON, foreignDataOutputFile);
        }

        if (null != hdtOutputFile) {
          saveAllAsHDT(hdtOutputFile);
        }
      } catch (XMLStreamException ex) {
        spec.commandLine().getErr().println(ex.getMessage());

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
      // method is called by UpdateAndExtractDumps (in the same JDK instance).
      if (null != tdbDir) {
        try {
          FileUtils.deleteDirectory(new File(tdbDir));
        } catch (IOException e) {
          spec.commandLine().getErr().println("Caught " + e.getClass()
              + " when attempting to delete the temporary TDB directory " + tdbDir);
          spec.commandLine().getErr().println(e.getLocalizedMessage());
        }
      }
      // cleanup fields
      wi = null;
      we = null;
      wdh = null;
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

  private void saveBox(ExtractionFeature f, File of) throws IOException {
    try (OutputStream ostream = compress ? new BZip2CompressorOutputStream(new FileOutputStream(of))
        : new FileOutputStream(of)) {
      spec.commandLine().getErr()
          .println("Dumping " + outputFormat + " representation of " + f.name() + ".");
      if (outputFormat.equals("RDF")) {
        wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), null);
      } else {
        wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), outputFormat);
      }
    } catch (IOException e) {
      spec.commandLine().getErr().println(
          "Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
      e.printStackTrace(spec.commandLine().getErr());
      throw e;
    }
  }


  private void saveAllAsHDT(File hdtOutputFile) throws IOException {
    try (OutputStream ostream =
        compress ? new BZip2CompressorOutputStream(new FileOutputStream(hdtOutputFile))
            : new FileOutputStream(hdtOutputFile)) {
      spec.commandLine().getErr().format("Dumping all features as a single HDT file in %s.%n",
          hdtOutputFile);
      wdh.dumpAllAsHDT(ostream);
    } catch (IOException e) {
      spec.commandLine().getErr()
          .println("Caught IOException while producing HDT file: \n" + e.getLocalizedMessage());
      e.printStackTrace(spec.commandLine().getErr());
      throw e;
    }
  }


}
