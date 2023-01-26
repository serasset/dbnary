package org.getalp.dbnary.cli;

import static org.getalp.dbnary.ExtractionFeature.MAIN;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.cli.mixins.BatchExtractorMixin;
import org.getalp.dbnary.cli.mixins.ExtractionFeaturesMixin;
import org.getalp.dbnary.cli.utils.ExtractionPreferences;
import org.getalp.dbnary.cli.utils.ShortErrorMessageHandler;
import org.getalp.wiktionary.WiktionaryIndex;
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

  private static final String DEFAULT_SERVER_URL = "https://dumps.wikimedia.org/";
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


  private ExtractionPreferences prefs;

  @Parameters(index = "0..*", description = "The languages to be updated and extracted.",
      arity = "1..*")
  String[] languages;

  private static class LockReleaser extends Thread {

    private final Path lockFile;

    public LockReleaser(Path lockFile) {
      super();
      this.lockFile = lockFile;
    }

    @Override
    public void run() {
      if (null != lockFile) {
        System.err.format("Deleting lock file %s on exit hook.%n", lockFile);
        try {
          Files.delete(lockFile);
        } catch (IOException e) {
          System.err.format(e.getLocalizedMessage());
          System.err.format("Could not delete lock file '%s'. %n", lockFile);
        }
      }
      super.run();
    }
  }
  private static class LanguageConfiguration {

    String lang;
    String dumpDir;
    private boolean uncompressed = false;
    private boolean extracted = false;
    private Path lock = null;
    private LockReleaser lockRemovalHook = null;

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

    public boolean ownsLock() {
      return null != getLock();
    }

    public Path getLock() {
      return lock;
    }

    public void setLock(Path lock) {
      this.lock = lock;
    }

    public LockReleaser getLockRemovalHook() {
      return lockRemovalHook;
    }

    public void setLockRemovalHook(LockReleaser lockRemovalHook) {
      this.lockRemovalHook = lockRemovalHook;
    }
  }

  @Override
  public Integer call() {
    prefs = new ExtractionPreferences(parent.dbnaryDir);
    updateAndExtract();
    return 0;
  }

  // TODO: Handle proxy parameter

  public void updateAndExtract() {
    List<LanguageConfiguration> confs = Arrays.stream(languages).distinct().sequential()
        .map(this::retrieveLastDump).collect(Collectors.toList());
    confs =
        confs.stream().parallel().map(this::uncompressRetrievedDump).collect(Collectors.toList());
    confs.stream().sequential().map(this::checkLock).map(this::extract).map(this::removeOldDumps)
        .map(this::releaseLock).forEach(this::linkToLatestExtractedFiles);
  }

  private LanguageConfiguration checkLock(LanguageConfiguration conf) {
    if (conf.isUncompressed()) {
      Path lock = getLockPath(conf.lang, conf.dumpDir);
      if (null == lock || Files.exists(lock)) {
        System.err.format("Unable to secure lock file '%s'; Bailing out. %n", lock);
        return conf;
      }
      LockReleaser lockReleaser = new LockReleaser(lock);
      conf.setLockRemovalHook(lockReleaser);
      Runtime.getRuntime().addShutdownHook(lockReleaser);
      try (
          OutputStream lockStream =
              Files.newOutputStream(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
          Writer lockWriter = new OutputStreamWriter(lockStream, StandardCharsets.UTF_8)) {
        lockWriter.write(Long.toString(ProcessHandle.current().pid()));
      } catch (IOException e) {
        System.err.format(e.getLocalizedMessage());
        System.err.format("Could not write to lock file '%s'; Bailing out. %n", lock);
        return conf;
      }
      conf.setLock(lock);
    }
    return conf;
  }

  private Path getLockPath(String lang, String dir) {
    if (null == dir || dir.equals("")) {
      return null;
    }

    Path odir = prefs.getExtractionDir(lang);
    try {
      Files.createDirectories(odir);
    } catch (IOException e) {
      System.err.format("Could not create directory '%s'.%n", odir);
      // e.printStackTrace(System.err);
      return null;
    }

    Path extractedFile = prefs.outputFileForFeature(MAIN, lang, dir, features.getOutputFormat(),
        batch.doCompress(), false);
    return Paths.get(extractedFile.toString() + ".lck");
  }

  private LanguageConfiguration releaseLock(LanguageConfiguration conf) {
    if (conf.ownsLock()) {
      Runtime.getRuntime().removeShutdownHook(conf.getLockRemovalHook());
      try {
        Files.delete(conf.getLock());
      } catch (IOException e) {
        System.err.format(e.getLocalizedMessage());
        System.err.format("Could not delete lock file '%s'. %n", conf.getLock());
        return conf;
      }
    }
    return conf;
  }


  private void linkToLatestExtractedFiles(LanguageConfiguration conf) {
    if (conf.isExtracted()) {
      System.err.format("[%s] ==> Linking to latest versions.%n", conf.lang);
      features.getEndolexFeatures()
          .forEach(f -> linkToLatestExtractFile(conf.lang, conf.dumpDir, f, false));
      if (null != features.getExolexFeatures()) {
        features.getExolexFeatures()
            .forEach(f -> linkToLatestExtractFile(conf.lang, conf.dumpDir, f, true));
      }
    }
  }

  private void linkToLatestExtractFile(String lang, String dir, ExtractionFeature feature,
      boolean isExolex) {
    if (null == dir || dir.equals("")) {
      return;
    }

    Path latest = prefs.getLatestExtractionDir();
    try {
      Files.createDirectories(latest);
    } catch (IOException e) {
      System.err.format("Could not create directory '%s'.%n", latest);
      // e.printStackTrace(System.err);
      return;
    }

    Path extractedFile = prefs.outputFileForFeature(feature, lang, dir, features.getOutputFormat(),
        batch.doCompress(), isExolex);
    if (!Files.exists(extractedFile)) {
      System.err.format(
          "Extracted wiktionary file %s does not exists. " + "I will not link to this version.%n",
          extractedFile);
      return;
    }

    Path latestFile = latest.resolve(ExtractionPreferences.outputFilename(feature, lang,
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
      // e.printStackTrace(System.err);
    }
  }

  private LanguageConfiguration removeOldDumps(LanguageConfiguration conf) {
    if (conf.isExtracted()) {
      cleanUpDumps(conf.lang, conf.dumpDir);
    } else if (parent.isVerbose()) {
      System.err.println("Older dumps cleanup aborted as extraction did not succeed.");
    }
    return conf;
  }

  private void cleanUpDumps(String lang, String lastDir) {

    // Do not clean up if there has been any problems before...
    if (lastDir == null || lastDir.equals("")) {
      return;
    }

    SortedSet<String> versions = getAvailableDumpsVersions(lang);

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

  private SortedSet<String> getAvailableDumpsVersions(String lang) {
    SortedSet<String> versions;
    try (Stream<Path> files = Files.list(prefs.getDumpDir(lang))) {
      versions = files.filter(Files::isDirectory).map(Path::getFileName).map(Path::toString)
          .collect(Collectors.toCollection(TreeSet::new));
    } catch (IOException e) {
      System.err
          .println("IOException while getting available dump versions: " + e.getLocalizedMessage());
      return new TreeSet<>();
    }
    return versions;
  }


  private void deleteDumpDir(String lang, String dir) {
    Path dumpDirectory = prefs.getDumpDir(lang).resolve(dir);

    try (Stream<Path> files = Files.list(dumpDirectory)) {
      if (files.findAny().isEmpty()) {
        System.err.println("Deleting dump directory: " + dumpDirectory);
        Files.delete(dumpDirectory);
      } else {
        System.err.println("Could not delete non empty dir: " + dumpDirectory);
      }
    } catch (IOException e) {
      System.err.println("IOException while attempting deletion of " + dumpDirectory + ": "
          + e.getLocalizedMessage());
    }
  }

  private void deleteUncompressedDump(String lang, String dir) {
    Path dump = prefs.expandedDump(lang, dir);
    deleteSilently(dump, "Deleting expanded dump: ");
    deleteSilently(WiktionaryIndex.indexFile(dump), "Deleting expanded dump: ");
  }

  private void deleteSilently(Path dump, String message) {
    if (Files.exists(dump)) {
      System.err.println(message + dump);
      try {
        Files.delete(dump);
      } catch (IOException ignored) {
        // Silence is golden
      }
    }
  }


  private void deleteDump(String lang, String dir) {
    Path dump = prefs.originalDump(lang, dir);

    deleteSilently(dump, "Deleting compressed dump: ");
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
      if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) { // URL protocol
                                                                                     // is not http
        System.err.format("Unsupported protocol: %s", url.getProtocol());
        return defaultRes;
      }

      String languageDumpFolder = server + lang + "wiktionary";
      String lastDir;

      try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
        System.err.println("Updating " + lang);

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

        Path dump = prefs.originalDump(lang, lastDir);
        if (Files.exists(dump)) {
          return lastDir;
        }

        Files.createDirectories(dump.getParent());

        String dumpFileUrl = languageDumpFolder + "/" + lastDir + "/"
            + ExtractionPreferences.originalDumpFilename(lang, lastDir);
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
            try (OutputStream dfile = Files.newOutputStream(dump)) {
              System.err.println("====>  Retrieving new dump for " + lang + ": " + lastDir);
              long s = System.currentTimeMillis();
              entity.writeTo(dfile);
              System.err.println("Retrieved " + dump.getFileName() + "["
                  + (System.currentTimeMillis() - s) + " ms]");
            }
          }
        }
      } catch (IOException e) {
        System.err.println("IOException while retrieving dump: " + e.getLocalizedMessage());
        // e.printStackTrace();
        return null;
      }

      return lastDir;
    } catch (MalformedURLException e) {
      System.err.format("Malformed dump server URL: %s", server);
      System.err.format("Using locally available dump.");
      // e.printStackTrace();
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
    try (InputStream is = entity.getContent()) {
      Document doc = Jsoup.parse(is, "UTF-8", url);

      Elements links = doc.select("a");
      for (Element link : links) {
        String href = link.attr("href");
        if (href.length() > 0) {
          if (href.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
          }
          folders.add(href);
        }
      }
    } catch (IOException e) {
      System.err.format("IOException while parsing retrieved folder index.");
    }
    return folders;
  }

  private String getLastLocalDumpDir(String lang) {
    SortedSet<String> versions = getAvailableDumpsVersions(lang);
    return (versions.isEmpty()) ? null : versions.first();
  }

  private static final String versionPattern = "\\d{8}";
  private static final Pattern vpat = Pattern.compile(versionPattern);

  private String getLastVersionDir(SortedSet<String> dirs) {
    if (null == dirs) {
      return null;
    }

    String res = null;
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

    Path compressedDump = prefs.originalDump(lang, dir);
    Path expandedDump = prefs.expandedDump(lang, dir);
    if (Files.exists(expandedDump)) {
      if (parent.isVerbose()) {
        System.err.println("Uncompressed dump file " + expandedDump + " already exists.");
      }
      return true;
    }

    System.err.println("uncompressing file : " + compressedDump + " to " + expandedDump);

    try (
        BZip2CompressorInputStream bzIn =
            new BZip2CompressorInputStream(Files.newInputStream(compressedDump));
        Reader r = new BufferedReader(new InputStreamReader(bzIn, StandardCharsets.UTF_8));

        OutputStream out = Files.newOutputStream(expandedDump);
        Writer w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_16))) {

      final char[] buffer = new char[4096];
      int len;
      while ((len = r.read(buffer)) != -1) {
        w.write(buffer, 0, len);
      }
      System.err.println("Correctly uncompressed file : " + expandedDump);

    } catch (IOException e) {
      System.err.println("Caught an IOException while uncompressing dump: " + compressedDump);
      System.err.println(e.getLocalizedMessage());
      deleteSilently(compressedDump, "Removing faulty compressed file: ");
      deleteSilently(expandedDump, "Removing faulty uncompressed file: ");
      deleteSilently(WiktionaryIndex.indexFile(expandedDump), "Removing faulty index file: ");
      status = false;
    }
    return status;
  }

  private LanguageConfiguration extract(LanguageConfiguration conf) {
    if (conf.ownsLock()) {
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

    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    System.err.println("--------------------------------");
    System.err.format("free memory: %d M%n", freeMemory / 1048576);
    System.err.format("allocated memory: %d M%n", allocatedMemory / 1048576);
    System.err.format("max memory: %d M%n", maxMemory / 1048576);
    System.err.format("total free memory: %d M%n",
        (freeMemory + (maxMemory - allocatedMemory)) / 1048576);
    System.err.println("--------------------------------");
  }

  private boolean extractDumpFile(String lang, String dir) {
    if (null == dir || dir.equals("")) {
      return true;
    }

    displayMemoryUsage();
    Path odir = prefs.getExtractionDir(lang);
    try {
      Files.createDirectories(odir);
    } catch (IOException e) {
      System.err.format("Could not create directory '%s'.%n", odir);
      // e.printStackTrace(System.err);
      return false;
    }

    Path extractedFile = prefs.outputFileForFeature(MAIN, lang, dir, features.getOutputFormat(),
        batch.doCompress(), false);

    if (Files.exists(extractedFile) && !force) {
      if (parent.isVerbose()) {
        System.err.println("Extracted wiktionary file " + extractedFile + " already exists.");
      }
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
    a.add(parent.getDbnaryDir().toString());
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
    if (batch.doCompress()) {
      a.add("--compress");
    } else {
      a.add("--no-compress");
    }
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
    a.add(prefs.expandedDump(lang, dir).toString());

    String[] args = a.toArray(new String[0]);

    if (parent.isVerbose()) {
      System.err.println("Launching ExtractWiktionary with args : ");
      System.err.println(String.join(" ", args));
    }

    int extractionReturnCode = new CommandLine(new DBnary())
        .setParameterExceptionHandler(new ShortErrorMessageHandler()).execute(args);

    displayMemoryUsage();
    return extractionReturnCode == 0;
  }

}
