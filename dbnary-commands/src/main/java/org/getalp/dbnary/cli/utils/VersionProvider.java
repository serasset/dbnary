package org.getalp.dbnary.cli.utils;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    String extractorVersion = "UNKNOWN";
    Manifest mf = new Manifest();
    try {
      mf.read(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("META-INF/MANIFEST.MF"));

      Attributes atts = mf.getMainAttributes();
      extractorVersion = atts.getValue("Implementation-Version");
    } catch (IOException e) {
      System.err.println("Could not retrieve extractor version.");
    }
    return new String[] {extractorVersion};
  }
}
