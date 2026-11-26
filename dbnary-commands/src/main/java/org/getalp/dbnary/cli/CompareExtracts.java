package org.getalp.dbnary.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.riot.RDFLanguages;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.cli.mixins.ExtractionFeaturesMixin;
import org.getalp.dbnary.cli.utils.ExtractionPreferences;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import tdb2.tdbloader;

@Command(name = "compare", mixinStandardHelpOptions = true, header = "fetch and compare extracts from different dates or local directories.",
    description = "Fetches 2 different dumps (or uses local extracts) and compute their differences.")
public class CompareExtracts implements Callable<Integer> {

  private static final long IN_MEMORY_FILESIZE_LIMIT = 100000000;
  // CommandLine specification
  @Spec
  protected CommandSpec spec;
  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  protected ExtractionFeaturesMixin features;

  private static final String DEFAULT_SERVER_URL = "http://kaiko.getalp.org/static/";
  private String server;

  @Option(names = {"-s", "--server"}, paramLabel = "KAIKO_STATIC URL", defaultValue = DEFAULT_SERVER_URL,
      description = "Use the specify URL to download dumps (Default: ${DEFAULT-VALUE}).")
  private void setServerUrl(String url) {
    if (!url.endsWith("/")) {
      url = url + "/";
    }
    this.server = url;
  }

  static class FromSpec {

    @Option(names = {"--from", "--from-date"}, paramLabel = "YYYYMMDD", required = true, description = "Fetch and base the comparison from given date")
    String date = null;

    @Option(names = {"--from-file", "--from-dir"}, paramLabel = "DIR", required = true,
        description = "Base the comparison on local extracts found in DIR (or DIR/<lang>), as produced by the extract command (.ttl or .ttl.bz2).")
    Path dir = null;
  }

  static class ToSpec {

    @Option(names = {"--to", "--to-date"}, paramLabel = "YYYYMMDD", required = true,
        description = "Specify the date of the target dump. If unspecified, latest dump will be used.")
    String date = null;

    @Option(names = {"--to-file", "--to-dir"}, paramLabel = "DIR", required = true,
        description = "Use local extracts found in DIR (or DIR/<lang>) as target, as produced by the extract command (.ttl or .ttl.bz2).")
    Path dir = null;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  private FromSpec from = new FromSpec();

  @ArgGroup(exclusive = true, multiplicity = "0..1")
  private ToSpec to = new ToSpec();

  @Option(names = {"--max_diffs", "--diffs"}, paramLabel = "123", required = false, description = "Stop diff computation after finding max_diffs differences")
  private Integer maxDiffs = Integer.MAX_VALUE;

  @Option(names = {"--max_triples", "--triples"}, paramLabel = "123", required = false,
      description = "Stop diff computation after checking max_triples triples")
  private Integer maxTriples = Integer.MAX_VALUE;

  @Option(names = {"-o", "--output"}, paramLabel = "DIR", defaultValue = ".", description = "create files in DIR (default: ${DEFAULT-VALUE})")
  private Path output = Path.of(".");

  @Parameters(index = "0..*", description = "The languages to be updated and extracted.", arity = "1..*")
  String[] languages;

  private Path fromDir;
  private Path toDir;
  private Path diffDir;

  public Integer call() {
    return fetchAndCompareDumps();
  }

  private Integer fetchAndCompareDumps() {
    try {
      fromDir = Files.createDirectories(output.resolve("from"));
      toDir = Files.createDirectories(output.resolve("to"));
      fetchRequestedDumps();
      prepareTDBForBigDumps();
      diffDir = Files.createDirectories(output.resolve("diffs"));
      compareExtracts();
      return 0;
    } catch (IOException e) {
      spec.commandLine().getErr().println("Could not create temporary directory. Aborting.");
      return -1;
    }
  }

  /**
   * Returns the turtle file to be used for the given extract on one side of the comparison: either
   * the file fetched in the side's working dir, or the local file found in the user given directory
   * (bz2 local files are uncompressed in the side's working dir).
   *
   * @return the turtle file or null if no local file was found.
   */
  private Path extractFor(boolean isFromSide, String lang, ExtractionFeature ft, boolean isExolex) {
    Path workDir = isFromSide ? fromDir : toDir;
    Path localDir = isFromSide ? from.dir : to.dir;
    String date = isFromSide ? from.date : to.date;
    String turtleName = ExtractionPreferences.outputFilename(ft, lang, date, "TURTLE", false, isExolex);
    if (null == localDir) {
      return workDir.resolve(turtleName);
    }
    Path local = localExtract(localDir, lang, ft, isExolex);
    if (null == local || !local.getFileName().toString().endsWith(".bz2")) {
      return local;
    }
    Path uncompressed = workDir.resolve(turtleName);
    if (!Files.isReadable(uncompressed)) {
      try (InputStream in = Files.newInputStream(local)) {
        decompress(in, uncompressed, local.toString());
      } catch (IOException e) {
        spec.commandLine().getErr().format("IOException while uncompressing %s: %s%n", local, e.getLocalizedMessage());
        return null;
      }
    }
    return uncompressed;
  }

