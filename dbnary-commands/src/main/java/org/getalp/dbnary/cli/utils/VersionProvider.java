package org.getalp.dbnary.cli.utils;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    return new String[] {getFromManifest("Implementation-Version")};
  }

  public String getExtractorVersion() {
    return getFromManifest("Extractor-Version");
  }

  private String getFromManifest(String field) {
    String extractorVersion = "UNKNOWN";
    Manifest mf = new Manifest();
    try {
      mf.read(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("META-INF/MANIFEST.MF"));

      Attributes atts = mf.getMainAttributes();
      extractorVersion = atts.getValue(field);
    } catch (IOException e) {
      System.err.println("Could not retrieve extractor version.");
    }
    return extractorVersion;
  }

  private static final Pattern DUMP_VERSION_PATTERN =
      Pattern.compile("(20\\d\\d\\d{4}|20\\d\\d_\\d{2}_\\d{2})");

  public static String getDumpVersion(String dumpFileName) {
    Matcher m = DUMP_VERSION_PATTERN.matcher(dumpFileName);
    if (m.find()) {
      return m.group();
    } else {
      return dumpFileName;
    }
  }
}
