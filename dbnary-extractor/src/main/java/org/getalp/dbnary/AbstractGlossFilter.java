package org.getalp.dbnary;

public abstract class AbstractGlossFilter {

  public abstract StructuredGloss extractGlossStructure(String rawGloss);

  public static String normalize(String rawGloss) {
    String res = rawGloss.trim();
    res = res.replaceAll("\\s{2,}", " ");
    res = res.replace("'''", "");
    res = res.replace("''", "");

    return res;
  }


}
