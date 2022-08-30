package org.getalp.dbnary.languages.zho;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class ChineseDbnaryWikiModel extends DbnaryWikiModel {

  public ChineseDbnaryWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    ParsedPageName originalPagename = parsedPagename;
    if ((parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        || parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY))
        && parsedPagename.pagename.length() > 0
        && Character.isLowerCase(parsedPagename.pagename.charAt(0))) {
      parsedPagename = new ParsedPageName(parsedPagename.namespace,
          Character.toUpperCase(originalPagename.pagename.charAt(0))
              + originalPagename.pagename.substring(1),
          parsedPagename.valid);
    }
    String wikiContent = super.getRawWikiContent(parsedPagename, map);
    if (null == wikiContent) {
      // Chinese templates are supposed to begin with an uppercase char
      wikiContent = super.getRawWikiContent(originalPagename, map);
    }
    return wikiContent;
  }
}
