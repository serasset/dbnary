package org.getalp.dbnary.deu;

import static org.getalp.dbnary.deu.GermanInflectionData.GNumber;
import static org.getalp.dbnary.deu.GermanInflectionData.Mode;
import static org.getalp.dbnary.deu.GermanInflectionData.Person;
import static org.getalp.dbnary.deu.GermanInflectionData.Tense;
import static org.getalp.dbnary.deu.GermanInflectionData.Valency;
import static org.getalp.dbnary.deu.GermanInflectionData.Voice;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanKonjugationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanKonjugationExtractorWikiModel.class);

  public GermanKonjugationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh,
        new GermanKonjugationTableExtractor(wdh.currentLexEntry()));
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
