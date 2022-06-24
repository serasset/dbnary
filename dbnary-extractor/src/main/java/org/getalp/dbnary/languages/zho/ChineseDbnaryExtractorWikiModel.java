package org.getalp.dbnary.languages.zho;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

import java.util.Locale;
import java.util.Map;

public class ChineseDbnaryExtractorWikiModel extends DbnaryWikiModel {
    public ChineseDbnaryExtractorWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
    }

    @Override
    public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map) throws WikiModelContentException {
        ParsedPageName originalPageName = parsedPagename;
        //if it's a template and not start with an uppercase template
        if ((parsedPagename.namespace.isType((INamespace.NamespaceCode.TEMPLATE_NAMESPACE_KEY))||parsedPagename.namespace.isType(INamespace.NamespaceCode.MODULE_NAMESPACE_KEY)) && (Character.isLowerCase(parsedPagename.pagename.charAt(0)) && parsedPagename.pagename.length() > 0)) {
            parsedPagename = new ParsedPageName(parsedPagename.namespace, Character.toUpperCase(originalPageName.pagename.charAt(0)) + originalPageName.pagename.substring(1), parsedPagename.valid);
        }
        String wikiContent = super.getRawWikiContent(parsedPagename, map);
        if (null == wikiContent) {
            //Chinese template are supposed to begin with an uppercase char
            wikiContent = super.getRawWikiContent(originalPageName, map);
        }
        return wikiContent;
    }
}
