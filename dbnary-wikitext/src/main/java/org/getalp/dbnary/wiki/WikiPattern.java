package org.getalp.dbnary.wiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serasset on 05/03/17.
 */
public class WikiPattern {

  /**
   * Compiles the pattern using extended syntax fit to define patterns on wiki texts.
   *
   * <h3><a id="sum">Summary of regular-expression extension</a></h3>
   *
   * <table style="border: 0px; border-collapse: collapse; border-spacing: 0; padding: 5px;">
   * <caption>Regular expression constructs, and what they match</caption>
   * <tr style="text-align: left;">
   * <th style="text-align: left;" id="construct">Construct</th>
   * <th style="text-align: left;" id="matches">Matches</th>
   * </tr>
   *
   * <tr>
   * <th>&nbsp;</th>
   * </tr>
   * <tr style="text-align: left;">
   * <th colspan="2" id="classes">Character classes</th>
   * </tr>
   *
   * <tr>
   * <td valign="top">{@code \p{Template}}</td>
   * <td headers="matches">a full wiki template.</td>
   * </tr>
   * <tr>
   * <td valign="top">{@code \p{Link}}</td>
   * <td headers="matches">either an Internal or an External Link.</td>
   * </tr>
   * <tr>
   * <td valign="top">{@code \p{ExternalLink}}</td>
   * <td headers="matches">an External Link.</td>
   * </tr>
   * <tr>
   * <td valign="top">{@code \p{InternalLink}}</td>
   * <td headers="matches">an Internal Link (complete with its eventual suffix).</td>
   * </tr>
   *
   *
   * <tr>
   * <th>&nbsp;</th>
   * </tr>
   * <tr style="text-align: left;">
   * <th colspan="2" id="events">Open/Close events</th>
   * </tr>
   *
   * <tr>
   * <td valign="top"><code>(_</code><i>xxx</i><code>_</code></td>
   * <td headers="matches">the opening of an event, where <i>xxx</i> is identifies the event (xxx is
   * a sequence of characters, possibly empty). If present, xxx will represent a group name (take it
   * into account when playing with group count.</td>
   * </tr>
   * <tr>
   * <td valign="top"><code>_</code><i>xxx</i><code>_)</code></td>
   * <td headers="matches">the closing of an event which was given name <i>xxx</i> on opening. If
   * <i>xxx</i> is empty, matches any closing event.</td>
   * </tr>
   * </table>
   *
   * @param regex The extended regular expression
   * @return a Pattern matching the given extended regex.
   */
  public static Pattern compile(String regex) {
    String correctedRegex = toStandardPattern(regex);
    return Pattern.compile(correctedRegex);
  }

  private static final String reserveWikiPatternWords = //
      "(?<TMPL>\\\\(?<TMATCH>[pP])\\{Template\\})" // \p{Template} to match templates
      + "|(?<RESERVED>\\\\(?<RMATCH>[pP])\\{Reserved\\})" // \p{Reserved} to match reserved
      + "|(?<L>\\\\(?<LMATCH>[pP])\\{Link\\})" // \p{Link} to match a link
      + "|(?<IL>\\\\(?<ILMATCH>[pP])\\{InternalLink\\})" // \p{InternalLink} for internal link
      + "|(?<EL>\\\\(?<ELMATCH>[pP])\\{ExternalLink\\})" // \p{ExternalLink} for external link
      + "|(?<OPEN>\\(_(?<ONAME>\\p{Alpha}\\p{Alnum}*)?_)" // (_
      + "|(?<CLOSE>_(?<CNAME>\\p{Alpha}\\p{Alnum}*)?_\\))"
      + "|(?<WHITESPACE>\\\\(?<WPMATCH>[pP])\\{White_Space\\})";

  private static final Pattern lexerPatern = Pattern.compile(reserveWikiPatternWords);

