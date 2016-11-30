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

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
	initializeEntryExtraction(wiktionaryPageName, currentEntryLanguage);
    }

    public boolean setCurrentLanguage(String lang) {
	currentEntryLanguage = EnglishLangToCode.threeLettersCode(lang);
	if (currentEntryLanguage == null){
	    log.debug("Null input language");
	    return false;
	}

	wktLanguageEdition = LangTools.getPart1OrId(lang);
	if (wktLanguageEdition == null){
	    log.debug("Function getPart1OrId returns null for language {}.");
	    return false;
	}
	
	currentPrefix = getPrefix(lang);
	if (currentPrefix == null){
	    log.debug("Null prefix: ignoring etymology entry");
	    return false;
	}

	
	lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
	return true;
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
    /*
    public String getPrefix(String lang) {
	String prefix = DBNARY_NS_PREFIX + "/eng/";
	lang = LangTools.normalize(EnglishLangToCode.threeLettersCode(lang));
       	if (this.prefixes.containsKey(lang)){
	    return this.prefixes.get(lang);
	}

	if (! lang.equals("eng")){
	    prefix = prefix + lang + "/";
	    aBox.setNsPrefix(lang + "-eng", prefix);
	}//else prefix = DBNARY_NS_PREFIX + "/eng/";
         
	prefixes.put(lang, prefix);
	return prefix;
	}*/
    
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
