package org.getalp.dbnary.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.riot.RDFLanguages;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.cli.mixins.ExtractionFeaturesMixin;
import org.getalp.dbnary.cli.utils.ExtractionPreferences;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "compare", mixinStandardHelpOptions = true,
    header = "fetch and compare extracts from different dates.",
    description = "Fetches 2 different dumps and compute their differences.")
public class CompareExtracts implements Callable<Integer> {

  // CommandLine specification
  @Spec
  protected CommandSpec spec;
  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  protected ExtractionFeaturesMixin features;

  private static final String DEFAULT_SERVER_URL = "http://kaiko.getalp.org/static/";
  private String server;

  @CommandLine.Option(names = {"-s", "--server"}, paramLabel = "KAIKO_STATIC URL",
      defaultValue = DEFAULT_SERVER_URL,
      description = "Use the specify URL to download dumps (Default: ${DEFAULT-VALUE}).")
  private void setServerUrl(String url) {
    if (!url.endsWith("/")) {
      url = url + "/";
    }
    this.server = url;
  }

  @CommandLine.Option(names = {"--from", "--from-date"}, paramLabel = "YYYYMMDD", required = true,
      description = "Fetch and base the comparison from given date")
  private String fromDate = null;

  @CommandLine.Option(names = {"--to", "--to-date"}, paramLabel = "YYYYMMDD",
      description = "Specify the date of the target dump. If unspecified, latest dump will be used.")
  private String toDate = null;

