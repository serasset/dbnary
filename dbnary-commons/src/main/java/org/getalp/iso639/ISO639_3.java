package org.getalp.iso639;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset Support class for ISO 639-3 standard for language naming.
 *         <p>
 *         This class is designed to be used as a singleton.
 *         <p>
 *         Usage: <code>ISO639_3 isoLanguages = ISO639_3.sharedInstance;
 *                String french = isoLanguages.getLanguageNameInEnglish("fre");</code>
 */
public class ISO639_3 {
  private static final Logger logger = LoggerFactory.getLogger(ISO639_3.class);

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
    extractSilIsoTable("iso-639-3.tab");
    // Also takes patch languages (mainly some collective languages that are not part of iso-639-3
    // but were part of iso-639-2)
    extractSilIsoTable("iso-639-patch.tab");

    try (InputStream fis = this.getClass().getResourceAsStream("iso-639-3-patchDBnary.tab");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      Matcher matcher = epolinePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        matcher.reset(s);
        if (matcher.find()) {
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.addName(l.id, matcher.group(2));
          }
        } else {
          logger.error("Unrecognized line: {}", s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      logger.error("ISO639 French data not available");
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
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.epo = matcher.group(2);
            l.addName(l.id, l.epo);
          }
        } else {
          logger.error("Unrecognized line:{}", s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      logger.error("Could not read ISO639-eponym.tab resource", e);
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
          // a3b, a3t, a2, en, fr
          Lang l = langMap.get(matcher.group(1));
          if (l != null) {
            l.fr = matcher.group(2);
            l.addName(l.id, l.fr);
          }
        } else {
          logger.error("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      logger.error("ISO639 French data not available", e);
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
          logger.error("Unrecognized line:" + s);
        }
        s = br.readLine();
      }
    } catch (IOException e) {
      logger.error("ISO639 Chinese data not available", e);
    }
    extractRetirements("iso-639-3_Retirements.tab");
  }

  private void extractSilIsoTable(String fname) {
    try (InputStream fis = this.getClass().getResourceAsStream(fname);
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      Matcher matcher = linePattern.matcher("");

      String s = br.readLine();
      while (s != null) {
        if (s.startsWith("#")) {
          s = br.readLine();
          continue;
        }

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
          if (!l.id.isEmpty()) {
            langMap.put(l.id, l);
          }
          if (!l.part1.isEmpty()) {
            langMap.put(l.part1, l);
          }
          if (!l.part2b.isEmpty()) {
            langMap.put(l.part2b, l);
          }
          if (!l.part2t.isEmpty()) {
            langMap.put(l.part2t, l);
          }

        } else {
          logger.error("Unrecognized line:{}", s);
        }
        s = br.readLine();
      }

    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      e.printStackTrace();
    }
  }

  private void extractRetirements(String fname) {
    try (InputStream fis = this.getClass().getResourceAsStream(fname);
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      // Read and ignore header line
      String s = br.readLine();
      if (s.startsWith("Id\tRef"))
        s = br.readLine();
      while (s != null) {
        if (s.startsWith("#")) {
          s = br.readLine();
          continue;
        }

        // Id Ref_Name Ret_Reason Change_To Ret_Remedy Effective
        String[] parts = s.split("\t");
        String lang_id = parts[0];
        String reason = parts[2];
        String change_to = parts[3];
        switch (reason) {
          case "C":
          case "D":
          case "M":
            Lang l = langMap.get(change_to);
            if (null != l) {
              if (langMap.containsKey(lang_id)) {
                logger.error("Retired language: {} conflict with {}", lang_id, change_to);
              }
              langMap.putIfAbsent(lang_id, l);
            }
            break;
          case "N":
          case "S":
            // Keep language that have been declared as non existing or obsolete if there is no
            // conflict
            if (!langMap.containsKey(lang_id)) {
              Lang obsolete_lang = new Lang();
              obsolete_lang.id = lang_id;
              obsolete_lang.en = parts[1];
              obsolete_lang.addName("eng", obsolete_lang.en);
              if (!obsolete_lang.id.isEmpty()) {
                langSet.add(obsolete_lang);
                langMap.put(obsolete_lang.id, obsolete_lang);
              }
            }
          default:
            break;
        }
        s = br.readLine();
      }

    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
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

  public String getTerm3BCode(String langcode) {
    Lang l = langMap.get(langcode);
    return (l != null) ? (l.part2b == null) ? l.id : l.part2b : null;
  }

  public String getTerm3TCode(String langcode) {
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
      l = getTerm3TCode(langcode);
    }
    if (l == null || l.isEmpty()) {
      l = getTerm3BCode(langcode);
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
