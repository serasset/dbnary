package org.getalp.dbnary.languages.cat;

import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ExpandAllWikiModelCat extends ExpandAllWikiModel {


    public ExpandAllWikiModelCat(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
    }


    @Override
    public void substituteTemplateCall(String templateName, Map<String, String> parameterMap, Appendable writer) throws IOException {
        if (!templateName.equals("maraca")) {
            super.substituteTemplateCall(templateName, parameterMap, writer);
        }
    }
}
