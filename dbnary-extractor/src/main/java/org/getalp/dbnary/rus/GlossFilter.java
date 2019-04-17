package org.getalp.dbnary.rus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.AbstractGlossFilter;
import org.getalp.dbnary.StructuredGloss;

public class GlossFilter extends AbstractGlossFilter {

  private static String sensenum = "(?:\\d+)";

  private static String senseNumOnlyRegExp = "^\\s*(" + sensenum + ")\\s*$";
  private static Pattern senseNumOnlyPattern = Pattern.compile(senseNumOnlyRegExp);
  private static Matcher senseNumOnlyMatcher = senseNumOnlyPattern.matcher("");

  public StructuredGloss extractGlossStructure(String rawGloss) {
    if (null == rawGloss) {
      return null;
    }

    rawGloss = normalize(rawGloss);
    if (rawGloss.length() == 0) {
      return null;
    }

    senseNumOnlyMatcher.reset(rawGloss);
    if (senseNumOnlyMatcher.matches()) {
      String n = senseNumOnlyMatcher.group(1);
      return new StructuredGloss(n, null);
    }

    return new StructuredGloss(null, rawGloss);
  }
}
