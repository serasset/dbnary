package org.getalp.dbnary.languages.fra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.StructuredGloss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlossFilter extends AbstractGlossFilter {

  private Logger log = LoggerFactory.getLogger(AbstractGlossFilter.class);

  private static String aTrierRegExp;

  static {
    aTrierRegExp = (new StringBuffer()).append("(?:").append("^\\s*[ÀAaà] trier").append(")|(?:")
        .append("^\\s*[Tt]raductions? à (?:trier|classer|vérifier)").append(")|(?:")
        .append("^\\s*[àaÀA] classer").append(")|(?:")
        .append("^\\s*[àaÀA] vérifier et (?:trier|classer)").append(")").toString();
  }

  private static Pattern aTrierPattern = Pattern.compile(aTrierRegExp);
  private static Matcher aTrierMatcher = aTrierPattern.matcher("");

  private static String sensenum = "(?:(?:\\d+\\.?[abcdefg])|\\d|et|\\s|,)+";
  private static String simpleSenseNumberingRegExp = "^([^\\|]*)\\|(" + sensenum + ")$";
  private static Pattern simpleSenseNumberingPattern = Pattern.compile(simpleSenseNumberingRegExp);
  private static Matcher simpleSenseNumberingMatcher = simpleSenseNumberingPattern.matcher("");

  private static String numSenseGlossRegExp = "^\\s*\\((" + sensenum + ")\\)(.*)$";
  private static Pattern numSenseGlossPattern = Pattern.compile(numSenseGlossRegExp);
  private static Matcher numSenseGlossMatcher = numSenseGlossPattern.matcher("");

  private static String glossNumSenseNumberingRegExp =
      "^(.*)\\s*\\((" + sensenum + ")\\)(?:[\\p{Punct}\\s])*$";
  private static Pattern glossNumSenseNumberingPattern =
      Pattern.compile(glossNumSenseNumberingRegExp);
  private static Matcher glossNumSenseNumberingMatcher = glossNumSenseNumberingPattern.matcher("");

  private static String senseNRegExp = "^\\s*Sens\\s+(\\d+)\\s*$";
  private static Pattern senseNPattern = Pattern.compile(senseNRegExp);
  private static Matcher senseNMatcher = senseNPattern.matcher("");

  private static String senseDashGlossRegExp =
      "^\\s*(" + sensenum + ")\\s*(?:[-:\\)\\.])\\s*(.*)\\s*$";
  private static Pattern senseDashGlossPattern = Pattern.compile(senseDashGlossRegExp);
  private static Matcher senseDashGlossMatcher = senseDashGlossPattern.matcher("");

  public StructuredGloss extractGlossStructure(String rawGloss) {
    if (null == rawGloss) {
      return null;
    }
    if (rawGloss.startsWith("|")) {
      rawGloss = rawGloss.substring(1);
    }

    aTrierMatcher.reset(rawGloss);
    if (aTrierMatcher.find()) {
      log.trace("Discarding gloss : {} ", rawGloss);
      return null; // non relevant glosses should be discarded
    }
    if (rawGloss.matches("[\\s\\u0085\\p{Z}]*")) {
      log.trace("Discarding empty gloss : {} ", rawGloss);
    }
    rawGloss = normalize(rawGloss);
    if (rawGloss.length() == 0) {
      return null;
    }
    simpleSenseNumberingMatcher.reset(rawGloss);
    if (simpleSenseNumberingMatcher.matches()) {
      return new StructuredGloss(simpleSenseNumberingMatcher.group(2),
          simpleSenseNumberingMatcher.group(1));
    }
    numSenseGlossMatcher.reset(rawGloss);
    if (numSenseGlossMatcher.matches()) {
      String g = numSenseGlossMatcher.group(2);
      if (null != g && g.trim().length() == 0) {
        g = null;
      }
      return new StructuredGloss(numSenseGlossMatcher.group(1), g);
    }
    glossNumSenseNumberingMatcher.reset(rawGloss);
    if (glossNumSenseNumberingMatcher.matches()) {
      String g = glossNumSenseNumberingMatcher.group(1);
      if (null != g && g.trim().length() == 0) {
        g = null;
      }
      return new StructuredGloss(glossNumSenseNumberingMatcher.group(2), g);
    }
    senseNMatcher.reset(rawGloss);
    if (senseNMatcher.matches()) {
      return new StructuredGloss(senseNMatcher.group(1), null);
    }
    if (rawGloss.matches("[\\d\\s\\p{Punct}]+")) {
      return new StructuredGloss(rawGloss, null);
    }
    senseDashGlossMatcher.reset(rawGloss);
    if (senseDashGlossMatcher.matches()) {
      return new StructuredGloss(senseDashGlossMatcher.group(1), senseDashGlossMatcher.group(2));
    }

    // if (rawGloss.matches(".*\\d.*")) System.err.println("Digit in gloss: " + rawGloss );
    return new StructuredGloss(null, rawGloss);
  }
}
