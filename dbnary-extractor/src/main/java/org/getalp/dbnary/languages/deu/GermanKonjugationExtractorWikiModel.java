package org.getalp.dbnary.languages.deu;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanKonjugationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanKonjugationExtractorWikiModel.class);

  public GermanKonjugationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh, new GermanKonjugationTableExtractor());
  }

  @Override
  protected InflectedFormSet parseTables(String declinationTemplateCall) {
    WikiText txt = new WikiText(getPageName(), declinationTemplateCall);
    InflectedFormSet forms = new InflectedFormSet();
    for (Token token : txt.headers()) {
      Heading head = token.asHeading();
      if (isVerbalSection(head)) {
        forms.addAll(super.parseTables(head.getSection().getText()));
      } else {
        log.trace("MORPH (Verb): Ignoring section {}", head.getContent().getText());
      }
    }
    return forms;
  }

  private boolean isVerbalSection(Heading heading) {
    log.trace("MORPH: Header = {}", heading.getText());
    return heading.getContent().templates().stream()
        .anyMatch(tmpl -> tmpl.asTemplate().getName().equals("Verbkonjugation")
            && tmpl.asTemplate().getParsedArg("1").equals("Deutsch"));
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
