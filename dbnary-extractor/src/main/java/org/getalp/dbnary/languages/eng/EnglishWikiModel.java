package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

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

        }
      } else {
        super.substituteTemplateCall(templateName, parameterMap, writer);
      }
    }
  }
}
