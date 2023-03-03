package org.getalp.dbnary.languages.deu;

import java.util.Locale;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanInPageKonjugationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(getClass());

  public GermanInPageKonjugationExtractorWikiModel(IWiktionaryDataHandler wdh,
      WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanInPageKonjugationTableExtractor());
  }

}
