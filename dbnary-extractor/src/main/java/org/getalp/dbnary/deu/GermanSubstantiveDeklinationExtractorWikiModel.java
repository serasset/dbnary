package org.getalp.dbnary.deu;

import java.util.Locale;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanSubstantiveDeklinationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log =
      LoggerFactory.getLogger(GermanSubstantiveDeklinationExtractorWikiModel.class);

  public GermanSubstantiveDeklinationExtractorWikiModel(IWiktionaryDataHandler wdh,
      WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh,
        new GermanSubstantiveDeklinationTableExtractor());
  }

}