  /**
   * Find the extract of the given language and feature in dir or dir/lang, named as produced by the
   * extract command (lang_dbnary_[exolex_]feature[_suffix].ttl[.bz2]).
   */
  private Path localExtract(Path dir, String lang, ExtractionFeature ft, boolean isExolex) {
    String baseName = ExtractionPreferences.outputFilename(ft, lang, null, "TURTLE", false, isExolex);
    String prefix = baseName.substring(0, baseName.lastIndexOf('.'));
    String shortLang = prefix.substring(0, prefix.indexOf("_dbnary_"));
    List<Path> candidates = Stream.of(dir, dir.resolve(shortLang)).filter(Files::isDirectory).flatMap(d -> {
      try (Stream<Path> files = Files.list(d)) {
        return files.toList().stream();
      } catch (IOException e) {
        return Stream.empty();
      }
    }).filter(f -> {
      String n = f.getFileName().toString();
      if (!n.startsWith(prefix) || !(n.endsWith(".ttl") || n.endsWith(".ttl.bz2"))) {
        return false;
      }
      String rest = n.substring(prefix.length());
      return rest.startsWith(".") || rest.startsWith("_");
    }).toList();
    if (candidates.size() > 1) {
      spec.commandLine().getErr().format("Several local extracts match %s in %s, ignoring them: %s%n", baseName, dir, candidates);
      return null;
    }
    return candidates.isEmpty() ? null : candidates.get(0);
  }

  private void prepareTDBForBigDumps() {
    Arrays.stream(languages).distinct().sequential().forEach(this::prepareTDBForBigDumps);
  }

  private void prepareTDBForBigDumps(String lang) {
    for (boolean isFromSide : new boolean[] {true, false}) {
      Path workDir = isFromSide ? fromDir : toDir;
      features.getEndolexFeatures().forEach(ft -> createTDBIfTooBig(extractFor(isFromSide, lang, ft, false), workDir));
      features.getExolexFeatures().forEach(ft -> createTDBIfTooBig(extractFor(isFromSide, lang, ft, true), workDir));
    }
  }

  private static Path tdbFor(Path turtleFile, Path workDir) {
    return workDir.resolve(turtleFile.getFileName().toString() + ".tdb");
  }

  private void createTDBIfTooBig(Path turtleFile, Path workDir) {
    if (null == turtleFile || !Files.isReadable(turtleFile)) {
      return;
    }
    try {
      Path tdbPath = tdbFor(turtleFile, workDir);
      if (Files.exists(tdbPath)) {
        if (Files.isDirectory(tdbPath)) {
          spec.commandLine().getErr().format(
              "TDB directory %s already exists:  ==> I will use it %n" + "      (if you want it to be recreated relaunch command after removing the folder).%n",
              tdbPath);
        } else {
          spec.commandLine().getErr().format("A file named %s already exists:  ==> Stopping.%n", tdbPath);
          System.exit(-1);
        }
      } else {
        long turtleFileSize = Files.size(turtleFile);
        if (turtleFileSize > IN_MEMORY_FILESIZE_LIMIT) {
          if (parent.isVerbose()) {
            spec.commandLine().getErr().format("Creating TDB for %s (file size: %d) %n", turtleFile, turtleFileSize);
          }
          ArrayList<String> a = new ArrayList<>();
          // --loc "$tdbfile" "$ttlfile"
          a.add("--loc");
          a.add(tdbPath.toString());
          a.add(turtleFile.toString());
          String[] args = a.toArray(new String[0]);
          tdbloader.main(args);
        }
      }

    } catch (IOException e) {
      spec.commandLine().getErr().println("IOException while preparing TDB: " + e.getLocalizedMessage());
      System.exit(-2);
    }
  }


  private void compareExtracts() {
    Arrays.stream(languages).distinct().parallel().forEach(this::compareExtracts);
  }

  private void compareExtracts(String lang) {
    features.getEndolexFeatures().forEach(ft -> compare(lang, ft, false));
    features.getExolexFeatures().forEach(ft -> compare(lang, ft, true));
  }

