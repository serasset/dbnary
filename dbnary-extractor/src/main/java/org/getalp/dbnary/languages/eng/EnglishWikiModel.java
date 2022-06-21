package org.getalp.dbnary.languages.eng;

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
    if (templateName.equals("check deprecated lang param usage")) {
      writer.append(parameterMap.get("1"));
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }
}
