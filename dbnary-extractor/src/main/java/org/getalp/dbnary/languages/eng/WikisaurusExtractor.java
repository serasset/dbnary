package org.getalp.dbnary.languages.eng;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.iso639.ISO639_3.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikisaurusExtractor {

  private final Logger log = LoggerFactory.getLogger(WikisaurusExtractor.class);

  private final WiktionaryDataHandler wdh;

  public WikisaurusExtractor(WiktionaryDataHandler ewdh) {
    this.wdh = ewdh;
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
        WikiSection wikiSection = section.asWikiSection();
        String pos = wikiSection.getHeading().getContent().getText();
        if (WiktionaryDataHandler.isValidPOS(pos)) {
          extractWikisaurusPoSSection(l2, pos, wikiSection.getContent());
        } else {
          log.debug("Ignoring section {} (not a Part Of Speech) in Thesarurus:{}", pos,
              wdh.currentPagename());
        }
      } else {
        log.debug("Ignoring token {} (not a Part Of Speech Section) in Thesarurus:{}", section,
            wdh.currentPagename());
      }
    }

    wdh.finalizeLanguageSection();
  }

  private void extractWikisaurusPoSSection(String l2, String pos, WikiContent content) {
    for (Token section : content.wikiTokens()) {
      if (section instanceof WikiSection) {
        // Level 4 sections are supposed to be word senses
        WikiSection wikiSection = section.asWikiSection();
        String gloss = extractGloss(wikiSection.getHeading().getContent());
        extractWikisaurusRelations(l2, pos, gloss, wikiSection.getContent());
      }
    }
  }

  private void extractWikisaurusRelations(String l2, String pos, String gloss,
      WikiContent content) {
    for (Token section : content.wikiTokens()) {
      if (section instanceof WikiSection) {
        // Level 5 sections are nym relations
        WikiSection wikiSection = section.asWikiSection();
        String nym =
            EnglishGlobals.nymMarkerToNymName.get(wikiSection.getHeading().getContent().getText());
        extractWikisaurusLinks(l2, pos, gloss, nym, wikiSection.getContent());
      } else {
        log.debug("Unexpected token {} in Thesaurus:{}", section, wdh.currentPagename());
      }
    }
  }

  private void extractWikisaurusLinks(String l2, String pos, String gloss, String nym,
      WikiContent content) {
    if (null == nym) return;
    List<Pair<String, String>> targets = new ArrayList<>();
    for (Token tok : content.templates()) {
      Template tmpl = tok.asTemplate();
      String tmplName = tmpl.getName().trim();
      if (tmplName.equals("ws beginlist") || tmplName.equals("ws endlist")
          || tmplName.equals("ws ----")) {
        // ignore
      } else if (tmplName.equals("ws")) {
        String target = tmpl.getParsedArg("2");
        String targetGloss = tmpl.getParsedArg("3");
        targetGloss = Optional.ofNullable(targetGloss).map(String::trim).filter(s -> !s.isEmpty())
            .orElse(null);
        if (target != null) {
          target = target.trim();
          String lang = tmpl.getParsedArg("1");
          if (!Objects.equals(lang, l2)) {
            log.trace("THESAURUS: incorrect nym language (expected: {}, actual: {}) in {}",
                wdh.getCurrentEntryLanguage(), lang, wdh.currentPagename());
          } else {
            targets.add(Pair.of(target, targetGloss));
          }
        } else {
          log.trace("Empty target in ws template in Thesaurus:{}", wdh.currentPagename());
        }
      } else {
        log.debug("Unexpected template {} in Thesaurus:{}", tmpl, wdh.currentPagename());
      }
    }
    String source = wdh.currentPagename();
    for (Pair<String, String> p : targets) {
      String target = p.getLeft();
      String targetGloss = p.getRight();
      wdh.registerWikisaurusNymFromTo(pos, nym, gloss, targetGloss, source, target);
    }
  }

  private String extractGloss(WikiContent content) {
    StringBuilder gloss = new StringBuilder();
    for (Token tok : content.tokens()) {
      if (tok instanceof Text) {
        gloss.append(tok.getText());
      } else if (tok instanceof Template) {
        Template tmpl = tok.asTemplate();
        if (tmpl.getName().trim().equals("ws sense")) {
          String senseDesc = tmpl.getParsedArg("2");
          gloss.append(senseDesc);
        } else {
          log.debug("Unexpected Template {} in Thesaurus:{}", tok, wdh.currentPagename());
        }
      } else {
        log.debug("Unexpected token {} in Thesaurus:{}", tok, wdh.currentPagename());
      }
    }
    String ws = gloss.toString().trim();
    return ws.isEmpty() ? null : ws;
  }
}
