package org.getalp.dbnary.languages.eng;

import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikisaurusExtractor {

  private Logger log = LoggerFactory.getLogger(WikisaurusExtractor.class);

  private final WiktionaryDataHandler wdh;

  public WikisaurusExtractor(WiktionaryDataHandler ewdh) {
    this.wdh = ewdh;
  }


  public void extractWikisaurusSection(String wiktionaryPageName, String wikisaurusSection) {
    wdh.initializeLanguageSection("en");
    WikiText wtext = new WikiText(wiktionaryPageName, wikisaurusSection);

    String currentPOS = null;
    String currentWS = null;
    String currentNym = null;
    for (WikiText.Token tok : wtext.wikiTokens()) {
      if (tok instanceof WikiText.Heading) {
        WikiText.Heading h = (WikiText.Heading) tok;
        if (h.getLevel() == 3) {
          currentPOS = h.getContent().toString().trim();
          currentWS = null;
          currentNym = null;
        } else if (h.getLevel() == 4) {
          // TODO: in Wikisaurus:theosophist the nym is given in a level 4 heading
          String nym = h.getContent().toString();
          if (null != (nym = EnglishGlobals.nymMarkerToNymName.get(nym))) {
            currentNym = nym;
          } else {
            currentWS = getGlossString(h.getContent());
            currentNym = null;
          }
        } else if (h.getLevel() == 5) {
          currentNym = h.getContent().toString();
          String simplifiedNym = EnglishGlobals.nymMarkerToNymName.get(currentNym);
          if (null == simplifiedNym) {
            log.debug("Unknown nym : {} in {}", currentNym, wiktionaryPageName);
          } else {
            currentNym = simplifiedNym;
          }
        }
      } else if (tok instanceof WikiText.Template) {
        WikiText.Template template = (WikiText.Template) tok;
        if (template.getName().equals("ws")) {
          wdh.registerWikisaurusNym(currentPOS, currentWS, currentNym,
              template.getParsedArgs().get("1"));
        }
      }
    }
    wdh.finalizeLanguageSection();
  }

  private String getGlossString(WikiText.WikiContent content) {
    StringBuilder gloss = new StringBuilder();
    log.debug("Wikisaurus gloss = {}", content.toString());
    for (WikiText.Token t : content.templatesOnUpperLevel()) {
      WikiText.Template tmpl = (WikiText.Template) t;
      String tmplName = tmpl.getName();
      if (tmplName.equals("ws sense") || tmplName.equals("wse-sense")) {
        gloss.append(tmpl.getParsedArgs().get("2"));
      } else {
        log.trace("Unknown template in gloss : {}", tmplName);
      }
    }
    return gloss.toString();
  }
}
