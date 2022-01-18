package org.getalp.dbnary.cli;

import static org.getalp.dbnary.ExtractionFeature.MAIN;

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
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.mixins.BatchExtractorMixin;
import org.getalp.dbnary.cli.mixins.ExtractionFeaturesMixin;
import org.getalp.dbnary.cli.utils.ExtractionPreferences;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "update", mixinStandardHelpOptions = true,
    header = "Update dumps for all specified languages, then extract them.",
    description = "Update/Downloads dumps from mediawiki mirrors, then process all pages and "
        + "extract lexical data according to options that are passed "
        + "to the program. The extracted lexical data is encoded as RDF graphs using ontolex, "
        + "lexinfo, olia and other standard vocabularies.")
public class UpdateAndExtractDumps implements Callable<Integer> {

  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  private BatchExtractorMixin batch; // injected by picocli
  @Mixin
  private ExtractionFeaturesMixin features; // injected by picocli

  private static final String DEFAULT_SERVER_URL = "http://dumps.wikimedia.org/";
  private String server;

  @CommandLine.Option(names = {"-s", "--server"}, paramLabel = "WIKTIONARY-DUMP_MIRROR URL",
      defaultValue = DEFAULT_SERVER_URL,
      description = "Use the specify URL to download dumps (Default: ${DEFAULT-VALUE}).")
  private void setServerUrl(String url) {
    if (!url.endsWith("/")) {
      url = url + "/";
    }
    this.server = url;
  }

  @CommandLine.Option(names = {"--no-network"}, negatable = true,
      description = "Do not connect to network (no dump update/download) but extract available dumps.")
  private boolean networkIsOff;

  @CommandLine.Option(names = {"--force"},
      description = "Force extraction even if extract already exists.")
  private boolean force;

  @CommandLine.Option(names = {"-k", "--keep"}, paramLabel = "N", defaultValue = "1",
      description = "Keep up to N previous dumps in the dumps folder (Default: ${DEFAULT-VALUE}).")
  private int historySize;

  @CommandLine.Option(names = {"-D", "--date"}, paramLabel = "YYYYMMDD",
      description = "Fetch/use the dump from given date instead of the latest one.")
  private String fetchDate = null;

  @CommandLine.Option(names = {"--sample"}, paramLabel = "N", defaultValue = "-1",
      description = "sample only the first N extracted entries.")
  private int sample = -1;


  private Path dumpsDir;
  private Path extractDir;
  private ExtractionPreferences prefs;

