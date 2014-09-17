package org.getalp.dbnary.fra;

import java.util.regex.Matcher;

import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;

public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {

	public ForeignLanguagesWiktionaryExtractor(IWiktionaryDataHandler wdh) {
		super(wdh);
	}

	@Override
	public void extractData() {
		// System.out.println(pageContent);
		Matcher languageFilter = languageSectionPattern.matcher(pageContent);
		int startForeignSection = -1;
		String previousLang = null;
		String lang = null;
		while (languageFilter.find()) {
			if ((lang = getLanguageInHeader(languageFilter)) != null && !lang.equals("fr")) {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), lang);
				}
				previousLang = lang;
				startForeignSection = languageFilter.end();
			} else {
				if (startForeignSection != -1) {
					extractForeignData(startForeignSection, languageFilter.start(), lang);
				}
				previousLang = null;
				startForeignSection = -1;
			}
		}
		// Either the filter is at end of sequence or on French language header.
		if (languageFilter.hitEnd()) {
			if (startForeignSection != -1) {
				extractForeignData(startForeignSection, pageContent.length(), previousLang);
			}
		}
	}

	private void extractForeignData(int startOffset, int endOffset, String lang) {
		Matcher m = WikiPatterns.macroPattern.matcher(pageContent);
		m.region(startOffset, endOffset);
		wdh.initializeEntryExtraction(wiktionaryPageName, lang);

		// WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are not macro based ?
		// DONE: (priority: top) link the definition node with the current Part of Speech
		// DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
		// DONE: handle alternative spelling
		// DONE: extract synonyms
		// DONE: extract antonyms
		// DONE: add an IGNOREPOS state to ignore the entire part of speech

		gotoNoData(m);
		String pos = null;
		String nym = null;
		while (m.find()) {
			switch (state) {
			case NODATA:
				if (isTranslation(m)) {
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					gotoNoData(m);
				} else {
					// unknownHeaders.add(m.group(0));
				}
				break;
			case DEFBLOCK:
				// Iterate until we find a new section
				if (isTranslation(m)) {
					leaveDefBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveDefBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m,pos);
				} else if (isAlternate(m)) {
					leaveDefBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveDefBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveDefBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveDefBlock(m);
					gotoNoData(m);
				} else {
					// leaveDefBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case TRADBLOCK:
				if (isTranslation(m)) {
					leaveTradBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveTradBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveTradBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveTradBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveTradBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveTradBlock(m);
					gotoNoData(m);
				} else {
					//leaveTradBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case ORTHOALTBLOCK:
				if (isTranslation(m)) {
					leaveOrthoAltBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveOrthoAltBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveOrthoAltBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveOrthoAltBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveOrthoAltBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveOrthoAltBlock(m);
					gotoNoData(m);
				} else {
					// leaveOrthoAltBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case NYMBLOCK:
				if (isTranslation(m)) {
					leaveNymBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveNymBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else 
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveNymBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveNymBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveNymBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveNymBlock(m);
					gotoNoData(m);
				} else {
					// leaveNymBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case PRONBLOCK:
				if (isTranslation(m)) {
					leavePronBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leavePronBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leavePronBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leavePronBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leavePronBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leavePronBlock(m);
					gotoNoData(m);
				} else {
					// leavePronBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case IGNOREPOS:
				if (isTranslation(m)) {
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
				} else if (null != (nym = isNymHeader(m))) {
				} else if (isPronounciation(m)) {
					// gotoPronBlock(m);
				} else if (isValidSection(m)) {
					// gotoIgnorePos();
				} else {
					// unknownHeaders.add(m.group(0));
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
		case IGNOREPOS:
			break;
		default:
			assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
		} 

		wdh.finalizeEntryExtraction();
	}

	public String getLanguageInHeader(Matcher m) {
		if (null != m.group(1))
			return m.group(1);
		else if (null != m.group(2))
			return m.group(2);
		else 
			return null;
	}
	
	@Override
	void leaveDefBlock(Matcher m) {
    	int end = computeRegionEnd(definitionBlockStart, m);
        extractDefinitions(definitionBlockStart, end);
        extractForeignPronounciation(definitionBlockStart, end);
        definitionBlockStart = -1;
    }
	
	private void extractForeignPronounciation(int startOffset, int endOffset) {
		Matcher pronMatcher = pronunciationPattern.matcher(pageContent);
		pronMatcher.region(startOffset, endOffset);

		while (pronMatcher.find()) {
    		String pron = pronMatcher.group(1);
    		String lang = pronMatcher.group(2);
    		
    		if (null == pron || pron.equals("")) return;
    		if (lang == null || lang.equals("")) return;
    		
    		if (pron.startsWith("1=")) pron = pron.substring(2);
    		if (lang.startsWith("|2=")) lang = lang.substring(2);
    		if (lang.startsWith("|lang=")) lang = lang.substring(5);
    		if (! pron.equals("")) wdh.registerPronunciation(pron, lang + "-fonipa");
    		
		}
    }
}
