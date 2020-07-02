package org.getalp.dbnary.cli;

import static org.getalp.dbnary.OntolexBasedRDFDataHandler.createGeneralStatisticsObservation;
import static org.getalp.dbnary.OntolexBasedRDFDataHandler.createNymRelationObservation;
import static org.getalp.dbnary.OntolexBasedRDFDataHandler.createTranslationObservation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryEtymologyOnt;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.DataCubeOnt;
import org.getalp.dbnary.DecompOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.LimeOnt;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.SkosOnt;
import org.getalp.dbnary.SynSemOnt;
import org.getalp.dbnary.VarTransOnt;
import org.getalp.dbnary.model.DbnaryModel;
import org.getalp.dbnary.model.NymRelation;
import org.getalp.dbnary.stats.GeneralStatistics;
import org.getalp.dbnary.stats.NymStatistics;
import org.getalp.dbnary.stats.TranslationsStatistics;

public class TranslateDiachronicStatisticsToDataCubes {


  private static String NS;
  private static Options options = null; // Command line options

  private static final String PREFIX_DIR_OPTION = "d";
  private static final String DEFAULT_PREFIX_DIR = ".";

  private CommandLine cmd = null; // Command Line arguments

  private String extractsDir = null;
  private String statsDir = null;

  static {
    options = new Options();
    options.addOption("h", false, "Prints usage and exits. ");
    options.addOption(PREFIX_DIR_OPTION, true,
        "directory containing the extracts and stats. " + DEFAULT_PREFIX_DIR + " by default ");
  }

  String[] remainingArgs;

  private String lg2;
  private String lg3;

  private void loadArgs(String[] args) {
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

    String prefixDir = DEFAULT_PREFIX_DIR;
    if (cmd.hasOption(PREFIX_DIR_OPTION)) {
      prefixDir = cmd.getOptionValue(PREFIX_DIR_OPTION);
    }

    remainingArgs = cmd.getArgs();
    if (remainingArgs.length != 1) {
      printUsage();
      System.exit(1);
    }

    lg3 = remainingArgs[0];
    lg2 = LangTools.getTerm2Code(lg3);
    if (null == lg2) {
      lg2 = lg3;
    }
    NS = DbnaryModel.DBNARY_NS_PREFIX + "/" + lg3 + "/";
    extractsDir = prefixDir + File.separator + "ontolex" + File.separator + lg2;
    statsDir = prefixDir + File.separator + "stats" + File.separator + lg2;
  }

  public static void main(String args[]) throws Exception {
    TranslateDiachronicStatisticsToDataCubes cliProg =
        new TranslateDiachronicStatisticsToDataCubes();
    cliProg.loadArgs(args);
    cliProg.translateStats();

  }

  private void translateStats() throws Exception {

    File d = new File(extractsDir);
    File ds = new File(statsDir);

    if (!d.isDirectory()) {
      System.err.println("Extracts directory not found: " + extractsDir);
      System.exit(1);
    }
    if (!ds.isDirectory()) {
      System.err.println("Stats directory not found: " + statsDir);
      System.exit(1);
    }

    Pattern dumpFilenamePattern = Pattern.compile(lg2 + "_dbnary_ontolex_(\\d{8})\\..*");

    String gstatFile = statsDir + File.separator + "general_stats.csv";
    ArrayList<Map<String, String>> gstats = readAndParseStats(gstatFile);

    String nstatFile = statsDir + File.separator + "nym_stats.csv";
    ArrayList<Map<String, String>> nstats = readAndParseStats(nstatFile);

    String tstatFile = statsDir + File.separator + "translations_stats.csv";
    ArrayList<Map<String, String>> tstats = readAndParseStats(tstatFile);

    Map<String, List<Map<String, String>>> statsByDate =
        Stream.of(gstats.stream(), nstats.stream(), tstats.stream()).flatMap(m -> m)
            .collect(Collectors.groupingBy(m -> m.get("Date")));

    for (Entry<String, List<Map<String, String>>> e : statsByDate.entrySet()) {
      File statsRDF = new File(
          extractsDir + File.separator + lg2 + "_dbnary_statistics_" + e.getKey() + ".ttl.bz2");
      if (statsRDF.exists()) {
        System.err.println(statsRDF.getAbsolutePath() + " already exists. Ignoring.");
        continue;
      }
      Model model = ModelFactory.createDefaultModel();
      initializePrefixes(model);
      e.getValue().forEach(map -> handleStatsLine(map, model, e.getKey()));
      saveBox(model, statsRDF.getAbsolutePath());
    }
  }

