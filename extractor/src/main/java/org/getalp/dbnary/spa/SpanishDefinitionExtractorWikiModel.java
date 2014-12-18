package org.getalp.dbnary.spa;

import info.bliki.wiki.filter.PlainTextConverter;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class SpanishDefinitionExtractorWikiModel extends DbnaryWikiModel {

    private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

    // static Set<String> ignoredTemplates = new TreeSet<String>();
	// static {
	// 	ignoredTemplates.add("Wikipedia");
	// 	ignoredTemplates.add("Incorrect");
	// }

	private IWiktionaryDataHandler delegate;


	public SpanishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}

	public SpanishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
	}

	public void parseDefinition(String definition, String senseNum) {
		// Render the definition to plain text, while ignoring the example template
        String def = render(new PlainTextConverter(), definition).trim();
		if (null != def && ! def.equals(""))
			delegate.registerNewDefinition(def, senseNum);
	}
	
	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		// Currently just expand the definition to get the full text.
		super.substituteTemplateCall(templateName, parameterMap, writer);
	}

}
