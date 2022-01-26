package org.getalp.dbnary.cli.utils;

import java.nio.file.Path;
import java.util.Objects;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;

public class ExtractionPreferences {

  private final Path directory;
  private final Path dumpDir;
  private final Path extractionDir;

  public ExtractionPreferences(Path directory) {
    this.directory = directory;
    dumpDir = directory.resolve("dumps");
    extractionDir = directory.resolve("extracts").resolve("ontolex");
  }

  public static String outputFilename(ExtractionFeature feature, String language,
      String outputFormat, boolean compress, boolean isExolex) {
    return outputFilename(feature, language, null, outputFormat, compress, isExolex);
  }

  public static String outputFilename(ExtractionFeature feature, String language, String suffix,
      String outputFormat, boolean compress, boolean isExolex) {
    StringBuilder fnb = new StringBuilder().append(shortLanguage(language)).append("_dbnary_");
    if (isExolex) {
      fnb.append("exolex_");
    }
    fnb.append(feature.toString());
    if (null != suffix) {
      fnb.append("_");
      fnb.append(suffix);
    }
    if (feature.equals(ExtractionFeature.HDT)) {
      fnb.append(".hdt");
    } else {
      fnb.append(".");
      fnb.append(RDFFormats.getExtension(outputFormat));
    }
    if (compress) {
      fnb.append(".bz2");
    }
    return fnb.toString();
  }

  public Path getDirectory() {
    return directory;
  }

  public Path getDumpDir() {
    return dumpDir;
  }

  public Path getDumpDir(String language) {
    return getDumpDir().resolve(shortLanguage(language));
  }

  public Path getExtractionDir() {
    return extractionDir;
  }

  public Path getExtractionDir(String language) {
    return getExtractionDir().resolve(shortLanguage(language));
  }

  public Path getLatestExtractionDir() {
    return getExtractionDir().resolve(shortLanguage("latest"));
  }

  public Path outputFileForFeature(ExtractionFeature feature, String language, String outputFormat,
      boolean compress, boolean isExolex) {
    return outputFileForFeature(feature, language, null, outputFormat, compress, isExolex);
  }

  public Path outputFileForFeature(ExtractionFeature feature, String language, String suffix,
      String outputFormat, boolean compress, boolean isExolex) {
    String fn = outputFilename(feature, language, suffix, outputFormat, compress, isExolex);
    return getExtractionDir(language).resolve(fn);
  }

  public static String originalDumpFilename(String language, String date) {
    return language + "wiktionary-" + date + "-pages-articles.xml.bz2";
  }

  public Path originalDump(String language, String date) {
    return this.getDumpDir(language).resolve(date).
        resolve(originalDumpFilename(language, date));
  }

  public static String expandedDumpFilename(String language, String date) {
    return language + "wkt-" + date + ".xml";
  }

  public Path expandedDump(String language, String date) {
    return this.getDumpDir(language).resolve(date)
        .resolve(expandedDumpFilename(language, date));
  }

  private static String shortLanguage(String language) {
    return Objects.requireNonNull(LangTools.getPart1OrId(language));
  }

}
