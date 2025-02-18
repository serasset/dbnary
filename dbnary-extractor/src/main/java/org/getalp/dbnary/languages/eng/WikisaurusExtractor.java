package org.getalp.dbnary.languages.eng;

import java.util.Objects;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.wiki.WikiEventFilter.Action;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.iso639.ISO639_3.Lang;
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

  public void extractWikisaurusSection(Lang lg, WikiContent sectionContent) {
    String l2 = lg.getPart1();
    if (null == l2 || l2.trim().isEmpty()) {
      l2 = lg.getId();
    }
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(l2)) {
      return;
    }

    wdh.initializeLanguageSection(l2);

    for (Token section : sectionContent.wikiTokens()) {
      if (section instanceof WikiSection) {
        String pos = section.asWikiSection().getHeading().getContent().getText();

      }
    }
    String currentPOS = null;
    String currentWS = null;
    String currentNym = null;
    WikiEventsSequence headingOrTemplates =
        sectionContent.headers().or(tok -> (tok instanceof Template) ? Action.KEEP : Action.VOID);
    for (WikiText.Token tok : headingOrTemplates) {
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
            log.debug("Unknown nym : {} in {}", currentNym, wdh.currentPagename());
          } else {
            currentNym = simplifiedNym;
          }
        }
      } else if (tok instanceof WikiText.Template) {
        WikiText.Template template = (WikiText.Template) tok;
        if (template.getName().equals("ws")) {
          // TODO: do not register a relation between a (non existent page) and the target but
          // rather keep the set of targets as nyms in an ad-hoc model and try to resolve the
          // entries / wordwense at enhancement phase.
          String lang = template.getParsedArg("1");
          if (!Objects.equals(lang, wdh.getCurrentEntryLanguage())) {
            log.trace("THESAURUS: incorrect nym language (expected: {}, actual: {}) in {}",
                wdh.getCurrentEntryLanguage(), lang, wdh.currentPagename());
          } else {
            wdh.registerWikisaurusNym(currentPOS, currentWS, currentNym,
                template.getParsedArgs().get("2"));
          }
        }
      }
    }

    wdh.finalizeLanguageSection();

  }
}
