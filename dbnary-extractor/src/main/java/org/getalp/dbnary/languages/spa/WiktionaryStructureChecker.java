package org.getalp.dbnary.languages.spa;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.getalp.dbnary.api.IStructureChecker;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryStructureChecker implements IStructureChecker {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryStructureChecker.class);
  private String pagename;

  public WiktionaryStructureChecker(String language, String tdbDir) {
    assert ("spa".equals(language));
  }


  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {

  }

  @Override
  public void checkPage(String wiktionaryPageName, String pageContent) {
    pagename = wiktionaryPageName;
    WikiText text = new WikiText(wiktionaryPageName, pageContent);
    check(text);
  }

  private void check(WikiText text) {
    text.headers().forEach(h -> {
      if (h.asHeading().getContent().templates().stream().anyMatch(t -> {
        String name = t.asTemplate().getName();
        String lang = t.asTemplate().getParsedArg("1");
        return name.equals("lengua") && null != lang && "es".equalsIgnoreCase(lang.trim());
      })) {
        int lvl = h.asHeading().getLevel();
        if (lvl != 2) {
          log.error("Spanish section has level {} in {}", lvl, pagename);
        }
        checkSpanish(h.asHeading().getSection().getContent());
      }
    });
  }

  private Consumer<String> precedesForms(String section) {
    return s -> {
      if (hadFormSection)
        log.error("{} should precede Form Sections {} in {}", section, s, pagename);
    };
  }

  private boolean hadFormSection;
  private final Map<String, Consumer<String>> knownHeadingsStarts = new HashMap<>();
  {
    knownHeadingsStarts.put("Etimología", precedesForms("Etimología"));
    knownHeadingsStarts.put("Locuciones", precedesForms("Locuciones"));
    knownHeadingsStarts.put("Refranes", precedesForms("Refranes"));
    knownHeadingsStarts.put("Conjugación", precedesForms("Conjugación"));
    knownHeadingsStarts.put("Información adicional", s -> {
    });
    knownHeadingsStarts.put("Véase también", s -> {
    });
    knownHeadingsStarts.put("Traducciones", precedesForms("Traducciones"));
    knownHeadingsStarts.put("Referencias y notas", s -> {
    });
    knownHeadingsStarts.put("Forma", s -> hadFormSection = true);
  }

  private void checkSpanish(WikiContent content) {
    hadFormSection = false;
    content.headers().forEach(h -> {
      if (h.asHeading().getContent().wikiTokens().isEmpty()) {
        String headerText = h.asHeading().getContent().getText();
        for (Entry<String, Consumer<String>> e : knownHeadingsStarts.entrySet()) {
          if (headerText.startsWith(e.getKey())) {
            e.getValue().accept(headerText);
          }
        }
      } else if (h.asHeading().getContent().wikiTokens().size() > 1) {
        log.error("Heading {} has more than one template in {}", h.getText(), pagename);
      }
    });
  }
}
