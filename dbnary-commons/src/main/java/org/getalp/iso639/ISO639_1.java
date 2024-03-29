package org.getalp.iso639;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// DONE: use the iso-639-3 file that contains a language id + other fields.
public final class ISO639_1 {

  public static class Lang {

    String a3b, a3t, a2, fr, en, epo;
  }

  private final static String linePatternString = "^(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)$";
  private final static String epolinePatternString = "^(.*?)\t(.*?)$";
  private final static Pattern linePattern = Pattern.compile(linePatternString);
  private final static Pattern epolinePattern = Pattern.compile(epolinePatternString);

  public static ISO639_1 sharedInstance = new ISO639_1();
  private final Map<String, Lang> langMap = new HashMap<>();

  private ISO639_1() {
    try (InputStream fis = this.getClass().getResourceAsStream("ISO639.data");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      Matcher matcher = linePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          // System.out.println(matcher.group(5));
          // a3b, a3t, a2, en, fr
          Lang l = new Lang();
          l.a3b = matcher.group(1);
          l.a3t = matcher.group(2);
          l.a2 = matcher.group(3);
          l.en = matcher.group(4);
          l.fr = matcher.group(5);

          langMap.put(l.a3b, l);
          langMap.put(l.a3t, l);
          langMap.put(l.a2, l);

        } else {
          System.out.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }

    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      e.printStackTrace();
    }

    try (InputStream fis = this.getClass().getResourceAsStream("ISO639-eponym.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      Matcher matcher = epolinePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.epo = matcher.group(2);
          }

        } else {
          System.out.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getLanguageNameInFrench(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.fr : null;
  }

  public String getLanguageNameInEnglish(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.en : null;
  }

  public String getBib3Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.a3b : null;
  }

  public String getTerm3Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? (l.a3t == null) ? l.a3b : l.a3t : null;
  }

  public String getTerm2Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.a2 : null;
  }

  public Lang getLang(String langcode) {
    return langMap.get(langcode);
  }

}
