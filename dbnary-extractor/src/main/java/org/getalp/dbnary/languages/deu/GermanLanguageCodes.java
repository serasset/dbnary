package org.getalp.dbnary.languages.deu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanLanguageCodes {
  private static final Logger log = LoggerFactory.getLogger(GermanLanguageCodes.class);

  private static final Map<String, String> languageNamesToCode = new HashMap<>(10000);

  static {
    readCodesFromWikipediaTables();

    // Patches as some language denomination are particular in German wiktionary
    registerLanguageName("Weißrussisch", "bel");
    registerLanguageName("Prußisch", "prg");
    registerLanguageName("Preußisch", "prg");
    registerLanguageName("Maori", "mri");
    registerLanguageName("Neugriechisch", "ell");
    registerLanguageName("Mittelgriechisch", "grc");
    registerLanguageName("Paschtu", "pus");
    registerLanguageName("Paschtunisch", "pus");
    registerLanguageName("Venezianisch", "vec");
    registerLanguageName("Venedisch", "vec");
    registerLanguageName("Venetisch", "vec");
    registerLanguageName("Venezisch", "vec");
    registerLanguageName("Friaulisch", "fur");
    registerLanguageName("Furlanisch", "fur");
    registerLanguageName("Friulanisch", "fur");
    registerLanguageName("West-Pandschabi", "pnb");
    registerLanguageName("Westpanjabi", "pnb");
    registerLanguageName("West-Pandschabisch", "pnb");
    registerLanguageName("Nepalesisch", "nep");
    registerLanguageName("Suaheli", "swa");
    registerLanguageName("Belutschi", "bal");
    registerLanguageName("Belutschisch", "bal");
    registerLanguageName("Huastekisches Zentral-Nahuatl", "nch");
    registerLanguageName("Zentral-Nahuatl", "nhn");
    registerLanguageName("Huastekisches West-Nahuatl", "nhw");
    registerLanguageName("Huastekisches Ost-Nahuatl", "nhe");
    registerLanguageName("Tetelcingo-Nahuatl", "nhg");
    registerLanguageName("Temascaltepec-Nahuatl", "nhv");
    registerLanguageName("Klassisches Nahuatl", "nah");
    registerLanguageName("Nahuatl", "nah");
    registerLanguageName("Orizaba-Nahuatl", "nlv");
    registerLanguageName("Guerrero-Nahuatl", "ngu");
    registerLanguageName("Durango-Nahuatl", "nln");
    registerLanguageName("Zentrales Puebla-Nahuatl", "ncx");
    registerLanguageName("Tibetisch", "bod");
    registerLanguageName("Dunganisch", "dng");
    registerLanguageName("Asturisch", "ast");
    registerLanguageName("asturianische", "ast");
    registerLanguageName("Somalisch", "som");
    registerLanguageName("Mokscha", "mdf");
    registerLanguageName("Fidschi", "fij");
    registerLanguageName("Võro", "vro");
    registerLanguageName("Pennsylvaniadeutsch", "pdc");
    registerLanguageName("Pensilfaanisch", "pdc");
    registerLanguageName("Madagassisch", "mlg");
    registerLanguageName("Malagassi", "mlg");
    registerLanguageName("Luwisch", "xlu");
    registerLanguageName("Acehnesisch", "ace");
    registerLanguageName("Seselwa", "crs");
    registerLanguageName("Ladino", "lad");
    registerLanguageName("Sephardisch", "lad");
    registerLanguageName("Djudezmo", "lad");
    registerLanguageName("Kumükisch", "kum");
    registerLanguageName("Kapingamarangi", "kpg");
    registerLanguageName("Pandschabi", "pan");
    registerLanguageName("Pandschabisch", "pan");
    registerLanguageName("Mezquital-Otomi", "ote");
    registerLanguageName("Argentinisches Quechua", "qus");
    registerLanguageName("Morisien", "mfe");
    registerLanguageName("Isthmus-Zapotekisch", "zai");
    registerLanguageName("Yosondúa-Mixtekisch", "mpm");
    registerLanguageName("Tzotzil", "tzo");
    registerLanguageName("Pukapuka", "pkp");
    registerLanguageName("Pikardisch", "pcd");
    registerLanguageName("Kumükisch", "kum");
    registerLanguageName("Komorisch", "swb");
    registerLanguageName("Jamaika-Kreolisch", "jam");
    registerLanguageName("Jamaika-Kreol", "jam");
    registerLanguageName("Gurage", "sgw");
    registerLanguageName("Sibirisch-Yupik", "ess");
    registerLanguageName("West-Abenaki", "abe");
    registerLanguageName("Micmac", "mic");

    // Oshivambo is underspecified, either it is ng (Ndonga), kj (Kwanjama)
    registerLanguageName("Oshivambo", "");

    // German Wiktionary uses Altaisch for Southern Altai rather than Northern Altai
    registerLanguageName("Altaisch", "alt");
    // German Wiktionary uses Sami and mainly refers to Northern Sami (in 2025)
    registerLanguageName("Sami", "sme");



    // Since 2023, North/South Levantine Arabic has been merge under apc code
    registerLanguageName("Levantinisches Arabisch", "apc");

    // Treat translingual/International entries as "Multiple Language"
    registerLanguageName("International", "mul");
    // Just recognize, but ignore the following "languages"
    registerLanguageName("Umschrift", "");
    // No usable language code for this older/historic languages
    registerLanguageName("Frühneuhochdeutsch", "");
    registerLanguageName("Alttschechisch", "");

  }

  private static void readCodesFromWikipediaTables() {
    try (InputStream fis = GermanLanguageCodes.class.getResourceAsStream("iso639-3-deu.csv");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

      String s = br.readLine();
      while (s != null) {
        if (s.isBlank() || s.startsWith("#")) {
          s = br.readLine();
          continue;
        }

        processLanguageLine(s);
        s = br.readLine();
      }

    } catch (IOException e) {
      // don't know what I should do here, as the data should be bundled with the code.
      log.error("Error reading Wiktionary Language Codes during german extractor initialization",
          e);
    }
  }

  private static void processLanguageLine(String s) {
    String[] cols = s.split("\t"); // \t is a char and split uses it as such, not as a regex
    String languageName = cols[3];
    String languageCode = cols[0];
    if (languageCode.startsWith("("))
      return;

    if (languageName.contains("[[")) {
      StringBuilder buf = new StringBuilder();
      // The language name is a link, parse and extract the target
      WikiText lname = new WikiText(languageName);
      for (Token t : lname.tokens()) {
        if (t instanceof Text) {
          buf.append(t.getText());
        } else if (t instanceof Link) {
          // Treat links as autonomous language names
          registerLanguageName(t.asLink().getLinkText(), languageCode);
        } else {
          log.debug("UNEXPECTED TOKEN {}", t.getText());
        }
      }
      languageName = buf.toString();
    }
    languageName = languageName.replaceAll(",(\\s+,)\\s", ", ").replaceAll("^[\\s,]+", "")
        .replaceAll("[\\s,]+$", "");
    if (languageName.contains(",")) {
      log.debug("{} --> {} non trivial language name !", languageName, languageCode);
    }

    if (languageName.isBlank() || languageName.startsWith("("))
      return;

    registerLanguageName(languageName, languageCode);
  }

  private static void registerLanguageName(String languageName, String languageCode) {
    languageName = languageName.toLowerCase().trim();
    String oldCode = languageNamesToCode.put(languageName, languageCode);
    if (null != oldCode) {
      log.debug("LANGUAGE NAME CONFLICT: {} --> {} has been redefined to {}", languageName, oldCode,
          languageCode);
    }
  }

  public static String getCode(String languageName) {
    if (null == languageName || languageName.isEmpty())
      return null;
    languageName = languageName.toLowerCase().replaceAll("[\\p{Cf}\\p{Cc}\\s]+$", "")
        .replaceAll("^[\\p{Cf}\\p{Cc}\\s]+", "");
    return languageNamesToCode.get(languageName);
  }
}
