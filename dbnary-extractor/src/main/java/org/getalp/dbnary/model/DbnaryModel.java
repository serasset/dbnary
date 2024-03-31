package org.getalp.dbnary.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbnaryModel {


  public static String DBNARY_NS_PREFIX = "http://kaiko.getalp.org/dbnary";

  public static final String LEXVO = "http://lexvo.org/id/iso639-3/";

  public static Model tBox;
  static Logger logger = LoggerFactory.getLogger(DbnaryModel.class);

  static {
    // Create T-Box and read rdf schema associated to it.
    tBox = ModelFactory.createDefaultModel();
  }

  /**
   * Set the DBNARY prefix globally. Warning, setting this too late in the processus may lead to
   * inconsistent dataset.
   * 
   * @param p the global prefix to be used for the extracted data.
   */
  public static void setGlobalDbnaryPrefix(String p) {
    DBNARY_NS_PREFIX = p;
  }

  public static String uriEncode(String s) {
    StringBuilder res = new StringBuilder();
    uriEncode(s, res);
    return res.toString();
  }

  protected static void uriEncode(String s, StringBuilder res) {
    int i = 0;
    s = Normalizer.normalize(s, Normalizer.Form.NFKC);
    while (i != s.length()) {
      char c = s.charAt(i);
      if (Character.isSpaceChar(c)) {
        res.append('_');
      } else if ((c >= '\u00A0' && c <= '\u00BF') || (c == '<') || (c == '>') || (c == '%')
          || (c == '"') || (c == '#') || (c == '[') || (c == ']') || (c == '\\') || (c == '^')
          || (c == '`') || (c == '{') || (c == '|') || (c == '}') || (c == '\u00D7')
          || (c == '\u00F7') || (c == ':')) {
        res.append(URLEncoder.encode("" + c, StandardCharsets.UTF_8));
      } else if (Character.isISOControl(c)) {
        // nop
      } else if (Character.isHighSurrogate(c) && i + 1 < s.length()
          && Character.isLowSurrogate(s.charAt(i + 1))) {
        // Encode the surrogate pair as a UTF_8 char, then percent encode the resulting octets
        res.append(URLEncoder.encode(s.substring(i, i + 2), StandardCharsets.UTF_8));
        i++;
      } else if (c == '\u200e' || c == '\u200f') {
        // ignore rRLM and LRM.
      } else if (c == '/') {
        res.append("!slash!"); // ignore rRLM and LRM.
      } else {
        res.append(c);
      }
      i++;
    }
  }

  protected static String uriEncode(String s, String pos) {
    StringBuilder res = new StringBuilder();
    uriEncode(s, res);
    res.append("__");
    pos = Normalizer.normalize(pos, Normalizer.Form.NFKC);
    int i = 0;
    while (i != pos.length()) {
      char c = pos.charAt(i);
      if (Character.isSpaceChar(c)) {
        res.append('_');
      } else if ((c >= '\u00A0' && c <= '\u00BF') || (c == '<') || (c == '>') || (c == '%')
          || (c == '"') || (c == '#') || (c == '[') || (c == ']') || (c == '\\') || (c == '^')
          || (c == '`') || (c == '{') || (c == '|') || (c == '}') || (c == '\u00D7')
          || (c == '\u00F7') || (c == '-') || (c == '_') || Character.isISOControl(c)) {
        // nop
      } else if (c == '\u200e' || c == '\u200f') {
        // ignore rRLM and LRM.
      } else if (c == '/') {
        res.append("!slash!"); // ignore rRLM and LRM.
      } else {
        res.append(c);
      }
      i++;
    }
    return res.toString();
  }

}
