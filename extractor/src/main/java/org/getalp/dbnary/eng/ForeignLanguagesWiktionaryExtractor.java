package org.getalp.dbnary.eng;

import java.util.regex.Matcher;

import org.getalp.dbnary.IWiktionaryDataHandler;

public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {

	public ForeignLanguagesWiktionaryExtractor(IWiktionaryDataHandler wdh) {
		super(wdh);
	}

	@Override
	public void extractData() {

        // System.out.println(pageContent);
        Matcher languageFilter = sectionPattern.matcher(pageContent);
        int startForeignSection = -1;
		String previousLang = null;
		String lang = null;
        while (languageFilter.find()) {
        	lang = getLanguageInHeader(languageFilter);
            if (lang != null && !lang.equals("English") && languageFilter.group().charAt(2) != '=') {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), lang);
				}
				previousLang = lang;
				startForeignSection = languageFilter.end();
			} else {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), previousLang);
				}
				previousLang = null;
				startForeignSection = -1;
			}
        }
        // Either the filter is at end of sequence or on English language header.
        if (languageFilter.hitEnd()) {
        	if (startForeignSection != -1) {
				extractForeignData(startForeignSection, pageContent.length(), previousLang);
			}
        }
	}

	private void extractForeignData(int startOffset, int endOffset, String lang) {
		Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName, lang);
        gotoNoData(m);
        // WONTDO: should I use a macroOrLink pattern to detect translations that are not macro based ?
        // DONE: (priority: top) link the definition node with the current Part of Speech
        // DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
        // DONE: handle alternative spelling
        // DONE: extract synonyms
        // DONE: extract antonyms
        while (m.find()) {
            String title = m.group(1).trim();
            switch (state) {
            case NODATA:
                if (title.equals("Translations")) {
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                	gotoPronBlock(m);
                }
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (title.equals("Translations")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                    leaveDefBlock(m);
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                    leaveDefBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                } 
                break;
            case TRADBLOCK:
                if (title.equals("Translations")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                    leaveTradBlock(m);
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                    leaveTradBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                } 
                break;
            case ORTHOALTBLOCK:
                if (title.equals("Translations")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                    leaveOrthoAltBlock(m);
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                	leaveOrthoAltBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
                if (title.equals("Translations")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                	leaveNymBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveNymBlock(m);
                    gotoNoData(m);
                }
                break;
            case PRONBLOCK:
            	if (title.equals("Translations")) {
                    leavePronBlock(m);
                    gotoTradBlock(m);
                } else if (WiktionaryDataHandler.isValidPOS(title)) {
                	leavePronBlock(m);
                    gotoDefBlock(m);
                } else if (title.equals("Alternative spellings")) {
                	leavePronBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(title)) {
                	leavePronBlock(m);
                    gotoNymBlock(m);
                } else if (title.equals("Pronunciation")) {
                	leavePronBlock(m);
                    gotoPronBlock(m);
                } else {
                	leavePronBlock(m);
                    gotoNoData(m);
                }
            	break;
            default:
                assert false : "Unexpected state while extracting translations from dictionary.";
            } 
        }
        // Finalize the entry parsing
        switch (state) {
        case NODATA:
            break;
        case DEFBLOCK:
            leaveDefBlock(m);
            break;
        case TRADBLOCK:
            leaveTradBlock(m);
            break;
        case ORTHOALTBLOCK:
            leaveOrthoAltBlock(m);
            break;
        case NYMBLOCK:
            leaveNymBlock(m);
            break;
        case PRONBLOCK:
        	leavePronBlock(m);
            break;
        default:
            assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
        } 
        wdh.finalizeEntryExtraction();
	}

	public String getLanguageInHeader(Matcher m) {
		if (null != m.group(1).trim()){
			return m.group(1).trim();
		}
		else if (null != m.group(2)){
			return m.group(2);
		}
		else 
			return null;
	}
}
