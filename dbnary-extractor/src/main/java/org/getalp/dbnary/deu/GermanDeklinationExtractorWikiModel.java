package org.getalp.dbnary.deu;

import java.util.HashSet;
import java.util.Locale;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanDeklinationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanDeklinationExtractorWikiModel.class);

  public GermanDeklinationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanDeklinationTableExtractor());
  }

}
