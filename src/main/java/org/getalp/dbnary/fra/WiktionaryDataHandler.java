package org.getalp.dbnary.fra;

import org.getalp.dbnary.LemonBasedRDFDataHandler;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;

/**
 * @author jakse
 */

public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {
    protected final static String conjugationPatternString = "\\{\\{fr-conj-[^\\|\\}]\\|?[^\\}]*\\}\\}";

	private final static Pattern conjugationPattern;
	private final static String FrenchConjugationPagePrefix = "Annexe:Conjugaison en français/";

	static {
	    conjugationPattern = Pattern.compile(conjugationPatternString);
    }

	public WiktionaryDataHandler(String lang) {
		super(lang);
	}

	@Override
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
		super.addPartOfSpeech(originalPOS, normalizedPOS, normalizedType);
    	if (normalizedPOS == verbPOS && ((normalizedType == wordEntryType) || (normalizedType == lexEntryType))) {
			String conjugationPageContent = wi.getTextOfPage(FrenchConjugationPagePrefix + currentLexEntry());
			Matcher conjugationFilter = conjugationPattern.matcher(conjugationPageContent);

		    if (conjugationFilter.find()) {
				FrenchConjugationExtractorWikiModel dbnmodel = new FrenchConjugationExtractorWikiModel(this, wi, new Locale("fr"), "/${image}", "/${title}");
				dbnmodel.parseConjugation(conjugationFilter.group());
		    }
    	}
	}
}