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
      log.error("Error reading Wiktionary Language Codes during german extractor initialization", e);
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
    String oldCode = languageNamesToCode.put(languageName, languageCode);
    if (null != oldCode) {
      log.debug("LANGUAGE NAME CONFLICT: {} --> {} has been redefined to {}", languageName, oldCode,
          languageCode);
    }
  }

  public static String getCode(String languageName) {
    return languageNamesToCode.get(languageName);
  }
}
