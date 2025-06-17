package org.getalp.dbnary.languages.ita;

import java.util.Locale;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;

public class ItalianExpandAllWikiModel extends ExpandAllWikiModel {

  public ItalianExpandAllWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public ItalianExpandAllWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

}
