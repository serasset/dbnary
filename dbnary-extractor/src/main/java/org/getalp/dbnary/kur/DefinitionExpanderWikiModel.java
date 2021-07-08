package org.getalp.dbnary.kur;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;

public class DefinitionExpanderWikiModel extends ExpandAllWikiModel {

  public DefinitionExpanderWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public DefinitionExpanderWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL,
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

}
