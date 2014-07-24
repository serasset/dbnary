package org.getalp.dbnary.eng;

import java.util.HashMap;

import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.LangTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForeignLanguagesWiktionaryDataHandler extends LemonBasedRDFDataHandler {

	private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);
	
	private HashMap<String,String> prefixes = new HashMap<String,String>();

	private String currentEntryLanguage = null;
	private String currentPrefix = null;

	public ForeignLanguagesWiktionaryDataHandler(String lang) {
		super(lang);
	}
	
	public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
		currentEntryLanguage = lang;
		currentPrefix = getPrefixe(lang);
		super.initializeEntryExtraction(wiktionaryPageName);
    }

	@Override
	public void finalizeEntryExtraction() {
		currentEntryLanguage = null;
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
	
	public String getPrefixe(String lang){
		if(this.prefixes.containsKey(lang))
			return this.prefixes.get(lang);

		lang = LangTools.normalize(EnglishLangToCode.threeLettersCode(lang));
		String prefix = DBNARY_URL + "/" + lang + "/eng/";
		prefixes.put(lang, prefix);
		aBox.setNsPrefix(lang + "-eng", prefix);
		return prefix;
	}
}