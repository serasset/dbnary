/**
 *
 */
package org.getalp.dbnary.eng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import org.getalp.LangTools;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mariam, pantaleo
 */
public class EnglishLangToCode extends LangTools {

  static Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  static HashMap<String, String> h = new HashMap<String, String>();

  static {
    // THIS LIST HAS BEEN REDUCED TO THOSE LANGUAGES THAT ARE NOT ALREADY INCLUDED IN FILE
    // LIST_OF_LANGUAGES.CSV
    // compared to data in list_of_languages.csv, the list below was missing some data
    // E.g.:
    ////// in file list_of_languages.csv we have that code "rw"
    ////// corresponds to languages: Rwanda-Rundi, Rwanda, Kinyarwanda, Rundi, Kirundi, Ha, Giha,
    // Hangaza, Vinza, Shubi, Subi
    ////// while in the list below we only had: add("Kinyarwanda", "rw");
    // E.g.:
    ////// in the list below Bosnian corresponded to code bs and Croatian to code hr,
    ////// while currently in file list_of_languages.csv both correspond to code sh
    // E.g.:
    ////// add("Eastern Frisian", "frs"); //this is stq in file list_of_languages.csv
    // E.g.:
    // add("Bikol","bik"); //this is probably incorrect. it is not present in file
    // list_of_languages.cs but bikol as bik is a MacroLanguage
    // E.g.:
    ////// add("Kirundi", "rn");//Kirundi is now merged with Rwanda-Rundi and corresponds to code rw
    // E.g.:
    ////// add("Komi-Zyrian", "kv");//code for Komi-Zyrian is now kpv
    add("Lenape", "del"); // lenape is a language family

    // add("Old Persian", "peo");//not sure what this is
    // add("Serbian", "sr");//this is now sh
    // add("Template:fil", "fil");//not sure what this is
    // add("Template:mis", "mis");//not sure what this is
    // add("Template:mo", "mo");//not sure what this is
    // add("Template:tlh", "tlh");//not sure what this is
    // add("Template:zbl", "zbl");//not sure what this is
    // add("Template:zxx", "zxx");//not sure what this is
    // add("Twi", "tw");//this is Akan language in file ...

    // we replaced the list of special languages with file etymology-only_languages.csv
    // most entries that were present below are present in file etymology-only_languages.csv
    // many entries that were present below are present in file etymology-only_languages.csv but
    // have changed over time
    // for example codes el-aeo, el-arp, el-ela, el-epc, el-hmr, el-ion, have changed to
    // grc-aeo, grc-arp, grc-att, grc-ela, grc-epc, grc-hmr, grc-ion
    // add("Old Gujarati", "gu-old");//this is not present in file etymology-only_languages.csv
    // anymore
    // add("pre-Roman (Balkans)", "und-bal"); "
    // add("pre-Roman (Iberia)", "und-ibe"); "

    // add also elements with key:
    // "Italian"
    // and value:
    // "ita"
    // as extracted from file ISO639_3.tab
    for (ISO639_3.Lang l : ISO639_3.sharedInstance.knownLanguages()) {
      add(l.getEn(), l.getId());
    }

    // add lines from file list_of_languages.csv in resources folder
    // Only add data for languages that are not present in the ISO file
    InputStream fis = null;
    try {
      fis = EnglishLangToCode.class.getResourceAsStream("list_of_languages.csv");
      BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

      String s = br.readLine();
      // skip header
      s = br.readLine();
      while (s != null) {
        String[] line = s.split(";");
        String code = line[1];
        String canonical_name = line[2];
        String[] other_names = {};
        if (line.length > 11) {
          other_names = line[11].split(",");
        }

        // Only add data for languages that are not present in the ISO file
        String language_names[] = new String[other_names.length + 1];
        language_names[0] = canonical_name;
        for (int i = 0; i < other_names.length; i++) {
          language_names[i + 1] = other_names[i];
        }

        boolean toAdd = true;
        for (int i = 0; i < language_names.length; i++) {
          if (h.containsKey(language_names[i])) {
            toAdd = false;
            add(code, h.get(language_names[i]));
            break;
          }
        }
        if (toAdd) {
          for (int i = 0; i < language_names.length; i++) {
            add(language_names[i], code);
          }
          add(code, code);
        }
        s = br.readLine();
      }
    } catch (UnsupportedEncodingException e) {
      // This should really never happen
    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          // nop
        }
      }
    }

    // add lines from file etymology-only_languages.csv in resources folder
    // Only add data for languages that are not present in the ISO file
    fis = null;
    try {
      fis = EnglishLangToCode.class.getResourceAsStream("etymology-only_languages.csv");
      BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

      String s = br.readLine();
      // skip header
      s = br.readLine();
      while (s != null) {
        String[] line = s.split(";");
        String codes[] = line[1].split(",");
        String canonical_name = line[2];
        String[] other_names = {};
        if (other_names.length > 4) {
          other_names = line[4].split(",");
        }

        // choose a representative code
        String code = null;
        for (int i = 0; i < codes.length; i++) {
          if (!codes[i].contains(" ") && !codes[i].contains(".")) {
            code = codes[i];
            break;
          }
        }
        if (null == code) {
          log.debug("Ignoring line {} in file etymology-only_languages.csv: invlid language code",
              s);
        } else {
          // Only add data for languages that are not present in the ISO file
          String language_names[] = new String[other_names.length + 1];
          language_names[0] = canonical_name;
          for (int i = 0; i < other_names.length; i++) {
            language_names[i + 1] = other_names[i];
          }

          boolean toAdd = true;
          for (int i = 0; i < language_names.length; i++) {
            if (h.containsKey(language_names[i])) {
              toAdd = false;
              for (int j = 0; j < codes.length; j++) {
                add(codes[j], h.get(language_names[i]));
              }
              break;
            }
          }
          if (toAdd) {
            for (int i = 0; i < language_names.length; i++) {
              add(language_names[i], code);
            }
            for (int i = 0; i < codes.length; i++) {
              add(codes[i], code);
            }
          }
        }
        s = br.readLine();
      }
    } catch (UnsupportedEncodingException e) {
      // This should really never happen
    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          // nop
        }
      }
    }
  }

  public static String threeLettersCode(String s) {
    return threeLettersCode(h, s);
  }

  private static void add(String n, String c) {
    h.put(n.toLowerCase(), c);
  }
}
