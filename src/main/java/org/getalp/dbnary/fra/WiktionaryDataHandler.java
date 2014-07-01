package org.getalp.dbnary.fra;

import org.getalp.dbnary.LemonBasedRDFDataHandler;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jakse
 */

public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {
	private final static String FrenchConjugationPagePrefix = "Annexe:Conjugaison en français/";

	private static Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

	public WiktionaryDataHandler(String lang) {
		super(lang);
	}

	private static String getTemplateCall(String page, int beginPos) {
		// precondition: page.charAt(beginPos) is the first '{' of the template call

		int openedBrackets = 0, 
		    len = page.length(),
		    curPos = beginPos;

		do {
			if (page.charAt(curPos) == '}') {
				if (page.charAt(curPos+1) == '}') {
					openedBrackets--;
				}
				curPos++;
			} else if (page.charAt(curPos) == '{') {
				if (page.charAt(curPos+1) == '{') {
					openedBrackets++;
					curPos++;
				}
			}
			curPos++;
		} while (openedBrackets > 0 && curPos + 1 < len);

		return page.substring(beginPos, curPos);
	}

	@Override
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
		super.addPartOfSpeech(originalPOS, normalizedPOS, normalizedType);
    	if (normalizedPOS == verbPOS && ((normalizedType == wordEntryType) || (normalizedType == lexEntryType))) {
			String conjugationPageContent = wi.getTextOfPage(FrenchConjugationPagePrefix + currentLexEntry());

			if (conjugationPageContent == null) {
				log.debug("Cannot get conjugation page for '" + currentLexEntry() + "'");
			} else {
				FrenchExtractorWikiModel dbnmodel = null;
				int curPos = -1;
			    do {
					curPos++;
					curPos = conjugationPageContent.indexOf("{{fr-conj", curPos);
				    if (curPos != -1 && !conjugationPageContent.startsWith("{{fr-conj-intro", curPos)) {
					    String templateCall = getTemplateCall(conjugationPageContent, curPos);

						if (dbnmodel == null) {
							dbnmodel = new FrenchExtractorWikiModel(this, wi, new Locale("fr"), "/${image}", "/${title}");
						}

						if (templateCall.startsWith("{{fr-conj/Tableau-impersonnels")) {
							dbnmodel.parseImpersonnalTableConjugation(templateCall);
						} else {
							dbnmodel.parseConjugation(templateCall);
						}
					}
			    } while (curPos != -1);
			}
    	}
	}
}