  @Parameters(index = "0..*", description = "The languages to be updated and extracted.",
      arity = "1..*")
  String[] languages;

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
    new CommandLine(new UpdateAndExtractDumps()).execute(args);
  }

  @Override
  public Integer call() {
    prefs = new ExtractionPreferences(parent.dbnaryDir);
    dumpsDir = prefs.getDumpDir();
    extractDir = prefs.getExtractionDir();
    updateAndExtract();
    return 0;
  }

  // TODO: Handle proxy parameter

  private String dumpFileName(String lang, String date) {
    return lang + "wiktionary-" + date + "-pages-articles.xml.bz2";
  }

  public void updateAndExtract() {
    List<LanguageConfiguration> confs = Arrays.stream(languages).distinct().sequential()
        .map(this::retrieveLastDump).collect(Collectors.toList());
    confs =
        confs.stream().parallel().map(this::uncompressRetrievedDump).collect(Collectors.toList());
    confs.stream().sequential().map(this::extract).map(this::removeOldDumps)
        .forEach(this::linkToLatestExtractedFiles);
  }

  private void linkToLatestExtractedFiles(LanguageConfiguration conf) {
    if (conf.isExtracted()) {
      System.err.format("[%s] ==> Linking to latest versions.%n", conf.lang);
      features.getEndolexFeatures()
          .forEach(f -> linkToLatestExtractFile(conf.lang, conf.dumpDir, f, false));
      if (null != features.getExolexFeatures())
        features.getExolexFeatures()
            .forEach(f -> linkToLatestExtractFile(conf.lang, conf.dumpDir, f, true));
    }
  }

  private void linkToLatestExtractFile(String lang, String dir, ExtractionFeature feature,
      boolean isExolex) {
    if (null == dir || dir.equals("")) {
      return;
    }

    Path latestdir = extractDir.resolve("latest");
    Path odir = extractDir.resolve(lang);
    try {
      Files.createDirectories(latestdir);
    } catch (IOException e) {
      System.err.format("Could not create directory '%s'.%n", latestdir);
      e.printStackTrace(System.err);
      return;
    }

    Path extractedFile = odir.resolve(ExtractionPreferences.outputFilename(feature, lang, dir,
        features.getOutputFormat(), batch.doCompress(), isExolex));
    if (!Files.exists(extractedFile)) {
      System.err.format(
          "Extracted wiktionary file %s does not exists. " + "I will not link to this version.%n",
          extractedFile);
      return;
    }

    Path latestFile = latestdir.resolve(ExtractionPreferences.outputFilename(feature, lang,
        features.getOutputFormat(), batch.doCompress(), isExolex));
    try {
      Files.deleteIfExists(latestFile);
    } catch (IOException e) {
      System.err.format("IOException while attempting to delete file '%s'.%n", latestFile);
    }
    Path linkTo = Paths.get("..", lang).resolve(extractedFile.getFileName());
    try {
      Files.createSymbolicLink(latestFile, linkTo);
    } catch (IOException e) {
      System.err.format("Error while trying to link to latest extract: %s -> %s%n", latestFile,
          linkTo);
      e.printStackTrace(System.err);
    }
  }

  private LanguageConfiguration removeOldDumps(LanguageConfiguration conf) {
    if (conf.isExtracted())
      cleanUpDumps(conf.lang, conf.dumpDir);
    else if (parent.isVerbose())
      System.err.println("Older dumps cleanup aborted as extraction did not succeed.");
    return conf;
  }

  private void cleanUpDumps(String lang, String lastDir) {

    // Do not cleanup if there has been any problems before...
    if (lastDir == null || lastDir.equals("")) {
      return;
    }

    String langDir = dumpsDir + "/" + lang;
    File[] dirs = new File(langDir).listFiles();

    if (null == dirs || dirs.length == 0) {
      return;
    }

    SortedSet<String> versions = new TreeSet<>();
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
    String dumpdir = dumpsDir + "/" + lang + "/" + dir;
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
    String dumpdir = dumpsDir + "/" + lang + "/" + dir;
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
      if (parent.isVerbose()) {
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
          if (parent.isVerbose()) {
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

        if (parent.isVerbose()) {
          System.err.println("Fetching latest version : " + lastDir);
        }

        String dumpdir = dumpsDir + "/" + lang + "/" + lastDir;
        File file = new File(dumpdir, dumpFileName(lang, lastDir));
        if (file.exists()) {
          return lastDir;
        }
        File dumpFile = new File(dumpdir);
        dumpFile.mkdirs();

        String dumpFileUrl = languageDumpFolder + "/" + lastDir + "/" + dumpFileName(lang, lastDir);
        if (parent.isVerbose()) {
          System.err.println("Fetching dump from " + dumpFileUrl);
        }
        HttpGet request = new HttpGet(dumpFileUrl);
        try (CloseableHttpResponse response = client.execute(request)) {
          HttpEntity entity = response.getEntity();

          if (entity != null) {
            if (parent.isVerbose()) {
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
    String langDir = dumpsDir + "/" + lang;
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

    String compressedDumpFile = dumpsDir + "/" + lang + "/" + dir + "/" + dumpFileName(lang, dir);
    String uncompressedDumpFile = uncompressDumpFileName(lang, dir);
    // System.err.println("Uncompressing " + compressedDumpFile);

    File file = new File(uncompressedDumpFile);
    if (file.exists()) {
      if (parent.isVerbose())
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
        Writer w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_16))) {

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
    return dumpsDir + "/" + lang + "/" + dir + "/" + lang + "wkt-" + dir + ".xml";
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

    displayMemoryUsage();
    Path odir = extractDir.resolve(lang);
    try {
      Files.createDirectories(odir);
    } catch (IOException e) {
      System.err.format("Could not create directory '%s'.%n", odir);
      e.printStackTrace(System.err);
      return false;
    }

    Path extractedFile = odir.resolve(ExtractionPreferences.outputFilename(MAIN, lang, dir,
        features.getOutputFormat(), batch.doCompress(), false));
    // TODO: correctly test for compressed file if compress is enabled

    if (Files.exists(extractedFile) && !force) {
      // System.err.println("Extracted wiktionary file " + extractFile + " already exists.");
      return true;
    }
    System.err.println("========= EXTRACTING file " + extractedFile + " ===========");

    ArrayList<String> a = new ArrayList<>();
    a.add("extract");
    a.add("-f");
    a.add(features.getOutputFormat());
    a.add("-l");
    a.add(lang);
    a.add("--dir");
    a.add(parent.dbnaryDir.toString());
    a.add("--suffix");
    a.add(dir);
    if (sample > 0) {
      a.add("--frompage");
      a.add("0");
      a.add("--topage");
      a.add(String.valueOf(sample));
    } else {
      if (batch.fromPage() > 0) {
        a.add("--frompage");
        a.add(String.valueOf(batch.fromPage()));
      }
      if (batch.toPage() != Integer.MAX_VALUE) {
        a.add("--topage");
        a.add(String.valueOf(batch.toPage()));
      }
    }
    if (batch.doCompress())
      a.add("--compress");
    else
      a.add("--no-compress");
    a.add("--endolex");
    a.add(String.join(",", features.getEndolexFeatures().stream().map(ExtractionFeature::toString)
        .toArray(String[]::new)));
    if (null != features.getExolexFeatures()) {
      a.add("--exolex");
      a.add(String.join(",", features.getExolexFeatures().stream().map(ExtractionFeature::toString)
          .toArray(String[]::new)));
    }
    if (batch.useTdb()) {
      a.add("--tdb");
    }
    if (parent.isVerbose()) {
      a.add("-v");
    }
    a.add(uncompressDumpFileName(lang, dir));

    String[] args = a.toArray(new String[0]);

    if (parent.isVerbose()) {
      System.err.println("Launching ExtractWiktionary with args : ");
      System.err.println(Arrays.stream(args).collect(Collectors.joining(" ")));
    }

    DBnary.main(args);
    displayMemoryUsage();
    return status;
  }

}
