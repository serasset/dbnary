package org.getalp.dbnary.eng;

import org.getalp.dbnary.PronunciationPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForeignLanguagesWiktionaryDataHandler extends WiktionaryDataHandler {

    private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);

    private String currentPrefix = null;

    public ForeignLanguagesWiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
        setCurrentLanguage(extractedLang);
        super.initializeEntryExtraction(wiktionaryPageName, extractedLang);
    }

    public void setCurrentLanguage(String lang) {
        extractedLang = lang;//!!!! check this change
        //extractedLang = LangTools.getPart1OrId(lang);
        lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
        currentPrefix = getPrefixe(lang);
    }

    @Override
    public void finalizeEntryExtraction() {
        extractedLang = null;
        currentPrefix = null;
    }

    @Override
    public String currentLexEntry() {
        // TODO Auto-generated method stub
        return currentWiktionaryPageName;
    }

    @Override
    public String getPrefix() {
        return currentPrefix;
    }

    @Override
    public void registerEtymologyPos() {
        registerEtymologyPos(extractedLang);
    }

    @Override
    public void registerPronunciation(String pron, String lang) {
        // Catch the call for foreign languages and disregard passed language
        lang = extractedLang + "-fonipa";
        if (null == currentCanonicalForm) {
            currentSharedPronunciations.add(new PronunciationPair(pron, lang));
        } else {
            registerPronunciation(currentCanonicalForm, pron, lang);
        }
    }
}
