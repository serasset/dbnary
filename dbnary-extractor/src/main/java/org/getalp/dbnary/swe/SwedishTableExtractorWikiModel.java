package org.getalp.dbnary.swe;

import java.util.Locale;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwedishTableExtractorWikiModel extends SwedishWikiModel {

  private Logger log = LoggerFactory.getLogger(SwedishTableExtractorWikiModel.class);

  public SwedishTableExtractorWikiModel(WiktionaryIndex wi, String imageBaseURL,
      String linkBaseURL) {
    super(wi, new Locale("sv"), imageBaseURL, linkBaseURL);
  }

  protected InflectedFormSet parseTables(String declinationTemplateCall) {

    if (log.isDebugEnabled()) {
      WikiText txt = new WikiText(declinationTemplateCall);
      for (Token token : txt.templatesOnUpperLevel()) {
        Template tmpl = (Template) token;
        // if (!ignoredTemplates.contains(tmpl.getName())) {
        log.debug("MORPH template: {}", tmpl.getName());

        String tmplSource = tmpl.toString().replaceAll("\\n", "");
        log.debug("MORPH template call:  {} @ {}", tmplSource, getPageName());
        // }
      }
    }

    SwedishTableExtractor tableExtractor = new SwedishTableExtractor(this.getPageName());
    String htmlCode = expandWikiCode(declinationTemplateCall);
    log.trace(htmlCode);
    return tableExtractor.parseHTML(htmlCode);
  }
}
