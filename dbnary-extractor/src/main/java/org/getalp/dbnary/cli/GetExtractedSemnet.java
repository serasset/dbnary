package org.getalp.dbnary.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.getalp.LangTools;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.WiktionaryExtractorFactory;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;

public class GetExtractedSemnet extends DBnaryCommandLine {

  private static final String LANGUAGE_OPTION = "l";
  private static final String DEFAULT_LANGUAGE = "en";

  private static final String OUTPUT_FORMAT_OPTION = "f";
  private static final String DEFAULT_OUTPUT_FORMAT = "ttl";

  private String outputFormat = DEFAULT_OUTPUT_FORMAT;
  private String language = DEFAULT_LANGUAGE;
  private boolean extractsMorpho = false;
  private boolean extractsEtymology = false;
  private boolean extractsStats = false;


  private static final String FOREIGN_EXTRACTION_OPTION = "x";

  private static final String MORPHOLOGY_OUTPUT_FILE_LONG_OPTION = "morpho";
  private static final String MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION = "M";

  protected static final String ETYMOLOGY_OUTPUT_FILE_LONG_OPTION = "etymology";
  protected static final String ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION = "E";

  protected static final String STATS_OUTPUT_FILE_LONG_OPTION = "stats";
  protected static final String STATS_OUTPUT_FILE_SHORT_OPTION = "S";

  protected static final String URI_PREFIX_LONG_OPTION = "prefix";
  protected static final String URI_PREFIX_SHORT_OPTION = "p";

  static {
    options.addOption(LANGUAGE_OPTION, true,
        "Language (fr, en,it,pt de, fi or ru). " + DEFAULT_LANGUAGE + " by default.");
    options.addOption(OUTPUT_FORMAT_OPTION, true,
        "Output format (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). "
            + DEFAULT_OUTPUT_FORMAT + " by default.");
    options.addOption(FOREIGN_EXTRACTION_OPTION, false, "Extract foreign languages");
    options.addOption(Option.builder(MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION).desc("extract morphology data.").build());
    options.addOption(Option.builder(ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION).desc("extract etymology data.").build());
    options.addOption(Option.builder(STATS_OUTPUT_FILE_SHORT_OPTION)
        .longOpt(STATS_OUTPUT_FILE_LONG_OPTION)
        .desc("extract statistics from data processing (enhancement, sizes, etc.).").build());
    options.addOption(Option.builder(URI_PREFIX_SHORT_OPTION).longOpt(URI_PREFIX_LONG_OPTION)
        .desc("set the URI prefix used in the extracted dataset. Default: "
            + DbnaryModel.DBNARY_NS_PREFIX)
        .hasArg().argName("uri").build());
  }

  WiktionaryIndex wi;
  String[] remainingArgs;
  IWiktionaryExtractor we;
  IWiktionaryDataHandler wdh;
  private String dumpFileName;

  public GetExtractedSemnet(String[] args) throws WiktionaryIndexerException {
    super(args);
    this.loadArgs();
  }

  /**
   * decode args to prepare process.
   */
  private void loadArgs() throws WiktionaryIndexerException {
    if (cmd.hasOption(OUTPUT_FORMAT_OPTION)) {
      outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
    }
    outputFormat = outputFormat.toUpperCase();

    if (cmd.hasOption(LANGUAGE_OPTION)) {
      language = cmd.getOptionValue(LANGUAGE_OPTION);
      language = LangTools.getCode(language);
    }

    extractsMorpho = cmd.hasOption(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION);
    extractsEtymology = cmd.hasOption(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION);
    extractsStats = cmd.hasOption(STATS_OUTPUT_FILE_LONG_OPTION);

    if (cmd.hasOption(URI_PREFIX_LONG_OPTION)) {
      DbnaryModel.setGlobalDbnaryPrefix(cmd.getOptionValue(URI_PREFIX_SHORT_OPTION));
    }

    remainingArgs = cmd.getArgs();
    if (remainingArgs.length <= 1) {
      printUsage();
      System.exit(1);
    }

    we = null;
    if (outputFormat.equals("RDF") || outputFormat.equals("TURTLE")
        || outputFormat.equals("NTRIPLE") || outputFormat.equals("N3") || outputFormat.equals("TTL")
        || outputFormat.equals("RDFABBREV")) {
      if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)) {
        wdh = WiktionaryDataHandlerFactory.getForeignDataHandler(language, null);
      } else {
        wdh = WiktionaryDataHandlerFactory.getDataHandler(language, null);
      }
      wdh.enableFeature(ExtractionFeature.ENHANCEMENT);
      if (extractsMorpho) {
        wdh.enableFeature(ExtractionFeature.MORPHOLOGY);
      }
      if (extractsEtymology) {
        wdh.enableFeature(ExtractionFeature.ETYMOLOGY);
      }
      if (extractsStats) {
        wdh.enableFeature(ExtractionFeature.STATS);
      }
    } else {
      System.err.println("unsupported format :" + outputFormat);
      System.exit(1);
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

    dumpFileName = remainingArgs[0];
    wi = new WiktionaryIndex(dumpFileName);
    we.setWiktionaryIndex(wi);
  }

  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    GetExtractedSemnet cli = new GetExtractedSemnet(args);
    cli.extract();
  }


  private void extract() throws IOException {

    for (int i = 1; i < remainingArgs.length; i++) {
      String pageContent = wi.getTextOfPage(remainingArgs[i]);
      we.extractData(remainingArgs[i], pageContent);
    }
    we.postProcessData(getDumpVersion(dumpFileName));

    dumpBox(ExtractionFeature.MAIN);

    System.out.println("----------- ENHANCEMENT ----------");
    dumpBox(ExtractionFeature.ENHANCEMENT);

    if (extractsMorpho) {
      System.out.println("----------- MORPHOLOGY ----------");
      dumpBox(ExtractionFeature.MORPHOLOGY);
    }
    if (extractsEtymology) {
      System.out.println("----------- ETYMOLOGY ----------");
      dumpBox(ExtractionFeature.ETYMOLOGY);
    }

    if (extractsStats) {
      System.out.println("----------- STATISTICS ----------");
      dumpBox(ExtractionFeature.STATS);
    }

  }

  public void dumpBox(ExtractionFeature f) throws IOException {
    OutputStream ostream = System.out;
    try {
      wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), outputFormat);
    } catch (IOException e) {
      System.err.println(
          "Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      throw e;
    } finally {
      if (null != ostream) {
        ostream.flush();
      }
    }
  }

  @Override
  public void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    String help =
        "dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index."
            + System.getProperty("line.separator", "\n")
            + "Displays the extracted semnet of the wiktionary page(s) named \"entryname\", ...";
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar org.getalp.dbnary.cli.GetExtractedSemnet [OPTIONS] dumpFile entryname ...",
        "With OPTIONS in:", options, help, false);
  }

}
