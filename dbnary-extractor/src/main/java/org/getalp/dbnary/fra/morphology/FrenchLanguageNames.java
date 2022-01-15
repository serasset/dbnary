package org.getalp.dbnary.fra.morphology;

import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchLanguageNames {

  private static Logger logger = LoggerFactory.getLogger(FrenchLanguageNames.class);

  private static HashMap<String, String> l = null;
  private static boolean languagesAvailable = false;

  public static String getName(String code, WiktionaryIndex wi) {
    if (null == l)
      languagesAvailable = loadLanguagesFromPage(wi);
    if (languagesAvailable)
      return l.get(code);
    else
      return ISO639_3.sharedInstance.getLanguageNameInFrench(code);
  }

  private static final Pattern languageDef =
      Pattern.compile("l\\['([^']*)']\\s*=\\s*\\{\\s*nom\\s=\\s*'([^']*)'.*}");
  private static final Matcher simpleLanguageLine = languageDef.matcher("");
  private static final Pattern languageRedirection =
      Pattern.compile("l\\['([^']*)']\\s*=\\s*l\\['([^']*)']");
  private static final Matcher languageRedirectionLine = languageRedirection.matcher("");


  private static boolean loadLanguagesFromPage(WiktionaryIndex wi) {
    l = new HashMap<>();
    String lgdata = wi.getTextOfPage("Module:langues/data");
    if (null == lgdata) {
      logger.error("Could not retrieve language data in French Wiktionary dump.");
      return false;
    }
    Scanner scanner = new Scanner(lgdata);
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      simpleLanguageLine.reset(line);

      if (simpleLanguageLine.find()) {
        l.put(simpleLanguageLine.group(1), simpleLanguageLine.group(2));
      } else {
        languageRedirectionLine.reset(line);
        if (languageRedirectionLine.find()) {
          l.put(languageRedirectionLine.group(1), l.get(languageRedirectionLine.group(2)));
        }
      }
    }
    scanner.close();
    return true;
  }

}
