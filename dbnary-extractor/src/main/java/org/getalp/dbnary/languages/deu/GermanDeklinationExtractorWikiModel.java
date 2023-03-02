package org.getalp.dbnary.languages.deu;

import java.util.Locale;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanDeklinationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private final Logger log = LoggerFactory.getLogger(GermanDeklinationExtractorWikiModel.class);

  @Override
  protected InflectedFormSet parseTables(String declinationTemplateCall) {
    WikiText txt = new WikiText(getPageName(), declinationTemplateCall);
    InflectedFormSet forms = new InflectedFormSet();
    for (Token token : txt.headers()) {
      Heading head = token.asHeading();
      if (isAdjectiveSection(head)) {
        forms.addAll(super.parseTables(head.getSection().getText()));
      } else {
        log.trace("MORPH (Adjektiv): Ignoring section {}", head.getContent().getText());
      }
    }
    return forms;
  }

  private boolean isAdjectiveSection(Heading heading) {
    log.trace("MORPH: Header = {}", heading.getText());
    return heading.getContent().templates().stream()
        .anyMatch(tmpl -> tmpl.asTemplate().getName().equals("Adjektivdeklination")
            && tmpl.asTemplate().getParsedArg("1").equals("Deutsch"));
  }

  public GermanDeklinationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanDeklinationTableExtractor());
  }

}
