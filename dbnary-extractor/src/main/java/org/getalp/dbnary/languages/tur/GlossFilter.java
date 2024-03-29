package org.getalp.dbnary.languages.tur;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.StructuredGloss;

public class GlossFilter extends AbstractGlossFilter {

  public static final String senseNumRegExp = "\\d+(?:[abcdefghijklmn][iv]*)?";

  public static final String simpleNumListFilter =
      "^\\s*\\[(" + senseNumRegExp + "(?:\\s*[\\,\\-–]\\s*" + senseNumRegExp + ")*)\\](.*)$";
  public static final Pattern simpleNumListPattern = Pattern.compile(simpleNumListFilter);
  public static final Matcher simpleNumListMatcher = simpleNumListPattern.matcher("");

  public static final String glossNumFilter = "^(.*)[\\[\\|](" + senseNumRegExp
      + "(?:\\s*[\\,\\-–]\\s*" + senseNumRegExp + ")*)[\\]\\|]\\s*$";
  public static final Pattern glossNumPattern = Pattern.compile(glossNumFilter);
  public static final Matcher glossNumMatcher = glossNumPattern.matcher("");

  public StructuredGloss extractGlossStructure(String rawGloss) {
    if (null == rawGloss) {
      return null;
    }

    simpleNumListMatcher.reset(rawGloss);
    if (simpleNumListMatcher.matches()) {
      String g = simpleNumListMatcher.group(2);
      g = g.trim();
      if (g.length() == 0) {
        g = null;
      }
      return new StructuredGloss(simpleNumListMatcher.group(1), g);
    }

    glossNumMatcher.reset(rawGloss);
    if (glossNumMatcher.matches()) {
      String g = glossNumMatcher.group(1);
      g = g.trim();
      if (g.length() == 0) {
        g = null;
      }
      return new StructuredGloss(glossNumMatcher.group(2), g);
    }

    return new StructuredGloss(null, rawGloss);
  }
}
