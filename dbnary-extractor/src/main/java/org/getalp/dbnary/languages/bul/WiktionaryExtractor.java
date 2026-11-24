package org.getalp.dbnary.languages.bul;

import static org.getalp.dbnary.tools.TokenListSplitter.split;

import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.tuple.Pair;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  private BulgarianEntryExtractor entryExtractor;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    entryExtractor = new BulgarianEntryExtractor(this.wdh, this.wi, new Locale("bg"), "/${image}", "/${title}");
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    entryExtractor.setPageName(this.getWiktionaryPageName());
  }

  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    // Language sections are introduced by a {{-xx-}} template
    for (Pair<Token, List<Token>> section : split(page.tokens(), WiktionaryExtractor::isLanguageTemplate)) {
      if (section.getLeft() != null && "-bg-".equals(section.getLeft().asTemplate().getName().trim())) {
        extractBulgarianSection(section.getRight());
      }
    }
    wdh.finalizePageExtraction();
  }

  private static boolean isLanguageTemplate(Token t) {
    if (!(t instanceof Template)) {
      return false;
    }
    String name = t.asTemplate().getName().trim();
    return name.length() > 2 && name.startsWith("-") && name.endsWith("-");
  }

  private void extractBulgarianSection(List<Token> tokens) {
    wdh.initializeLanguageSection("bg");
    // The whole entry is a single POS template, other templates (Словоформи, Уикипедия...) are
    // ignored by the entry extractor.
    for (Token t : tokens) {
      if (t instanceof Template) {
        entryExtractor.extractEntry(t.asTemplate());
      }
    }
    if (log.isDebugEnabled()) {
      entryExtractor.displayUsedTemplates();
    }
    wdh.finalizeLanguageSection();
  }

}
