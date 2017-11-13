package org.getalp.dbnary.eng;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.IWiktionaryDataHandler;


public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {

  protected final static String level2HeaderPatternString = "^==([^=].*[^=])==$";
  protected final static Pattern level2HeaderPattern;

  static {
    level2HeaderPattern = Pattern.compile(level2HeaderPatternString, Pattern.MULTILINE);
  }

  private ForeignLanguagesWiktionaryDataHandler flwdh; // English specific version of the data
  // handler.

  public ForeignLanguagesWiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    if (wdh instanceof ForeignLanguagesWiktionaryDataHandler) {
      flwdh = (ForeignLanguagesWiktionaryDataHandler) wdh;
    } else {
      log.error(
          "Foreign Language Wiktionary Extractor instanciated with a non foreign language data handler!");
    }
  }

  @Override
  public boolean filterOutPage(String pagename) {
    return pagename.contains(":") && !pagename.startsWith("Reconstruction");
  }

  @Override
  public void extractData() {
    if (wiktionaryPageName.startsWith("Reconstruction:")) {
      wiktionaryPageName = wiktionaryPageName.split("/", 2)[1];
    }
    Matcher l1 = level2HeaderPattern.matcher(pageContent);
    int nonEnglishSectionStart = -1;
    String lang = null;
    String languageName = null;
    while (l1.find()) {
      wdh.initializePageExtraction(wiktionaryPageName);
      // System.err.println(l1.group());
      if (-1 != nonEnglishSectionStart) {
        // Parsing a previous non english section;
        extractNonEnglishData(lang, languageName, nonEnglishSectionStart, l1.start());
        nonEnglishSectionStart = -1;
      }
      languageName = l1.group(1).trim();
      if (null != (lang = getNonEnglishLanguageCode(languageName))) {
        nonEnglishSectionStart = l1.end();
      }
      wdh.finalizePageExtraction();
    }
    if (-1 != nonEnglishSectionStart) {
      // System.err.println("Parsing previous non English entry");
      wdh.initializePageExtraction(wiktionaryPageName);
      extractNonEnglishData(lang, languageName, nonEnglishSectionStart, pageContent.length());
      wdh.finalizePageExtraction();
    }
  }

  private String getNonEnglishLanguageCode(String t) {
    if (t.equals("English")) {
      return null;
    } else {
      String c = EnglishLangToCode.threeLettersCode(t);
      if (null == c) {
        log.debug("Unknown language: {} in {}", t, this.wiktionaryPageName);
      }

      return c;
    }
  }

  protected void extractNonEnglishData(String lang, String languageName, int startOffset,
      int endOffset) {
    flwdh.setCurrentLanguage(lang, languageName);
    super.extractEnglishData(startOffset, endOffset);
  }
}
