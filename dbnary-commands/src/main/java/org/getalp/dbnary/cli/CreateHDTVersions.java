package org.getalp.dbnary.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.getalp.dbnary.cli.utils.VersionProvider;
import org.getalp.dbnary.hdt.RDF2HDT;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "dbnary-hdt", mixinStandardHelpOptions = true, helpCommand = true,
    versionProvider = VersionProvider.class)
public class CreateHDTVersions implements Callable<Integer> {
  @Parameters(index = "0",
      description = "directory containing the latest extracts that need to be converted to HDT.")
  Path latestDir;

  @Override
  public Integer call() throws Exception {
    try (Stream<Path> stream = Files.list(latestDir)) {
      Map<String, List<Path>> filesByLanguage =
          stream.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".ttl.bz2"))
              .flatMap(p -> toRealPath(p).stream())
              .collect(Collectors.groupingBy(CreateHDTVersions::language));

      filesByLanguage.forEach((lg, paths) -> {
        System.err.format("Processing %s:%n", lg);
        List<String> hdtFiles =
            paths.stream().map(CreateHDTVersions::rdf2hdt).collect(Collectors.toList());

      });
    }
    return 0;
  }

  private static final String languageRegex = "(...?)_dbnary.*";
  private static final Pattern languagePattern = Pattern.compile(languageRegex);

  private static String language(Path path) {
    Matcher matcher = languagePattern.matcher(path.getFileName().toString());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  private static Optional<Path> toRealPath(Path path) {
    try {
      return Optional.of(path.toRealPath());
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  private static String rdf2hdt(Path rdf) {
    String rdfFilename = rdf.getFileName().toString();
    assert rdfFilename.endsWith(".ttl.bz2");
    String hdtFilename = rdfFilename.substring(0, rdfFilename.length() - 8) + ".hdt.bz2";
    Path hdt = rdf.getParent().resolve(hdtFilename);
    if (!Files.exists(hdt)) {
      RDF2HDT converter = new RDF2HDT(false);
      System.err.format("Converting %s to %s.%n", rdf, hdt);
      converter.rdf2hdt(rdf, hdt);
    } else {
      System.err.format("%s already exists; Ignoring.%n", hdt);
    }
    return hdtFilename;
  }

  public static void main(String... args) {
    int exitCode = new CommandLine(new CreateHDTVersions()).execute(args);
    System.exit(exitCode);
  }
}