  public static final String TEMPLATES = WikiCharSequence.TEMPLATES_RANGE.toString();
  public static final String INTERNAL_LINKS = WikiCharSequence.INTERNAL_LINKS_RANGE.toString();
  public static final String EXTERNAL_LINKS = WikiCharSequence.EXTERNAL_LINKS_RANGE.toString();
  public static final String HEADERS = WikiCharSequence.HEADERS_RANGE.toString();
  public static final String LIST_ITEMS = WikiCharSequence.LISTS_RANGE.toString();
  public static final String LINKS = INTERNAL_LINKS + EXTERNAL_LINKS;
  public static final String RESERVED = TEMPLATES + LINKS + HEADERS + LIST_ITEMS;

  private static final String whitespace_chars = "" /* dummy empty string for homogeneity */
      + "\\u0009" // CHARACTER TABULATION
      + "\\u000A" // LINE FEED (LF)
      + "\\u000B" // LINE TABULATION
      + "\\u000C" // FORM FEED (FF)
      + "\\u000D" // CARRIAGE RETURN (CR)
      + "\\u0020" // SPACE
      + "\\u0085" // NEXT LINE (NEL)
      + "\\u00A0" // NO-BREAK SPACE
      + "\\u1680" // OGHAM SPACE MARK
      + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
      + "\\u2000" // EN QUAD
      + "\\u2001" // EM QUAD
      + "\\u2002" // EN SPACE
      + "\\u2003" // EM SPACE
      + "\\u2004" // THREE-PER-EM SPACE
      + "\\u2005" // FOUR-PER-EM SPACE
      + "\\u2006" // SIX-PER-EM SPACE
      + "\\u2007" // FIGURE SPACE
      + "\\u2008" // PUNCTUATION SPACE
      + "\\u2009" // THIN SPACE
      + "\\u200A" // HAIR SPACE
      + "\\u2028" // LINE SEPARATOR
      + "\\u2029" // PARAGRAPH SEPARATOR
      + "\\u202F" // NARROW NO-BREAK SPACE
      + "\\u205F" // MEDIUM MATHEMATICAL SPACE
      + "\\u3000" // IDEOGRAPHIC SPACE
  ;

  public static String toStandardPattern(String regex) {
    StringBuffer correctedRegex = new StringBuffer(regex.length());

    Matcher lexer = lexerPatern.matcher(regex);
    while (lexer.find()) {
      String g;
      lexer.appendReplacement(correctedRegex, "");
      if (null != (g = lexer.group("TMPL"))) {
        String negation = "";
        if (lexer.group("TMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + TEMPLATES + "]"));
      } else if (null != (g = lexer.group("RESERVED"))) {
        String negation = "";
        if (lexer.group("RMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + RESERVED + "]"));
      } else if (null != (g = lexer.group("L"))) {
        String negation = "";
        if (lexer.group("LMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + LINKS + "]"));
      } else if (null != (g = lexer.group("IL"))) {
        String negation = "";
        if (lexer.group("ILMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + INTERNAL_LINKS + "]"));
      } else if (null != (g = lexer.group("EL"))) {
        String negation = "";
        if (lexer.group("ELMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + EXTERNAL_LINKS + "]"));
      } else if (null != (g = lexer.group("OPEN"))) {
        String oname = lexer.group("ONAME");
        if (oname != null && oname.length() > 0) {
          correctedRegex.append(("〔(?<" + oname + ">[" + RESERVED + "])"));
        } else {
          correctedRegex.append(("〔[" + RESERVED + "]"));
        }
      } else if (null != (g = lexer.group("CLOSE"))) {
        String cname = lexer.group("CNAME");
        if (cname != null && cname.length() > 0) {
          correctedRegex.append(("\\k<" + cname + ">〕"));
        } else {
          correctedRegex.append(("[" + RESERVED + "]〕"));
        }
      } else if (null != (g = lexer.group("WHITESPACE"))) {
        String negation = "";
        if (lexer.group("WPMATCH").equals("P")) {
          negation = "^";
        }
        correctedRegex.append(("[" + negation + whitespace_chars + "]"));
      } else {
        assert false;
      }
    }
    lexer.appendTail(correctedRegex);

    return correctedRegex.toString();
  }


}
