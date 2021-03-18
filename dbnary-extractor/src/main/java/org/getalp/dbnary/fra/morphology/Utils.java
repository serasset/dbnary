package org.getalp.dbnary.fra.morphology;

public class Utils {
  public static String standardizePronunciation(String p) {
    p = p.trim();
    if (p.startsWith("\\"))
      p = p.substring(1);
    if (p.endsWith("\\"))
      p = p.substring(0, p.length() - 1);
    return p;
  }
}
