package org.getalp.dbnary.languages.ind;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.util.Locale;
import java.util.Map;

import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class IndonesianModulesPatcherWikiModel extends DbnaryWikiModel {

  public IndonesianModulesPatcherWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map) throws WikiModelContentException {
    // This allows the handling of the Chinese wikimodel that uses capitalized module names
    String pagename = parsedPagename.pagename.toLowerCase();
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      switch (pagename) {
        case "table/getunprotectedmetatable":
          // This module uses debug, which is undefined in our Lua Engine
          return getAndPatchModule(parsedPagename, map, t -> t.replace("local _getmetatable = debug.getmetatable", "local _getmetatable = nil"));
      }
    }
    return super.getRawWikiContent(parsedPagename, map);
  }

}
