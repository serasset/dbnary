package org.getalp.dbnary.fra;

import java.util.HashMap;
import org.getalp.LangTools;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForeignLanguagesWiktionaryDataHandler extends OntolexBasedRDFDataHandler {


  private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);

  private HashMap<String, String> prefixes = new HashMap<String, String>();

  private String currentEntryLanguage = null;
  private String currentPrefix = null;

  public ForeignLanguagesWiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);

  }

  public void initializeLanguageSection(String wiktionaryPageName, String lang) {
    currentPrefix = getPrefix(lang);
    super.initializeLanguageSection(wiktionaryPageName);
  }

  public void setCurrentLanguage(String lang, String languageName) {
    lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
    currentEntryLanguage = LangTools.normalize(LangTools.threeLettersCode(lang));
    // currentEntryLanguage = lang;
    // currentEntryLanguageName = languageName;
    // wktLanguageEdition = LangTools.getPart1OrId(lang);
    currentPrefix = getPrefix(currentEntryLanguage);
  }

  @Override
  public void finalizeLanguageSection() {
    currentPrefix = null;
  }


  @Override
  public String currentLexEntry() {
    // TODO Auto-generated method stub
    return currentWiktionaryPageName;
  }

  // TODO: Refactor and generalize the prefixes and current entry languages in main ontolex based
  // data handler so that
  // the current english implementation is available for all languages.
  @Override
  public String getPrefix() {
    return currentPrefix;
  }

  public String getPrefix(String lang) {
    if (this.prefixes.containsKey(lang)) {
      return this.prefixes.get(lang);
    } else {
      lang = LangTools.normalize(lang);
      String prefix = DBNARY_NS_PREFIX + "/fra/" + lang + "/";
      prefixes.put(lang, prefix);
      aBox.setNsPrefix(lang + "-fra", prefix);
      return prefix;
    }
  }
}
