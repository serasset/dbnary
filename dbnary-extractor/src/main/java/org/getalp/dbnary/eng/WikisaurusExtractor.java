package org.getalp.dbnary.eng;

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
    WikiText wtext = new WikiText(wikisaurusSection);

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
  }

  private String getGlossString(WikiText.WikiContent content) {
    StringBuilder gloss = new StringBuilder();
    log.debug("Wikisaurus gloss = {}", content.toString());
    for (WikiText.Token t : content.templatesOnUpperLevel()) {
      WikiText.Template tmpl = (WikiText.Template) t;
      if (((WikiText.Template) t).getName().equals("ws sense")) {
        gloss.append(((WikiText.Template) t).getParsedArgs().get("1"));
      }
    }
    return gloss.toString();
  }
}
