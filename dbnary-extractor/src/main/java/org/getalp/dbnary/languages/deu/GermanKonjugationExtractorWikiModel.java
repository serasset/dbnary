package org.getalp.dbnary.languages.deu;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanKonjugationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanKonjugationExtractorWikiModel.class);

  public GermanKonjugationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanKonjugationTableExtractor());
  }

  // Catch non German verb templates to avoid expanding them.
  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (templateName.contains("Niederländisch")) {
      log.debug("German Verb Conjugation Extraction: Ignoring template call: {}", templateName);
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

}
