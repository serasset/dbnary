package org.getalp.dbnary.cli.utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.cli.DBnary;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    return new String[] {getVersionFromPackage()};
  }

  public String getExtractorVersion() {
    return getVersionFromPackage();
  }

  private String getVersionFromPackage() {
    Package pkg = DBnary.class.getPackage();
    Annotation[] annots = pkg.getAnnotations();
    for (Annotation a : annots) {
      System.err.println(a);
    }
    return DBnary.class.getPackage().getImplementationVersion();
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
