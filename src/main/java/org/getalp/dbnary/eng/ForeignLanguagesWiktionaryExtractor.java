package org.getalp.dbnary.eng;

import java.util.regex.Matcher;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;

public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {

	public ForeignLanguagesWiktionaryExtractor(WiktionaryDataHandler wdh) {
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
            switch (state) {
            case NODATA:
                if (m.group(1).equals("Translations")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	gotoPronBlock(m);
                }
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("Translations")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                    leaveDefBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                } 
                break;
            case TRADBLOCK:
                if (m.group(1).equals("Translations")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                    leaveTradBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                } 
                break;
            case ORTHOALTBLOCK:
                if (m.group(1).equals("Translations")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	leaveOrthoAltBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
                if (m.group(1).equals("Translations")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	leaveNymBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveNymBlock(m);
                    gotoNoData(m);
                }
                break;
            case PRONBLOCK:
            	if (m.group(1).equals("Translations")) {
                    leavePronBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                	leavePronBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                	leavePronBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                	leavePronBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
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
		if (null != m.group(1)){
			return m.group(1);
		}
		else if (null != m.group(2)){
			return m.group(2);
		}
		else 
			return null;
	}
}
