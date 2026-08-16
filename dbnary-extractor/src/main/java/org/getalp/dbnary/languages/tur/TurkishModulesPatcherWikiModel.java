package org.getalp.dbnary.languages.tur;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.util.Locale;
import java.util.Map;

import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class TurkishModulesPatcherWikiModel extends DbnaryWikiModel {

  public TurkishModulesPatcherWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map) throws WikiModelContentException {
    // This allows the handling of the Chinese wikimodel that uses capitalized module names
    String pagename = parsedPagename.pagename.toLowerCase();
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      switch (pagename) {
        case "kaynak/kb1":
          // This module contains too many locals and hits the 200 local per chunk limit of Lua.
          // This is problematic here as we compile lua code (hence the limit, while interpreted lua
          // seems not to bear the same issue)
          // we fix it by a hack that replaces the local functions and first level vars with global functions
          return getAndPatchModule(parsedPagename, map, t -> t.replace("\nlocal function ", "\nfunction "), t -> t.replace("\n\tlocal ", "\n\t"));
      }
    }
    return super.getRawWikiContent(parsedPagename, map);
  }

}
