package org.getalp.dbnary.deu;

import static org.getalp.dbnary.deu.GermanInflectionData.Cas;
import static org.getalp.dbnary.deu.GermanInflectionData.GNumber;
import static org.getalp.dbnary.deu.GermanInflectionData.Genre;
import java.util.List;
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
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanSubstantiveDeklinationTableExtractor(wdh.currentLexEntry()));
  }

}
