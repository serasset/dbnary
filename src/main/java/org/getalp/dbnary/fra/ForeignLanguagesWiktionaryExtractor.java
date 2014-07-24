package org.getalp.dbnary.fra;

import java.util.regex.Matcher;

import java.util.Locale;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;

public class ForeignLanguagesWiktionaryExtractor extends WiktionaryExtractor {
	public ForeignLanguagesWiktionaryExtractor(WiktionaryDataHandler wdh) {
		super(wdh);
	}

	@Override
	public void extractData() {
		extractData(true);
	}
}