  private void compare(String lang, ExtractionFeature ft, boolean isExolex) {
    Path fromFile = extractFor(true, lang, ft, isExolex);
    Path toFile = extractFor(false, lang, ft, isExolex);
    if (null == fromFile || null == toFile) {
      spec.commandLine().getErr().format("Could not compare %s %s%s: a file does not exist.%n", lang, isExolex ? "exolex " : "", ft);
      return;
    }
    Path fromTDB = tdbFor(fromFile, fromDir);
    Path toTDB = tdbFor(toFile, toDir);
    if (Files.isDirectory(fromTDB)) {
      fromFile = fromTDB;
    }
    if (Files.isDirectory(toTDB)) {
      toFile = toTDB;
    }
    if ((Files.isReadable(fromFile) || Files.isDirectory(fromFile)) && (Files.isReadable(toFile) || Files.isDirectory(toFile))) {
      try {
        Path lost = diffDir.resolve(lang + "_lost_" + (isExolex ? "exolex_" : "") + ft + ".ttl");
        Path gain = diffDir.resolve(lang + "_gain_" + (isExolex ? "exolex_" : "") + ft + ".ttl");
        spec.commandLine().getErr().format("Comparing %s and %s%n", fromFile, toFile);
        spec.commandLine().getErr().format("    --> %s%n", lost);
        ArrayList<String> a = new ArrayList<>();
        if (parent.isVerbose())
          a.add("-v");
        if (maxDiffs < Integer.MAX_VALUE) {
          a.add("-d");
          a.add(maxDiffs.toString());
        }
        if (maxTriples < Integer.MAX_VALUE) {
          a.add("-t");
          a.add(maxTriples.toString());
        }
        a.add(String.valueOf(fromFile));
        a.add(String.valueOf(toFile));
        a.add(String.valueOf(lost));
        RDFDiff.main(a.toArray(new String[0]));
        spec.commandLine().getErr().format("    --> %s%n", gain);
        a.clear();
        if (parent.isVerbose())
          a.add("-v");
        if (maxDiffs < Integer.MAX_VALUE) {
          a.add("-d");
          a.add(maxDiffs.toString());
        }
        if (maxTriples < Integer.MAX_VALUE) {
          a.add("-t");
          a.add(maxTriples.toString());
        }
        a.add(String.valueOf(toFile));
        a.add(String.valueOf(fromFile));
        a.add(String.valueOf(gain));
        RDFDiff.main(a.toArray(new String[0]));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      spec.commandLine().getErr().format("Could not compare %s with %s. a file does not exist.%n", fromFile, toFile);
    }
  }

  private void fetchRequestedDumps() throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      Arrays.stream(languages).distinct().parallel().forEach(l -> retrieveDumps(l, client));
    } catch (IOException e) {
      spec.commandLine().getErr().println("Could not create HttpClient: " + e.getLocalizedMessage());
    }
  }


  private void retrieveDumps(String lang, CloseableHttpClient client) {
    if (null == from.dir) {
      features.getEndolexFeatures().forEach(ft -> fetchExtract(client, lang, ft, from.date, false, fromDir));
      features.getExolexFeatures().forEach(ft -> fetchExtract(client, lang, ft, from.date, true, fromDir));
    }
    if (null == to.dir) {
      features.getEndolexFeatures().forEach(ft -> fetchExtract(client, lang, ft, to.date, false, toDir));
      features.getExolexFeatures().forEach(ft -> fetchExtract(client, lang, ft, to.date, true, toDir));
    }
  }

  private Path fetchExtract(CloseableHttpClient client, String lang, ExtractionFeature f, String date, boolean isExolex, Path folder) {
    try {
      Path outputExtract = folder.resolve(ExtractionPreferences.outputFilename(f, lang, date, "TURTLE", false, isExolex));
      if (Files.isReadable(outputExtract)) {
        spec.commandLine().getErr().format("File %s already exists, I won't fecth it again.", outputExtract);
        return outputExtract;
      }

      URL url = new URL(server);
      if (!url.getProtocol().equals("http")) { // URL protocol is not https
        spec.commandLine().getErr().format("Unsupported protocol: %s", url.getProtocol());
        return null;
      }

      String extractFilename = ExtractionPreferences.outputFilename(f, lang, date, RDFLanguages.TURTLE.getName(), true, isExolex);
      String fullExtractURL = server + "ontolex/" + ((null == date) ? "latest" : lang) + "/" + extractFilename;
      if (parent.isVerbose()) {
        spec.commandLine().getErr().format("Fetching %s%n", fullExtractURL);
      }

      HttpGet request = new HttpGet(fullExtractURL);
      try (CloseableHttpResponse response = client.execute(request)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          spec.commandLine().getErr().format("Fetching: %s ==> Unexpected Response %s%n", fullExtractURL, response.getStatusLine().toString());
          return null;
        }
        HttpEntity entity = response.getEntity();

        if (entity != null) {
          if (parent.isVerbose()) {
            spec.commandLine().getErr().println("Retrieving and uncompressing data from : " + fullExtractURL);
          }
          decompress(entity.getContent(), outputExtract, fullExtractURL);
        }
      } catch (IOException e) {
        spec.commandLine().getErr().println("IOException while retrieving extract: " + e.getLocalizedMessage());

      }

      return outputExtract;
    } catch (MalformedURLException e) {
      spec.commandLine().getErr().format("Malformed dump server URL: %s", server);
      return null;
    }
  }

  private void decompress(InputStream in, Path outputExtract, String source) throws IOException {
    try (BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in); OutputStream out = Files.newOutputStream(outputExtract)) {
      long s = System.currentTimeMillis();
      final byte[] buffer = new byte[2048];
      int read, total = 0;
      while (-1 != (read = bzIn.read(buffer))) {
        if (parent.isVerbose()) {
          total += read;
          long len = bzIn.getUncompressedCount();
          final int percentage = (int) (total * 100 / len);
          spec.commandLine().getErr().format("%d/%d / %d %%. \r", total, len, percentage);
        }
        out.write(buffer, 0, read);
      }
      spec.commandLine().getErr().println("Uncompressed " + source + " to " + outputExtract + " [" + (System.currentTimeMillis() - s) + " ms]");
    }
  }

}
