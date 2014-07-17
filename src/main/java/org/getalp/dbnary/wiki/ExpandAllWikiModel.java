package org.getalp.dbnary.wiki;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.filter.WikipediaParser;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.IEventListener;
import info.bliki.wiki.model.WikiModel;

public class ExpandAllWikiModel extends DbnaryWikiModel {
		
	protected Set<String> templates = null;
	
	public ExpandAllWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
		this((WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public ExpandAllWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
	}

	/**
	 * Convert a wiki code to plain text, while keeping track of all template calls.
	 * @param definition the wiki code
	 * @param templates if not null, the method will add all called templates to the set.
	 * @return
	 */
	public String expandAll(String definition, Set<String> templates) {
		this.templates = templates;
		return render(new PlainTextConverter(), definition).trim();
	}
	
	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
					throws IOException {
		if (templates != null) templates.add(templateName);
		super.substituteTemplateCall(templateName, parameterMap, writer);
	}
	
}
