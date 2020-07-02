package org.getalp.dbnary.enhancer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
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
import org.getalp.LangTools;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStatsModule;

public class EnhanceLatestExtracts {


  private static Options options = null; // Command line options

  private static final String PREFIX_DIR_OPTION = "d";
  private static final String DEFAULT_PREFIX_DIR = ".";
  protected static final String COMPRESS_OPTION = "z";
  protected static final String RDF_FORMAT_OPTION = "f";
  protected static final String DEFAULT_RDF_FORMAT = "turtle";

  private CommandLine cmd = null; // Command Line arguments

  private String extractsDir = null;
  private String statsDir = null;
  private boolean doCompress;
  protected String rdfFormat;
  private String prefixDir = null;

  static {
    options = new Options();
    options.addOption("h", false, "Prints usage and exits. ");
    options.addOption(PREFIX_DIR_OPTION, true,
        "directory containing the extracts and stats. " + DEFAULT_PREFIX_DIR + " by default ");
    options.addOption(COMPRESS_OPTION, false, "if present, compress the ouput with BZip2.");
    options.addOption(RDF_FORMAT_OPTION, true,
        "RDF file format (xmlrdf, turtle, n3, etc.). " + DEFAULT_RDF_FORMAT + " by default.");
  }

  String[] remainingArgs;
  private TranslationGlossesStatsModule stats;
  private EvaluationStats evaluator;
  private TranslationSourcesDisambiguator disambiguator;

  private void loadArgs(String[] args) {
    CommandLineParser parser = new DefaultParser();
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
      printUsage();
      System.exit(1);
    }

    remainingArgs = cmd.getArgs();

    // Check for args
    if (cmd.hasOption("h")) {
      printUsage();
      System.exit(0);
    }

    prefixDir = DEFAULT_PREFIX_DIR;
    if (cmd.hasOption(PREFIX_DIR_OPTION)) {
      prefixDir = cmd.getOptionValue(PREFIX_DIR_OPTION);
    }
    extractsDir = prefixDir + File.separator + "ontolex" + File.separator + "latest";
    statsDir = prefixDir + File.separator + "stats";
    doCompress = cmd.hasOption(COMPRESS_OPTION);

    rdfFormat = cmd.getOptionValue(RDF_FORMAT_OPTION, DEFAULT_RDF_FORMAT);
    rdfFormat = rdfFormat.toUpperCase();

