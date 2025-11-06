package org.getalp.dbnary.languages.kur;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;

public class DefinitionExpanderWikiModel extends ExpandAllWikiModel {

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("nexşe".equals(templateName)) {
      // This template call leads to LuaError, catch it and simply output '(Bajar)'
      writer.append(parameterMap.getOrDefault("text", ""));
    } else
      super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    String wikiContent = super.getRawWikiContent(parsedPagename, map);
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      String pagename = parsedPagename.pagename.toLowerCase();
      if (pagename.equalsIgnoreCase("parameters")) {
        // December 2024: English Module:parameters now uses the traceback that is only available in
        // debug
        // mode avoid an error while compiling the module as debug is not available in our execution
        // environment and when English community does a stupid thing, it always percolate to
        // Chinese language edition... — patch it
        return getAndPatchModule(parsedPagename, map,
            t -> t.replaceAll("local\\s+traceback\\s*=\\s*debug.traceback\n", //
                "local function traceback() \n" //
                    + " return \"\"\n" //
                    + "end\n"));
      }
    }
    return wikiContent;
  }
}
