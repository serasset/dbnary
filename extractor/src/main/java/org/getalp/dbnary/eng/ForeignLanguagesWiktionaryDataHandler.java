package org.getalp.dbnary.eng;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.PronunciationPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ForeignLanguagesWiktionaryDataHandler extends WiktionaryDataHandler {

    private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);

    private HashMap<String, String> prefixes = new HashMap<String, String>();

    private String currentPrefix = null;
    private String currentEntryLanguage = null;

    public ForeignLanguagesWiktionaryDataHandler(String lang) {
        super(lang);
    }

    public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
        setCurrentLanguage(lang);
        super.initializeEntryExtraction(wiktionaryPageName);
    }

    public void setCurrentLanguage(String lang) {
        currentEntryLanguage = lang;//!!!! check this change
        //wktLanguageEdition = LangTools.getPart1OrId(lang);

        lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
        currentPrefix = getPrefix(lang);
    }

    @Override
    public String getCurrentEntryLanguage() {
        return currentEntryLanguage;
    }

    @Override
    public void finalizeEntryExtraction() {
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

    public String getPrefix(String lang) {
        if (this.prefixes.containsKey(lang))
            return this.prefixes.get(lang);

        lang = LangTools.normalize(EnglishLangToCode.threeLettersCode(lang));
        String prefix = DBNARY_NS_PREFIX + "/eng/" + lang + "/";
        prefixes.put(lang, prefix);
        aBox.setNsPrefix(lang + "-eng", prefix);
        return prefix;
    }

    @Override
    public void registerEtymologyPos() {
        registerEtymologyPos(wktLanguageEdition);
    }

    @Override
    public void registerPronunciation(String pron, String lang) {
        // Catch the call for foreign languages and disregard passed language
        lang = wktLanguageEdition + "-fonipa";
        if (null == currentCanonicalForm) {
            currentSharedPronunciations.add(new PronunciationPair(pron, lang));
        } else {
            registerPronunciation(currentCanonicalForm, pron, lang);
        }
    }
}
