package org.getalp.iso639;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author serasset Support class for ISO 639-3 standard for language naming.
 *         <p>
 *         This class is designed to be used as a singleton.
 *         <p>
 *         Usage: <code>ISO639_3 isoLanguages = ISO639_3.sharedInstance;
 *                String french = isoLanguages.getLanguageNameInEnglish("fre");</code>
 */
public class ISO639_3 {

  public static class Lang {

    /**
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * @return the part2b
     */
    public String getPart2b() {
      return part2b;
    }

    /**
     * @return the part2t
     */
    public String getPart2t() {
      return part2t;
    }

    /**
     * @return the part1
     */
    public String getPart1() {
      return part1;
    }

    /**
     * @return the fr
     */
    public String getFr() {
      return fr;
    }

    /**
     * @return the en
     */
    public String getEn() {
      return en;
    }

    /**
     * @return the epo
     */
    public String getEpo() {
      return epo;
    }

    private String id, part2b, part2t, part1, fr, en, epo;
    Map<String, LinkedHashSet<String>> names = new HashMap<>();

    void addName(String lang, String langName) {
      names.computeIfAbsent(lang, k -> new LinkedHashSet<>()).add(langName);

    }

    String getName(String lang) {
      LinkedHashSet<String> namesInLang = names.get(lang);
      if (null == namesInLang)
        return null;
      Iterator<String> itr = namesInLang.iterator();

      // get the first element
      if (itr.hasNext())
        return itr.next();
      else
        return null;
    }

    Set<String> getNames(String lang) {
      return names.get(lang);
    }

  }

  private final static String linePatternString =
      "^(.*?)\t(.*?)\t(.*?)\t(.*?)\t(.*?)\t(.*?)\t(.*?)(?:\t(.*))?$";
  private final static String epolinePatternString = "^(.*?)\t(.*?)$";
  private final static String chinesePatternString =
      "^(?:^(.*?)(?:\\s?(?:\\(.?\\)?)?))\t(.*?)\t(.*?)\t(.*?)\t(.*?)\t(.*)";
  private final static Pattern linePattern = Pattern.compile(linePatternString);
  private final static Pattern chinesePattern = Pattern.compile(chinesePatternString);
  private final static Pattern epolinePattern = Pattern.compile(epolinePatternString);

  public static ISO639_3 sharedInstance = new ISO639_3();
  private final Map<String, Lang> langMap = new HashMap<>();
  private final Set<Lang> langSet = new HashSet<>();

  private ISO639_3() {
    // an element of langMap has key "ita"
    // and value {id: "ita", part2b: "ita", part2t: "ita", part1: "it", en: "Italian"}
    // another element of langMap has key "it"
    // and value {id: "ita", part2b: "ita", part2t: "ita", part1: "it", en: "Italian"}
    try (InputStream fis = this.getClass().getResourceAsStream("iso-639-3.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      Matcher matcher = linePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          Lang l = new Lang();
          // Id___Part2B_Part2T_Part1 Scope Language_Type Ref_Name Comment
          // ita__ita____ita____it____I_____L_____________Italian
          l.id = matcher.group(1);
          l.part2b = matcher.group(2);
          l.part2t = matcher.group(3);
          l.part1 = matcher.group(4);
          l.en = matcher.group(7);
          l.addName("eng", l.en);

          langSet.add(l);
          langMap.put(l.id, l);
          if (l.part1.length() != 0) {
            langMap.put(l.part1, l);
          }
          if (l.part2b.length() != 0) {
            langMap.put(l.part2b, l);
          }
          if (l.part2t.length() != 0) {
            langMap.put(l.part2t, l);
          }

        } else {
          System.err.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }


    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      e.printStackTrace();
    }
    try (InputStream fis = this.getClass().getResourceAsStream("iso-639-3-patchDBnary.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      Matcher matcher = epolinePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          // System.err.println(matcher.group(5));
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.addName(l.id, matcher.group(2));
          }
        } else {
          System.err.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      System.err.println("ISO639 French data not available");
      e.printStackTrace();
    }
    // Get eponym language names
    // TODO: do it lazily.
    try (InputStream fis = this.getClass().getResourceAsStream("ISO639-eponym.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      Matcher matcher = epolinePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          // System.err.println(matcher.group(5));
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.epo = matcher.group(2);
            l.addName(l.id, l.epo);
          }
        } else {
          System.err.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Get French names
    // TODO: do this lazily
    try (InputStream fis = this.getClass().getResourceAsStream("ISO639-fr.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      Matcher matcher = epolinePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          // System.err.println(matcher.group(5));
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.fr = matcher.group(2);
            l.addName(l.id, l.fr);
          }
        } else {
          System.err.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      System.err.println("ISO639 French data not available");
      e.printStackTrace();
    }
    // Get Chinese names
    try (InputStream fis = this.getClass().getResourceAsStream("ISO639-zh.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      Matcher matcher = chinesePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            String zhTraditionalGroup = matcher.group(5);
            String zhSimpleGroup = matcher.group(6);
            for (String name : zhTraditionalGroup.split("、")) {
              if (name.trim().length() == 0)
                continue;
              langMap.put(name, l);
            }
            for (String name : zhSimpleGroup.split("、")) {
              if (name.trim().length() == 0)
                continue;
              langMap.put(name, l);
            }
          }
        } else {
          System.err.println("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      System.err.println("ISO639 Chinese data not available");
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

  public String getEponymLanguageName(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.epo : null;
  }

  public String getBib3Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.part2b : null;
  }

  public String getTerm3Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? (l.part2t == null) ? l.id : l.part2t : null;
  }

  public String getIdCode(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.id : null;
  }

  public String getTerm2Code(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? l.part1 : null;
  }

  public String getShortestCode(String langcode) {
    String l = getTerm2Code(langcode);
    if (l == null || l.isEmpty()) {
      l = getTerm3Code(langcode);
    }
    if (l == null || l.isEmpty()) {
      l = getIdCode(langcode);
    }

    return l;
  }

  public Lang getLang(String langcode) {
    return langMap.get(langcode);
  }

  private Map<String, Lang> name2Lang;

  public Lang getLangFromName(String langName) {
    return getLangFromName("eng", langName);
  }

  public Lang getLangFromName(String lang, String langName) {
    if (null == name2Lang) {
      name2Lang = new HashMap<>(langSet.size());
      for (Lang l : langSet) {
        name2Lang.put(l.en, l);
      }
    }
    return name2Lang.get(langName);
  }

  public Iterator<Lang> knownLanguagesIterator() {
    return langSet.iterator();
  }

  public Set<Lang> knownLanguages() {
    return langSet;
  }

}
