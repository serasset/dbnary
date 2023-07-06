package org.getalp.dbnary.languages.cat;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ExpandAllWikiModelCat extends ExpandAllWikiModel {


  public ExpandAllWikiModelCat(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }


  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("la-pron")) {
      return "{{pronafi|la|/{{#invoke:la-pron|show|{{{1|{{PAGENAME}}}}}}}/|{{#if:{{{2|}}}|/{{#invoke:la-pron|show|{{{2}}}}}/}}}}\n";
    }
    return super.getRawWikiContent(parsedPagename, map);
  }
}
