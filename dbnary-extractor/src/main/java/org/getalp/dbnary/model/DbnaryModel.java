package org.getalp.dbnary.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class DbnaryModel {


  public static String DBNARY_NS_PREFIX = "http://kaiko.getalp.org/dbnary";

  public static final String LEXVO = "http://lexvo.org/id/iso639-3/";

  public static Model tBox;

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
    StringBuffer res = new StringBuffer();
    uriEncode(s, res);
    return res.toString();
  }

  protected static void uriEncode(String s, StringBuffer res) {
    int i = 0;
    s = Normalizer.normalize(s, Normalizer.Form.NFKC);
    while (i != s.length()) {
      char c = s.charAt(i);
      if (Character.isSpaceChar(c)) {
        res.append('_');
      } else if ((c >= '\u00A0' && c <= '\u00BF') || (c == '<') || (c == '>') || (c == '%')
          || (c == '"') || (c == '#') || (c == '[') || (c == ']') || (c == '\\') || (c == '^')
          || (c == '`') || (c == '{') || (c == '|') || (c == '}') || (c == '\u00D7')
          || (c == '\u00F7')) {
        try {
          res.append(URLEncoder.encode("" + c, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          // Should never happen
          e.printStackTrace();
        }
      } else if (Character.isISOControl(c)) {
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
  }

  protected static String uriEncode(String s, String pos) {
    StringBuffer res = new StringBuffer();
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
        ; // nop
      } else if (c == '\u200e' || c == '\u200f') {
        ; // ignore rRLM and LRM.
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