  @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "DIR", defaultValue = ".",
      description = "create files in DIR (default: ${DEFAULT-VALUE})")
  private Path output = null;

  @Parameters(index = "0..*", description = "The languages to be updated and extracted.",
      arity = "1..*")
  String[] languages;

  private Path fromDir;
  private Path toDir;
  private Path diffDir;

  private static class LanguageConfiguration {
    String lang;
    Map<ExtractionFeature, Path> extracts = new HashMap<>();
    Map<ExtractionFeature, Path> exolexExtracts = new HashMap<>();

    private LanguageConfiguration(String lang) {
      this.lang = lang;
    }

    private Path geEndolextExtract(ExtractionFeature f) {
      return extracts.get(f);
    }

    private Path geExolextExtract(ExtractionFeature f) {
      return exolexExtracts.get(f);
    }

    private Path setEndolexExtract(ExtractionFeature f, Path p) {
      return extracts.put(f, p);
    }

    public Path setExolexExtract(ExtractionFeature f, Path p) {
      return exolexExtracts.put(f, p);
    }
  }

  public Integer call() {
    return fetchAndCompareDumps();
  }

  private Integer fetchAndCompareDumps() {
    try {
      fromDir = Files.createDirectories(output.resolve("from"));
      toDir = Files.createDirectories(output.resolve("to"));
      fetchRequestedDumps();
      diffDir = Files.createDirectories(output.resolve("diffs"));
      compareExtracts();
      return 0;
    } catch (IOException e) {
      spec.commandLine().getErr().println("Could not create temporary directory. Aborting.");
      return -1;
    }
  }

  private void compareExtracts() {
    Arrays.stream(languages).distinct().parallel().forEach(l -> compareExtracts(l));
  }

  private void compareExtracts(String lang) {
    features.getEndolexFeatures().forEach(ft -> compare(lang, ft, fromDate, toDate, false));
    features.getExolexFeatures().forEach(ft -> compare(lang, ft, fromDate, toDate, true));
    features.getEndolexFeatures().forEach(ft -> compare(lang, ft, fromDate, toDate, false));
    features.getExolexFeatures().forEach(ft -> compare(lang, ft, fromDate, toDate, true));
  }

  private void compare(String lang, ExtractionFeature ft, String fromDate, String toDate,
      boolean isExolex) {
    Path from = fromDir.resolve(
        ExtractionPreferences.outputFilename(ft, lang, fromDate, "TURTLE", false, isExolex));
    Path to = toDir
        .resolve(ExtractionPreferences.outputFilename(ft, lang, toDate, "TURTLE", false, isExolex));
    if (Files.isReadable(from) && Files.isReadable(to)) {
      try {
        Path lost = diffDir.resolve(lang + "_lost_" + (isExolex ? "exolex_" : "") + ft + ".ttl");
        Path gain = diffDir.resolve(lang + "_gain_" + (isExolex ? "exolex_" : "") + ft + ".ttl");
        spec.commandLine().getErr().format("Comparing %s and %s%n", from, to);
        spec.commandLine().getErr().format("    --> %s%n", lost);
        RDFDiff.main(new String[] {String.valueOf(from), String.valueOf(to), String.valueOf(lost)});
        spec.commandLine().getErr().format("    --> %s%n", gain);
        RDFDiff.main(new String[] {String.valueOf(to), String.valueOf(from), String.valueOf(gain)});
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      spec.commandLine().getErr().format("Could not compare %s with %s. a file does not exist.%n",
          from, to);
    }
  }

  private void fetchRequestedDumps() throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      Arrays.stream(languages).distinct().parallel().forEach(l -> retrieveDumps(l, client));
    } catch (IOException e) {
      spec.commandLine().getErr()
          .println("Could not create HttpClient: " + e.getLocalizedMessage());
    }
  }


  private void retrieveDumps(String lang, CloseableHttpClient client) {
    features.getEndolexFeatures()
        .forEach(ft -> fetchExtract(client, lang, ft, fromDate, false, fromDir));
    features.getExolexFeatures()
        .forEach(ft -> fetchExtract(client, lang, ft, fromDate, true, fromDir));
    features.getEndolexFeatures()
        .forEach(ft -> fetchExtract(client, lang, ft, toDate, false, toDir));
    features.getExolexFeatures().forEach(ft -> fetchExtract(client, lang, ft, toDate, true, toDir));
  }

  private Path fetchExtract(CloseableHttpClient client, String lang, ExtractionFeature f,
      String date, boolean isExolex, Path folder) {
    try {
      URL url = new URL(server);
      if (!url.getProtocol().equals("http")) { // URL protocol is not https
        System.err.format("Unsupported protocol: %s", url.getProtocol());
        return null;
      }

      String extractFilename = ExtractionPreferences.outputFilename(f, lang, date,
          RDFLanguages.TURTLE.getName(), true, isExolex);
      String fullExtractURL =
          server + "ontolex/" + ((null == date) ? "latest" : lang) + "/" + extractFilename;
      Path outputExtract = folder
          .resolve(ExtractionPreferences.outputFilename(f, lang, date, "TURTLE", false, isExolex));


      if (parent.isVerbose()) {
        spec.commandLine().getErr().format("Fetching %s%n", fullExtractURL);
      }

      HttpGet request = new HttpGet(fullExtractURL);
      try (CloseableHttpResponse response = client.execute(request)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          spec.commandLine().getErr().format("Fetching: %s ==> Unexpected Response %s%n",
              fullExtractURL, response.getStatusLine().toString());
          return null;
        }
        HttpEntity entity = response.getEntity();

        if (entity != null) {
          if (parent.isVerbose()) {
            System.err.println("Retrieving and uncompressing data from : " + fullExtractURL);
          }
          try (
              BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(entity.getContent());
              OutputStream out = Files.newOutputStream(outputExtract)) {
            long s = System.currentTimeMillis();
            final byte[] buffer = new byte[2048];
            int read = 0, total = 0;
            while (-1 != (read = bzIn.read(buffer))) {
              if (parent.isVerbose()) {
                total += read;
                long len = bzIn.getUncompressedCount();
                final int percentage = (int) (total * 100 / len);
                spec.commandLine().getErr().format("%d/%d / %d %%. \r", total, len, percentage);
              }
              out.write(buffer, 0, read);
            }
            System.err.println("Fetched and uncompressed to " + outputExtract + "["
                + (System.currentTimeMillis() - s) + " ms]");
          }
        }
      } catch (IOException e) {
        spec.commandLine().getErr()
            .println("IOException while retrieving extract: " + e.getLocalizedMessage());

      }

      return outputExtract;
    } catch (MalformedURLException e) {
      System.err.format("Malformed dump server URL: %s", server);
      return null;
    }
  }

}