    stats = new TranslationGlossesStatsModule();
    evaluator = new EvaluationStats();
    disambiguator = new TranslationSourcesDisambiguator(0.1, 0.9, 0.05, true, stats, evaluator);

  }

  public static void main(String args[]) throws Exception {
    EnhanceLatestExtracts cliProg = new EnhanceLatestExtracts();
    cliProg.loadArgs(args);
    cliProg.enhanceExtract();

  }

  private void enhanceExtract() throws Exception {

    File d = new File(extractsDir);
    File ds = new File(statsDir);

    if (!d.isDirectory()) {
      System.err.println("Extracts directory not found: " + extractsDir);
    }
    if (!ds.isDirectory()) {
      ds.mkdirs();
    }

    String enhConfidenceFile = statsDir + File.separator + "latest_enhancement_confidence.csv";
    Map<String, String> enhConfidence = readAndParseStats(enhConfidenceFile);

    String glossStatsFile = statsDir + File.separator + "latest_glosses_stats.csv";
    Map<String, String> glossStats = readAndParseStats(glossStatsFile);

    for (File e : d.listFiles((dir, name) -> name.matches(".._dbnary_ontolex\\..*"))) {
      String l2 = e.getName().substring(0, 2);
      String language = LangTools.getCode(l2);
      String elang = LangTools.inEnglish(language);

      String checksum = getCheckSumColumn(enhConfidence.get(elang));
      String md5 = getMD5Checksum(e);
      if (md5.equals(checksum)) {
        System.err.println("Ignoring already available enhancements for: " + e.getName());
        continue;
      }

      System.err.println("Enhancing: " + e.getName());

      try {
        Model inputModel = ModelFactory.createDefaultModel();
        InputStream in = new FileInputStream(e);
        if (e.getName().endsWith(".bz2")) {
          in = new BZip2CompressorInputStream(in);
        }
        inputModel.read(in, null, this.rdfFormat);

        Model outputModel = ModelFactory.createDefaultModel();
        outputModel.setNsPrefixes(inputModel.getNsPrefixMap());

        disambiguator.processTranslations(inputModel, outputModel, language);

        outputAndLink(l2, e.getPath(), outputModel);

        // Update disambiguation confidence
        StringWriter ow = new StringWriter();

        evaluator.printStat(language, new PrintWriter(ow));
        String stat = ow.toString();
        stat = elang + "," + md5 + "," + stat;
        enhConfidence.put(elang, stat);

        // Update gloss statistics
        ow = new StringWriter();
        stats.printStat(language, new PrintWriter(ow));
        stat = ow.toString();
        stat = elang + "," + stat;
        glossStats.put(elang, stat);

      } catch (Exception ex) {
        System.err.println("Exception caught while computing disambiguations for: " + e.getName());
        System.err.println(ex.getLocalizedMessage());
        ex.printStackTrace(System.err);
      }

    }

    // TODO: stats should be written after each language so that already computed languages will be
    // correctly ignored at next launch.
    writeStats(enhConfidence, "Language,MD5," + EvaluationStats.getHeaders(), enhConfidenceFile);
    writeStats(glossStats, "Language," + TranslationGlossesStatsModule.getHeaders(),
        glossStatsFile);

  }

  protected void outputAndLink(String lang, String modelFile, Model m) {
    String outputModelFileName = null;

    Path modelPath = Paths.get(modelFile);

    Path latestDir = modelPath.getParent();
    if (null == latestDir) {
      System.err.println("Unexpected null parent for input model file...");
      System.exit(-1);
    }
    if (Files.isSymbolicLink(modelPath)) {
      try {
        Path link = Files.readSymbolicLink(modelPath);
        modelPath = latestDir.resolve(link).normalize();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    Path effectiveDir = modelPath.getParent();

    String filename = modelPath.getFileName().toString();
    if (filename.endsWith(".bz2")) {
      filename = filename.substring(0, filename.length() - 4);
    }
    outputModelFileName = effectiveDir.resolve(filename.replaceAll("_ontolex", "_enhancement"))
        .normalize().toString();

    OutputStream outputModelStream = null;

    try {
      if (doCompress) {
        outputModelFileName = outputModelFileName + ".bz2";
        outputModelStream = new FileOutputStream(outputModelFileName);
        outputModelStream = new BZip2CompressorOutputStream(outputModelStream);
      } else {
        outputModelStream = new FileOutputStream(outputModelFileName);
      }
      m.write(outputModelStream, this.rdfFormat);

      // Linking effective outputfile into latest folder
      linkToLatest(lang, latestDir, Paths.get(outputModelFileName));

    } catch (FileNotFoundException e) {
      System.err.println("Could not create output stream: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      return;
    } catch (IOException e) {
      System.err.println("IOException while creating output stream: " + e.getLocalizedMessage());
      e.printStackTrace();
      return;
    } finally {
      if (null != outputModelStream) {
        try {
          outputModelStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void linkToLatest(String lang, Path latestFolder, Path effectiveEnhancement) {

    if (!effectiveEnhancement.toFile().exists()) {
      System.err.println("Enhanced file " + effectiveEnhancement
          + " does not exists. I will not link to this version.");
      return;
    }

    String latestLinkName = lang + "_dbnary_enhancement.ttl";
    if (doCompress) {
      latestLinkName = latestLinkName + ".bz2";
    }

    Path latestFile = latestFolder.resolve(latestLinkName);
    if (Files.exists(latestFile) && !Files.isSymbolicLink(latestFile)) {
      // If no symbolic link, then there is a problem (maybe latest file and effective files are the
      // same...
      System.err.println("I'd like to link " + latestFile + " to " + effectiveEnhancement
          + " but the former exists and is not a link...");
      System.err.println("Symbolic link creation aborted.");
      return;
    }

    try {
      Files.deleteIfExists(latestFile);
    } catch (IOException e) {
      System.err.format("IOException while attempting to delete file '%s'.", latestFile);
    }

    String linkTo =
        Paths.get("..").resolve(lang).resolve(effectiveEnhancement.getFileName()).toString();
    try {
      String[] args = {"ln", "-s", linkTo, latestLinkName};
      Runtime.getRuntime().exec(args, null, new File(extractsDir));
    } catch (IOException e) {
      System.err
          .println("Error while trying to link to latest extract: " + latestFile + "->" + linkTo);
      e.printStackTrace(System.err);
    }
  }

  private void writeStats(Map<String, String> gstats, String headers, String gstatFile)
      throws IOException {
    File gs = new File(gstatFile);

    if (!gs.exists() || (gs.isFile() && gs.canWrite())) {
      PrintWriter stats = new PrintWriter(gs, "UTF-8");
      // Print Header
      stats.println(headers);
      for (String s : gstats.values()) {
        stats.println(s);
      }
      stats.flush();
      stats.close();
    }
  }

  private String getCheckSumColumn(String s) {
    return (null == s) ? null : s.split(",")[1];
  }

  private Map<String, String> readAndParseStats(String gstatFile) throws IOException {
    TreeMap<String, String> m = new TreeMap<String, String>();

    File gs = new File(gstatFile);

    if (gs.isFile() && gs.canRead()) {
      BufferedReader br =
          new BufferedReader(new InputStreamReader(new FileInputStream(gs), "UTF-8"));
      String h = br.readLine(); // reading header
      String s = br.readLine();
      while (s != null) {
        String line[] = s.split(",");
        m.put(line[0], s);
        s = br.readLine();
      }
      br.close();
    }
    return m;
  }

  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    String help = "Update Latest statistics based on latest extracts.";
    formatter.printHelp("java -cp /path/to/dbnary.jar "
        + EnhanceLatestExtracts.class.getCanonicalName() + "[OPTIONS]", "With OPTIONS in:", options,
        help, false);
  }

  public static byte[] createChecksum(File file) throws Exception {
    InputStream fis = new FileInputStream(file);

    byte[] buffer = new byte[4096];
    MessageDigest complete = MessageDigest.getInstance("MD5");
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    return complete.digest();
  }

  // see this How-to for a faster way to convert
  // a byte array to a HEX string
  public static String getMD5Checksum(File file) throws Exception {
    byte[] b = createChecksum(file);
    String result = "";

    for (int i = 0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }

}
