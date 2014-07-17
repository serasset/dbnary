package org.getalp.dbnary.ita;

import info.bliki.wiki.filter.WikipediaParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

public class ItalianPronunciationExtractorWikiModel extends DbnaryWikiModel {
	
	private WiktionaryDataHandler delegate;
	
	public ItalianPronunciationExtractorWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public ItalianPronunciationExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
	}

	public void parsePronunciation(String block) {
		initialize();
		if (block == null) {
			return;
		}
		WikipediaParser.parse(block, this, true, null);
		initialize();
	}

	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		if ("IPA".equals(templateName)) {
            if (parameterMap.get("4") != null) delegate.registerPronunciation(parameterMap.get("4"), "it-fonipa");
            if (parameterMap.get("3") != null) delegate.registerPronunciation(parameterMap.get("3"), "it-fonipa");
            if (parameterMap.get("2") != null) delegate.registerPronunciation(parameterMap.get("2"), "it-fonipa");
            if (parameterMap.get("1") != null) delegate.registerPronunciation(parameterMap.get("1"), "it-fonipa");
		} if ("SAMPA".equals(templateName)) {
            // TODO !
		} else {
			// System.err.println("Called template: " + templateName + " while parsing translations of: " + this.getImageBaseURL());
			// Just ignore the other template calls (uncomment to expand the template calls).
			// super.substituteTemplateCall(templateName, parameterMap, writer);
		}
	}

}
