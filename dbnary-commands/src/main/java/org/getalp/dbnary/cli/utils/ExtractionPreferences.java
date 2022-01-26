package org.getalp.dbnary.cli.utils;

import java.io.File;
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
      String outputFormat, boolean compress) {
    return outputFilename(feature, language, null, outputFormat, compress);
  }

  public static String outputFilename(ExtractionFeature feature, String language, String suffix,
      String outputFormat, boolean compress) {
    return outputFilename(feature, language, suffix, outputFormat, compress, false);
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

  public Path getExtractionDir() {
    return extractionDir;
  }

  public File outputFileForFeature(ExtractionFeature feature, String language, String outputFormat,
      boolean compress, boolean isExolex) {
    return outputFileForFeature(feature, language, null, outputFormat, compress, isExolex);
  }

  public File outputFileForFeature(ExtractionFeature feature, String language, String suffix,
      String outputFormat, boolean compress, boolean isExolex) {
    String fn = outputFilename(feature, language, suffix, outputFormat, compress, isExolex);
    return getExtractionDir().resolve(shortLanguage(language)).resolve(fn).toFile();
  }

  private static String shortLanguage(String language) {
    return Objects.requireNonNull(LangTools.getPart1OrId(language));
  }

}
