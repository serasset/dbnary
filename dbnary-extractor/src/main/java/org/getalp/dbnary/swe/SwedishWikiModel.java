package org.getalp.dbnary.swe;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class SwedishWikiModel extends DbnaryWikiModel {

  public SwedishWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public SwedishWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    String content = super.getRawWikiContent(parsedPagename, map);
    if (content != null) {
      content = content.replaceAll("\\{\\{\\{!}}", "{ {{!}}");
    }
    return content;
  }

}
