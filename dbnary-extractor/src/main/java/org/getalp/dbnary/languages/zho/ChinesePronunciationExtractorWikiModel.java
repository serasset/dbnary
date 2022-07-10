package org.getalp.dbnary.languages.zho;

import org.getalp.dbnary.api.WiktionaryPageSource;

import java.util.Locale;

public class ChinesePronunciationExtratorWikiModel extends ChineseDbnaryWikiModel{
    public ChinesePronunciationExtratorWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
    }
}
