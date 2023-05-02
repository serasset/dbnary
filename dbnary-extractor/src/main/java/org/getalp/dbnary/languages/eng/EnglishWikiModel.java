package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.iso639.ISO639_3;

public class EnglishWikiModel extends DbnaryWikiModel {

  public EnglishWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (!templateName.startsWith("tracking")) {
      if (templateName.equals("check deprecated lang param usage")) {
        writer.append(parameterMap.get("1"));
      } else if ("str left".equals(templateName)) {
        try {
          int l = Integer.parseInt(parameterMap.get("2"));
          String val = parameterMap.getOrDefault("1", "");
          writer.append(val.substring(0, Math.min(l, val.length())));
        } catch (NumberFormatException e) {
          // nop
        }
      } else if ("langname".equals(templateName)) {
        String langname = ISO639_3.sharedInstance.getLanguageNameInEnglish(parameterMap.get("1"));
        if (null != langname)
          writer.append(langname);
        else
          super.substituteTemplateCall(templateName, parameterMap, writer);
      } else {
        super.substituteTemplateCall(templateName, parameterMap, writer);
      }
    }
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("langname")) {
      // give a langname implementation without safesubst
      return "{{#invoke:languages/templates|getByCode|{{{1}}}|getCanonicalName}}";
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("Jpan-sortkey")) {
      // Jpan sortkey uses a hack that is not possible in our setting and generates a Lua error
      // As sortkey is not essential in our setting, just return a stub
      String rawContent = super.getRawWikiContent(parsedPagename, map);
      if (null == rawContent)
        return null;
      String patchedContent = rawContent.replaceAll(
          "tonumber\\(mw.getCurrentFrame\\(\\):extensionTag\\('nowiki', ''\\):match'\\(\\[%dA-F\\]\\+\\)', 16\\)",
          "0");
      if (logger.isDebugEnabled()) {
        boolean patched = !patchedContent.equals(rawContent);
        if (patched)
          logger.debug("Module:Jpan-sortkey has been patched.");
        else
          logger.warn("Module:Jpan-sortkey could not be patched ! Check current implementation.");
      }
      return patchedContent;
    }
    return super.getRawWikiContent(parsedPagename, map);
  }
}
