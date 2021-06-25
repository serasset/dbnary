package org.getalp.dbnary.eng;

import java.util.HashMap;
import org.getalp.LangTools;
import org.getalp.dbnary.PronunciationPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Do not use this subclass, not compatible with Model based extraction.
 */
public class ForeignLanguagesWiktionaryDataHandler extends WiktionaryDataHandler {

  private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);

  private HashMap<String, String> prefixes = new HashMap<>();

  private String currentEntryLanguage = null;
  private String currentPrefix = null;

  public ForeignLanguagesWiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLanguageSection__noModel(String wiktionaryPageName) {
    super.initializeLanguageSection__noModel(wiktionaryPageName, currentEntryLanguage,
        currentEntryLanguageName);
  }


  public void setCurrentLanguage(String lang, String languageName) {
    lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
    currentEntryLanguage = LangTools.normalize(EnglishLangToCode.threeLettersCode(lang));
    // currentEntryLanguage = lang;
    currentEntryLanguageName = languageName;
    // wktLanguageEdition = LangTools.getPart1OrId(lang);
    currentPrefix = getPrefix(currentEntryLanguage);
  }

  @Override
  public String getCurrentEntryLanguage() {
    return currentEntryLanguage;
  }

  public void finalizeLanguageSection() {
    currentPrefix = null;
  }

  @Override
  public String currentLexEntry() {
    return currentWiktionaryPageName;
  }

  @Override
  public String getPrefix() {
    return currentPrefix;
  }

  private String getPrefix(String lang) {
    if (this.prefixes.containsKey(lang)) {
      return this.prefixes.get(lang);
    }
    String prefix = DBNARY_NS_PREFIX + "/eng/" + lang + "/";
    prefixes.put(lang, prefix);
    aBox.setNsPrefix(lang + "-eng", prefix);
    return prefix;
  }

  @Override
  public void registerEtymologyPos(String wiktionaryPageName) {
    registerEtymologyPos(currentEntryLanguage, currentEntryLanguageName, wiktionaryPageName);
  }

  @Override
  public void registerPronunciation(String pron, String lang) {
    // Catch the call for foreign languages and disregard passed language
    lang = getCurrentEntryLanguage() + "-fonipa";
    if (null == currentCanonicalForm) {
      currentSharedPronunciations.add(new PronunciationPair(pron, lang));
    } else {
      registerPronunciation(currentCanonicalForm, pron, lang);
    }
  }

}