  private void initializePrefixes(Model model) {
    model.setNsPrefix(lg3, NS);
    model.setNsPrefix("dbnary", DBnaryOnt.getURI());
    model.setNsPrefix("dbetym", DBnaryEtymologyOnt.getURI());
    model.setNsPrefix("lexinfo", LexinfoOnt.getURI());
    model.setNsPrefix("rdfs", RDFS.getURI());
    model.setNsPrefix("dcterms", DCTerms.getURI());
    model.setNsPrefix("lexvo", DbnaryModel.LEXVO);
    model.setNsPrefix("rdf", RDF.getURI());
    model.setNsPrefix("olia", OliaOnt.getURI());
    model.setNsPrefix("ontolex", OntolexOnt.getURI());
    model.setNsPrefix("vartrans", VarTransOnt.getURI());
    model.setNsPrefix("synsem", SynSemOnt.getURI());
    model.setNsPrefix("lime", LimeOnt.getURI());
    model.setNsPrefix("decomp", DecompOnt.getURI());
    model.setNsPrefix("skos", SkosOnt.getURI());
    model.setNsPrefix("xs", XSD.getURI());
    model.setNsPrefix("qb", DataCubeOnt.getURI());
  }

  private void handleStatsLine(Map<String, String> map, Model model, String dumpVersion) {
    if (map.containsKey("# of lang")) {
      // It's a translation stats cvs
      createTranslationObservation(model, dumpVersion, NS, lg3, "number_of_languages",
          Long.valueOf(map.get("# of lang")));
      map.remove("# of lang");
      map.remove("Date");
      map.forEach(
          (k, v) -> createTranslationObservation(model, dumpVersion, NS, lg3, k, Long.valueOf(v)));
    } else if (map.containsKey("Entries")) {
      // It's the general statistics
      createGeneralStatisticsObservation(model, dumpVersion, NS, lg3,
          Long.parseLong(map.get("Translations")), Long.parseLong(map.get("Vocables")),
          Long.parseLong(map.get("Entries")), Long.parseLong(map.get("Senses")));
    } else if (map.containsKey("qsyn")) {
      // It's the nym statistics
      map.remove("Date");
      map.forEach((k, v) -> createNymRelationObservation(model, dumpVersion, NS, lg3,
          NymRelation.of(k), Long.parseLong(v)));
    }
  }

  private ArrayList<Map<String, String>> readAndParseStats(String gstatFile) throws IOException {
    ArrayList<Map<String, String>> m = new ArrayList<>();

    File gs = new File(gstatFile);

    if (gs.isFile() && gs.canRead()) {
      BufferedReader br =
          new BufferedReader(new InputStreamReader(new FileInputStream(gs), "UTF-8"));
      String h = br.readLine(); // reading header
      // TODO : parse header to know the columns sequence
      List<String> columns = Stream.of(h.split(",")).sequential().collect(Collectors.toList());
      String s = br.readLine();
      while (s != null) {
        // TODO : parse the line and generate a map from column header to cell value.
        Iterator<String> coliter = columns.iterator();
        HashMap<String, String> lineMap = new HashMap<>();
        Stream.of(s.split(",")).forEach(v -> lineMap.put(coliter.next(), v));

        m.add(lineMap);
        s = br.readLine();
      }
      br.close();
    }
    return m;
  }

  private void saveBox(Model box, String of) throws IOException {
    OutputStream ostream;
    if (of.endsWith(".bz2")) {
      // outputFile = outputFile + ".bz2";
      ostream = new BZip2CompressorOutputStream(new FileOutputStream(of));
    } else {
      ostream = new FileOutputStream(of);
    }
    try (PrintStream out = new PrintStream(ostream, false, "UTF-8")) {
      System.err.println("Dumping statistics to " + of + ".");
      box.write(out, "TURTLE");
      out.flush();
    } catch (IOException e) {
      System.err.println(
          "Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      throw e;
    }
  }

  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    String help =
        "Translate diachronic statistics that are available in csv file in the prefix folder "
            + "to stats file using datacube properties.";
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar "
            + TranslateDiachronicStatisticsToDataCubes.class.getCanonicalName() + "[OPTIONS] lang",
        "With OPTIONS in:", options, help, false);
  }

}
