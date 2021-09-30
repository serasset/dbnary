package org.getalp.dbnary.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class UpdateAndExtractDumps extends DBnaryCommandLine {

  private static final String FOREIGN_PREFIX = "_x";

  private static final String SERVER_URL_OPTION = "s";
  private static final String DEFAULT_SERVER_URL = "http://dumps.wikimedia.org/";

  private static final String NETWORK_OFF_OPTION = "n";

  private static final String TDB_OPTION = "tdb";

  private static final String FORCE_OPTION = "f";
  private static final boolean DEFAULT_FORCE = false;

  private static final String PREFIX_DIR_OPTION = "d";
  private static final String DEFAULT_PREFIX_DIR = ".";

  private static final String DEFAULT_MODEL = "ontolex";

  private static final String HISTORY_SIZE_OPTION = "k";
  private static final String DEFAULT_HISTORY_SIZE = "5";

  private static final String COMPRESS_OPTION = "z";
  private static final boolean DEFAULT_COMPRESS = true;

  private static final String FETCH_DATE_OPTION = "D";

  private static final String ENABLE_FEATURE_OPTION = "enable";

  private static final String SAMPLE_FEATURE_OPTION = "sample";

  private String outputDir;
  private String extractDir;
  private boolean tdbDir;
  private int historySize;
  private boolean force = DEFAULT_FORCE;
  private boolean compress = DEFAULT_COMPRESS;
  private String server = DEFAULT_SERVER_URL;
  private String model = DEFAULT_MODEL;
  private List<String> features = null;
  private String fetchDate = null;
  private int sample = -1;

  String[] remainingArgs;

  private boolean networkIsOff = false;


  static {
    options.addOption("h", false, "Prints usage and exits. ");
    options.addOption(SERVER_URL_OPTION, true,
        "give the URL pointing to a wikimedia mirror. " + DEFAULT_SERVER_URL + " by default.");
    options.addOption(FORCE_OPTION, false,
        "force the updating even if a file with the same name already exists in the output directory. "
            + DEFAULT_FORCE + " by default.");
    options.addOption(VERBOSE_OPTION, false, "Print more info while processing.");
    options.addOption(HISTORY_SIZE_OPTION, true,
        "number of dumps to be kept in output directory. " + DEFAULT_HISTORY_SIZE + " by default ");
    options.addOption(PREFIX_DIR_OPTION, true,
        "directory containing the wiktionary dumps and extracts. " + DEFAULT_PREFIX_DIR
            + " by default ");
    options.addOption(COMPRESS_OPTION, false,
        "compress the output file using bzip2." + DEFAULT_COMPRESS + " by default ");
    options.addOption(NETWORK_OFF_OPTION, false,
        "Do not use the ftp network, but decompress and extract.");
    options.addOption(FETCH_DATE_OPTION, true,
        "force the dump date to be retrieved. latest dump by default ");
    options.addOption(Option.builder().longOpt(SAMPLE_FEATURE_OPTION)
        .desc("sample only the first N extracted entries.").hasArg().argName("N").build());
    options.addOption(Option.builder().longOpt(ENABLE_FEATURE_OPTION).desc(
        "Enable additional extraction features (e.g. morphology,etymology,lime,enhancement,foreign).")
        .hasArg().argName("feature").build());
    options.addOption(Option.builder().longOpt(TDB_OPTION).desc(
        "Use the specified dir as a TDB to back the extractors models (use only for big extractions).")
        .build());
  }

  public UpdateAndExtractDumps(String[] args) {
    super(args);
    this.loadArgs();
  }

  private static class LanguageConfiguration {
    String lang;
    String dumpDir;
    private boolean uncompressed = false;
    private boolean extracted = false;

    public LanguageConfiguration(String lang, String dumpDir) {
      this.lang = lang;
      this.dumpDir = dumpDir;
    }

    public void setUncompressed(boolean status) {
      uncompressed = status;
    }

    public boolean isUncompressed() {
      return uncompressed;
    }

    public void setExtracted(boolean ok) {
      extracted = ok;
    }

    public boolean isExtracted() {
      return extracted;
    }
  }

  public static void main(String[] args) throws WiktionaryIndexerException, IOException {
    UpdateAndExtractDumps cliProg = new UpdateAndExtractDumps(args);
    cliProg.updateAndExtract();
  }

  // TODO: Handle proxy parameter

  private String dumpFileName(String lang, String date) {
    return lang + "wiktionary-" + date + "-pages-articles.xml.bz2";
  }

  /**
   * Validate and set command line arguments. Exit after printing usage if anything is astray
   *
   */
  private void loadArgs() {

    String h = cmd.getOptionValue(HISTORY_SIZE_OPTION, DEFAULT_HISTORY_SIZE);
    historySize = Integer.parseInt(h);

    if (cmd.hasOption(SERVER_URL_OPTION)) {
      server = cmd.getOptionValue(SERVER_URL_OPTION);
      if (!server.endsWith("/")) {
        server = server + "/";
      }
    }

    if (cmd.hasOption(FETCH_DATE_OPTION)) {
      fetchDate = cmd.getOptionValue(FETCH_DATE_OPTION);
    }

    if (cmd.hasOption(SAMPLE_FEATURE_OPTION)) {
      String sampleOtion = cmd.getOptionValue(SAMPLE_FEATURE_OPTION);
      sample = Integer.parseInt(sampleOtion);
    }

    force = cmd.hasOption(FORCE_OPTION);

    compress = cmd.hasOption(COMPRESS_OPTION);

    networkIsOff = cmd.hasOption(NETWORK_OFF_OPTION);

    tdbDir = cmd.hasOption(TDB_OPTION);

    features = new ArrayList<>();
    if (cmd.hasOption(ENABLE_FEATURE_OPTION)) {
      String[] vs = cmd.getOptionValues(ENABLE_FEATURE_OPTION);
      for (String v : vs) {
        features.addAll(Arrays.asList(v.split("[,;]")));
      }

      ArrayList<String> fCopy = new ArrayList<>(features);
      for (ExtractionFeature f : ExtractionFeature.values()) {
        fCopy.remove(f.toString());
      }
      if (!fCopy.isEmpty()) {
        System.err.println("Unknown feature(s) : " + fCopy);
        printUsage();
        System.exit(1);
      }
    }

    String prefixDir = DEFAULT_PREFIX_DIR;
    if (cmd.hasOption(PREFIX_DIR_OPTION)) {
      prefixDir = cmd.getOptionValue(PREFIX_DIR_OPTION);
    }
    outputDir = prefixDir + "/dumps";
    extractDir = prefixDir + "/extracts";

    remainingArgs = cmd.getArgs();
    if (remainingArgs.length == 0) {
      printUsage();
      System.exit(1);
    }

  }

  public void updateAndExtract() {
    List<LanguageConfiguration> confs = Arrays.stream(remainingArgs).distinct().sequential()
        .map(this::retrieveLastDump).collect(Collectors.toList());
    confs =
        confs.stream().parallel().map(this::uncompressRetrievedDump).collect(Collectors.toList());
    confs.stream().sequential().map(this::extract).map(this::removeOldDumps)
        .forEach(this::linkToLatestExtractedFiles);
  }

  private void linkToLatestExtractedFiles(LanguageConfiguration conf) {
    if (conf.isExtracted()) {
      System.err.format("[%s] ==> Linking to latest versions.%n", conf.lang);
      linkToLatestExtractFile(conf.lang, conf.dumpDir, model.toLowerCase());
      for (String f : features) {
        linkToLatestExtractFile(conf.lang, conf.dumpDir, f);
      }
    }
  }

  private void linkToLatestExtractFile(String lang, String dir, String feature) {
    if (null == dir || dir.equals("")) {
      return;
    }

    String latestdir = extractDir + "/" + model.toLowerCase() + "/latest";
    String odir = extractDir + "/" + model.toLowerCase() + "/" + lang;
    File d = new File(latestdir);
    d.mkdirs();

    String extractFile = odir + "/" + lang + "_dbnary_" + feature + "_" + dir + ".ttl";
    if (compress) {
      extractFile = extractFile + ".bz2";
    }
    File extractedFile = new File(extractFile);
    if (!extractedFile.exists()) {
      System.err.println("Extracted wiktionary file " + extractFile
          + " does not exists. I will not link to this version.");
      return;
    }

    String latestFile = latestdir + "/" + lang + "_dbnary_" + feature + ".ttl";
    if (compress) {
      latestFile = latestFile + ".bz2";
    }
    Path lf = Paths.get(latestFile);
    try {
      Files.deleteIfExists(lf);
    } catch (IOException e) {
      System.err.format("IOException while attempting to delete file '%s'.", lf);
    }
    try {
      String linkTo = "../" + lang + "/" + extractedFile.getName();
      String linkName = lang + "_dbnary_" + feature + ".ttl";
      if (compress) {
        linkName = linkName + ".bz2";
      }

      String[] args = {"ln", "-s", linkTo, linkName};
      Runtime.getRuntime().exec(args, null, d);
    } catch (IOException e) {
      System.err.println(
          "Error while trying to link to latest extract: " + latestFile + "->" + extractFile);
      e.printStackTrace(System.err);
    }
  }

  private LanguageConfiguration removeOldDumps(LanguageConfiguration conf) {
    if (conf.isExtracted())
      cleanUpDumps(conf.lang, conf.dumpDir);
    else if (verbose)
      System.err.println("Older dumps cleanup aborted as extraction did not succeed.");
    return conf;
  }

  private void cleanUpDumps(String lang, String lastDir) {

    // Do not cleanup if there has been any problems before...
    if (lastDir == null || lastDir.equals("")) {
      return;
    }

    String langDir = outputDir + "/" + lang;
    File[] dirs = new File(langDir).listFiles();

    if (null == dirs || dirs.length == 0) {
      return;
    }

    SortedSet<String> versions = new TreeSet<String>();
    for (File dir : dirs) {
      if (dir.isDirectory()) {
        versions.add(dir.getName());
      } else {
        System.err.println("Ignoring unexpected file: " + dir.getName());
      }
    }

    int vsize = versions.size();

    for (String v : versions) {
      if (!v.equals(lastDir)) {
        if (vsize > historySize) {
          deleteDump(lang, v);
        }
        if (vsize > 1) {
          deleteUncompressedDump(lang, v);
        }
        if (vsize > historySize) {
          deleteDumpDir(lang, v);
        }
      }
      vsize--;
    }
  }


  private void deleteDumpDir(String lang, String dir) {
    String dumpdir = outputDir + "/" + lang + "/" + dir;
    File f = new File(dumpdir);

    if (f.listFiles().length == 0) {
      System.err.println("Deleting dump directory: " + f.getName());
      f.delete();
    } else {
      System.err.println("Could not delete non empty dir: " + f.getName());
    }
  }

  private void deleteUncompressedDump(String lang, String dir) {
    String filename = uncompressDumpFileName(lang, dir);

    File f = new File(filename);

    if (f.exists()) {
      System.err.println("Deleting uncompressed dump: " + f.getName());
      f.delete();
    }

    File fidx = new File(filename + ".idx");
    if (fidx.exists()) {
      System.err.println("Deleting index file: " + fidx.getName());
      fidx.delete();
    }

  }


  private void deleteDump(String lang, String dir) {
    String dumpdir = outputDir + "/" + lang + "/" + dir;
    String filename = dumpdir + "/" + dumpFileName(lang, dir);

    File f = new File(filename);

    if (f.exists()) {
      System.err.println("Deleting compressed dump: " + f.getName());
      f.delete();
    }
  }

  private LanguageConfiguration retrieveLastDump(String lang) {
    return new LanguageConfiguration(lang, updateDumpFile(lang));
  }

  private String updateDumpFile(String lang) {
    String defaultRes = getLastLocalDumpDir(lang);
    if (networkIsOff) {
      if (verbose) {
        System.err.println("Using already existing dump : " + defaultRes);
      }
      return defaultRes;
    }

    try {
      URL url = new URL(server);
      if (!url.getProtocol().equals("http")) { // URL protocol is not http
        System.err.format("Unsupported protocol: %s", url.getProtocol());
        return defaultRes;
      }

      String languageDumpFolder = server + lang + "wiktionary";
      String lastDir;

      try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
        System.err.println("Updating " + lang);
        SortedSet<String> dirs = null;

        if (null != fetchDate) {
          if (verbose) {
            System.err.println("Using specified version date : " + fetchDate);
          }
          lastDir = fetchDate;
        } else {
          lastDir = getLatestDirFromServer(languageDumpFolder, client);
        }

        if (null == lastDir) {
          System.err.format("Empty directory list for %s (url=%s)%n", lang, languageDumpFolder);
          System.err.format("Using locally available dump.%n");
          return defaultRes;
        }

        if (verbose) {
          System.err.println("Fetching latest version : " + lastDir);
        }

        String dumpdir = outputDir + "/" + lang + "/" + lastDir;
        File file = new File(dumpdir, dumpFileName(lang, lastDir));
        if (file.exists()) {
          return lastDir;
        }
        File dumpFile = new File(dumpdir);
        dumpFile.mkdirs();

        String dumpFileUrl = languageDumpFolder + "/" + lastDir + "/" + dumpFileName(lang, lastDir);
        if (verbose) {
          System.err.println("Fetching dump from " + dumpFileUrl);
        }
        HttpGet request = new HttpGet(dumpFileUrl);
        try (CloseableHttpResponse response = client.execute(request)) {
          HttpEntity entity = response.getEntity();

          if (entity != null) {
            if (verbose) {
              System.err.println("Retrieving data from : " + dumpFileUrl);
            }
            try (FileOutputStream dfile = new FileOutputStream(file)) {
              System.err.println("====>  Retrieving new dump for " + lang + ": " + lastDir);
              long s = System.currentTimeMillis();
              entity.writeTo(dfile);
              System.err.println(
                  "Retrieved " + file.getName() + "[" + (System.currentTimeMillis() - s) + " ms]");
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }

      return lastDir;
    } catch (MalformedURLException e) {
      System.err.format("Malformed dump server URL: %s", server);
      System.err.format("Using locally available dump.");
      e.printStackTrace();
      return defaultRes;
    }
  }

  private String getLatestDirFromServer(String languageDumpFolder, CloseableHttpClient client)
      throws IOException {
    SortedSet<String> dirs;
    HttpGet request = new HttpGet(languageDumpFolder);
    try (CloseableHttpResponse response = client.execute(request)) {
      HttpEntity entity = response.getEntity();

      if (null == entity) {
        System.err.format("Could not retrieve directory listing (url=%s)%n", languageDumpFolder);
        System.err.format("Using locally available dump.%n");
        return null;
      }

      // parse directory listing to get the latest dump folder
      dirs = getFolderSetFromIndex(entity, languageDumpFolder);
      return getLastVersionDir(dirs);
    }
  }

  private SortedSet<String> getFolderSetFromIndex(HttpEntity entity, String url) {
    SortedSet<String> folders = new TreeSet<>();
    InputStream is = null;
    try {
      is = entity.getContent();
      Document doc = Jsoup.parse(entity.getContent(), "UTF-8", url);

      if (null == doc) {
        return folders;
      }
      Elements links = doc.select("a");
      for (Element link : links) {
        String href = link.attr("href");
        if (null != href && href.length() > 0) {
          if (href.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
          }
          folders.add(href);
        }
      }
    } catch (IOException e) {
      System.err.format("IOException while parsing retrieved folder index.");
    } finally {
      try {
        if (null != is) {
          is.close();
        }
      } catch (IOException e) {
      }
    }
    return folders;
  }

  private String getLastLocalDumpDir(String lang) {
    String langDir = outputDir + "/" + lang;
    File[] dirs = new File(langDir).listFiles();

    if (null == dirs || dirs.length == 0) {
      return null;
    }

    SortedSet<String> versions = new TreeSet<>();
    for (File dir : dirs) {
      if (dir.isDirectory()) {
        versions.add(dir.getName());
      } else {
        System.err.println("Ignoring unexpected file: " + dir.getName());
      }
    }

    return (versions.isEmpty()) ? null : versions.first();

  }

  private static String versionPattern = "\\d{8}";
  private static Pattern vpat = Pattern.compile(versionPattern);

  private String getLastVersionDir(SortedSet<String> dirs) {
    String res = null;
    if (null == dirs) {
      return res;
    }

    Matcher m = vpat.matcher("");
    for (String d : dirs) {
      m.reset(d);
      if (m.matches()) {
        res = d;
      }
    }
    return res;
  }

  private LanguageConfiguration uncompressRetrievedDump(LanguageConfiguration conf) {
    boolean status = uncompressDumpFile(conf.lang, conf.dumpDir);
    conf.setUncompressed(status);
    return conf;
  }

  private boolean uncompressDumpFile(String lang, String dir) {
    boolean status = true;
    if (null == dir || dir.equals("")) {
      return false;
    }

    String compressedDumpFile = outputDir + "/" + lang + "/" + dir + "/" + dumpFileName(lang, dir);
    String uncompressedDumpFile = uncompressDumpFileName(lang, dir);
    // System.err.println("Uncompressing " + compressedDumpFile);

    File file = new File(uncompressedDumpFile);
    if (file.exists()) {
      if (verbose)
        System.err.println("Uncompressed dump file " + uncompressedDumpFile + " already exists.");
      return true;
    }

    System.err
        .println("uncompressing file : " + compressedDumpFile + " to " + uncompressedDumpFile);

    try (
        BZip2CompressorInputStream bzIn =
            new BZip2CompressorInputStream(new FileInputStream(compressedDumpFile));
        Reader r = new BufferedReader(new InputStreamReader(bzIn, StandardCharsets.UTF_8));
        FileOutputStream out = new FileOutputStream(uncompressedDumpFile);
        Writer w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_16));) {



      final char[] buffer = new char[4096];
      int len;
      while ((len = r.read(buffer)) != -1) {
        w.write(buffer, 0, len);
      }
      System.err.println("Correctly uncompressed file : " + uncompressedDumpFile);

    } catch (IOException e) {
      System.err
          .println("Caught an IOException while uncompressing dump: " + dumpFileName(lang, dir));
      System.err.println(e.getLocalizedMessage());
      e.printStackTrace();
      System.err.println("Removing faulty compressed file: " + compressedDumpFile);
      File f = new File(compressedDumpFile);
      if (f.exists()) {
        f.delete();
      }
      status = false;
    }
    return status;
  }

  private String uncompressDumpFileName(String lang, String dir) {
    return outputDir + "/" + lang + "/" + dir + "/" + lang + "wkt-" + dir + ".xml";
  }

  private LanguageConfiguration extract(LanguageConfiguration conf) {
    if (conf.isUncompressed()) {
      boolean ok = extractDumpFile(conf.lang, conf.dumpDir);
      conf.setExtracted(ok);
      if (!ok) {
        // Sometimes the dump is incomplete and finishes in the middle of the xml file,
        // leading to IOException or IndexException from Extractor.
        deleteDump(conf.lang, conf.dumpDir);
        deleteUncompressedDump(conf.lang, conf.dumpDir);
        deleteDumpDir(conf.lang, conf.dumpDir);
      }
    }
    return conf;
  }

  private static void displayMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();

    NumberFormat format = NumberFormat.getInstance();

    StringBuilder sb = new StringBuilder();
    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
    sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
    sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
    sb.append("total free memory: "
        + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
    System.err.println("--------------------------------");
    System.err.println(sb);
    System.err.println("--------------------------------");
  }

  private boolean extractDumpFile(String lang, String dir) {
    boolean status = true;
    if (null == dir || dir.equals("")) {
      return true;
    }

    // TODO: GC and show used memory.
    displayMemoryUsage();
    String odir = extractDir + "/" + model.toLowerCase() + "/" + lang;
    File d = new File(odir);
    d.mkdirs();

    String prefix = "";
    if (features.contains("foreign")) {
      prefix = FOREIGN_PREFIX;
    }

    // TODO: correctly test for compressed file if compress is enabled
    String extractFile =
        odir + "/" + lang + prefix + "_dbnary_" + model.toLowerCase() + "_" + dir + ".ttl";
    String morphoFile =
        odir + "/" + lang + prefix + "_dbnary_" + ExtractionFeature.MORPHOLOGY + "_" + dir + ".ttl";
    String etymologyFile =
        odir + "/" + lang + prefix + "_dbnary_" + ExtractionFeature.ETYMOLOGY + "_" + dir + ".ttl";
    String limeFile =
        odir + "/" + lang + prefix + "_dbnary_" + ExtractionFeature.LIME + "_" + dir + ".ttl";
    String enhancementFile = odir + "/" + lang + prefix + "_dbnary_" + ExtractionFeature.ENHANCEMENT
        + "_" + dir + ".ttl";
    String statsFile =
        odir + "/" + lang + prefix + "_dbnary_" + ExtractionFeature.STATISTICS + "_" + dir + ".ttl";
    String foreignFile = odir + "/" + lang + prefix + "_dbnary_"
        + ExtractionFeature.FOREIGN_LANGUAGES + "_" + dir + ".ttl";
    if (compress) {
      extractFile = extractFile + ".bz2";
      morphoFile = morphoFile + ".bz2";
      etymologyFile = etymologyFile + ".bz2";
      limeFile = limeFile + ".bz2";
      enhancementFile = enhancementFile + ".bz2";
      statsFile = statsFile + ".bz2";
      foreignFile = foreignFile + ".bz2";
    }

    File file = new File(extractFile);
    if (file.exists() && !force) {
      // System.err.println("Extracted wiktionary file " + extractFile + " already exists.");
      return true;
    }
    System.err.println("========= EXTRACTING file " + extractFile + " ===========");

    ArrayList<String> a = new ArrayList<>();
    a.add("-f");
    a.add("turtle");
    a.add("-l");
    a.add(lang);
    a.add("-o");
    a.add(extractFile);
    if (sample > 0) {
      a.add("--frompage");
      a.add("0");
      a.add("--topage");
      a.add(String.valueOf(sample));
    }
    a.add("-z");
    a.add(compress ? "yes" : "no");
    if (features.contains("morphology")) {
      a.add("--morphology");
      a.add(morphoFile);
    }
    if (features.contains("etymology")) {
      a.add("--etymology");
      a.add(etymologyFile);
    }
    if (features.contains("lime")) {
      a.add("--lime");
      a.add(limeFile);
    }
    if (features.contains("enhancement")) {
      a.add("--enhancement");
      a.add(enhancementFile);
    }
    if (features.contains("statistics")) {
      a.add("--statistics");
      a.add(statsFile);
    }
    if (features.contains("foreign")) {
      a.add("-x");
      a.add("--foreign");
      a.add(foreignFile);
    }

    if (tdbDir) {
      a.add("--tdb");
    }
    if (verbose) {
      a.add("-v");
    }
    a.add(uncompressDumpFileName(lang, dir));

    String[] args = a.toArray(new String[0]);

    if (verbose) {
      System.err.println("Launching ExtractWiktionary with args : ");
      System.err.println(Arrays.stream(args).collect(Collectors.joining(" ")));
    }
    try {
      ExtractWiktionary.main(args);
    } catch (WiktionaryIndexerException e) {
      System.err.println("Caught IndexerException while extracting dump file: "
          + uncompressDumpFileName(lang, dir));
      System.err.println(e.getLocalizedMessage());
      e.printStackTrace();
      status = false;
    } catch (IOException e) {
      System.err.println(
          "Caught IOException while extracting dump file: " + uncompressDumpFileName(lang, dir));
      System.err.println(e.getLocalizedMessage());
      e.printStackTrace();
      status = false;
    }
    displayMemoryUsage();
    return status;
  }

  @Override
  protected void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar " + UpdateAndExtractDumps.class.getName()
            + " [OPTIONS] languageCode...",
        "With OPTIONS in:", options,
        "languageCode is the wiktionary code for a language (usually a 2 letter code).", false);
  }

}
