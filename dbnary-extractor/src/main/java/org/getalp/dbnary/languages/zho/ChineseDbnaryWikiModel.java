package org.getalp.dbnary.languages.zho;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.commons.EnglishLikeModulesPatcherWikiModel;

public class ChineseDbnaryWikiModel extends EnglishLikeModulesPatcherWikiModel {

  private final Stack<String> linkCallStack = new Stack<>();

  public ChineseDbnaryWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    // Chinese edition systematically capitalize modules and templates names
    ParsedPageName originalPagename = parsedPagename;
    if ((parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        || parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY))
        && !parsedPagename.pagename.isEmpty()
        && Character.isLowerCase(parsedPagename.pagename.charAt(0))) {
      parsedPagename = new ParsedPageName(parsedPagename.namespace,
          Character.toUpperCase(originalPagename.pagename.charAt(0))
              + originalPagename.pagename.substring(1),
          parsedPagename.valid);
    }
    String wikiContent = super.getRawWikiContent(parsedPagename, map);
    if (null == wikiContent) {
      // Chinese templates are supposed to begin with an uppercase char
      // but if the uppercase variant does not exist, we try the original name
      wikiContent = super.getRawWikiContent(originalPagename, map);
    }
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      String pagename = parsedPagename.pagename.toLowerCase();
      if (pagename.equals("debug/track")) {
        wikiContent = patchModule(parsedPagename.pagename, wikiContent,
            t -> t.replace("return function(input)",
                "return function(input)\n" + "\tif 1 == 1 then return true end\n"));
      } else if (parsedPagename.pagename.equalsIgnoreCase("parameters")) {
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
      } else if (parsedPagename.pagename.equalsIgnoreCase("Table/getUnprotectedMetatable")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replaceAll("local\\s+_getmetatable\\s*=\\s*debug.getmetatable\\s*\n", //
                "local _getmetatable = nil\n"));
      }
    }
    return wikiContent;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    if (templateName.equals("check deprecated lang param usage")) {
      writer.append(parameterMap.getOrDefault("1", ""));
    } else if (templateName.equals("分類") || templateName.equalsIgnoreCase("Redlink category")) {
      // Do nothing
    } else if (templateName.equalsIgnoreCase("l")) {
      // A nasty bug in the Chinese wiktionary, the l template is used to display links, but
      // it is sometimes used to link to the page itself, which leads to infinite recursion, as
      // the page is parsed again and again by the Lua link module.
      String linkTarget = parameterMap.get("2");
      if (linkCallStack.contains(linkTarget)) {
        logger.debug("Avoiding infinite recursion on link to {} in {}", linkTarget, getPageName());
        if (linkTarget.contains("[["))
          writer.append(linkTarget);
        else
          writer.append("[[").append(linkTarget).append("]]");
      } else {
        linkCallStack.push(linkTarget);
        super.substituteTemplateCall(templateName, parameterMap, writer);
        linkCallStack.pop();
      }
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

}
