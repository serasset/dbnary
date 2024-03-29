package org.getalp.dbnary.languages.fin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.StructuredGloss;

public class GlossFilter extends AbstractGlossFilter {

  private static String sensenum = "(?:\\d+\\.?|\\s|,|/)+";
  private static String simpleSenseNumberingRegExp = "^((?:" + sensenum + "\\|?)+)(.*)$";
  private static Pattern simpleSenseNumberingPattern = Pattern.compile(simpleSenseNumberingRegExp);
  private static Matcher simpleSenseNumberingMatcher = simpleSenseNumberingPattern.matcher("");

  public StructuredGloss extractGlossStructure(String rawGloss) {
    if (null == rawGloss) {
      return null;
    }

    rawGloss = normalize(rawGloss);
    if (rawGloss.length() == 0) {
      return null;
    }
    simpleSenseNumberingMatcher.reset(rawGloss);
    if (simpleSenseNumberingMatcher.matches()) {
      String g = simpleSenseNumberingMatcher.group(2);
      if (g.trim().length() == 0) {
        g = null;
      }
      String n = simpleSenseNumberingMatcher.group(1);
      n = n.trim().replace('|', ',');
      if (n.endsWith(",")) {
        n = n.substring(0, n.length() - 1);
      }
      return new StructuredGloss(n, g);
    }

    return new StructuredGloss(null, rawGloss);
  }
}
