package org.getalp.dbnary.languages.bul;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.text.StringEscapeUtils;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts a Bulgarian lexical entry from its POS template. On bg.wiktionary, the whole entry is a
 * single template (e.g. {{Съществително нарицателно име|ЗНАЧЕНИЕ=...|ПРЕВОД=...}}) whose named
 * arguments are the entry sections.
 */
public class BulgarianEntryExtractor {

  private final Logger log = LoggerFactory.getLogger(BulgarianEntryExtractor.class);

  protected final static Set<String> bulgarianPOS = new TreeSet<>();
  protected final static HashMap<String, String> nymMarkerToNymName;

  static {

    bulgarianPOS.add("Съществително нарицателно име"); // Common Noun
    bulgarianPOS.add("Съществително собствено име"); // Proper Noun
    bulgarianPOS.add("Прилагателно име"); // Adjective
    bulgarianPOS.add("Глагол"); // Verb
    bulgarianPOS.add("Наречие"); // Adverb
    bulgarianPOS.add("Частица"); // Particle
    bulgarianPOS.add("Числително име"); // Ordinal
    bulgarianPOS.add("Предлог"); // Preposition
    bulgarianPOS.add("междуметие"); // Interjection
    bulgarianPOS.add("съюз"); // Conjunction
  }

  static {

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("Синоними", "syn"); //
    nymMarkerToNymName.put("Антоними", "ant"); //

    nymMarkerToNymName.put("Хипоними", "hypo");
    nymMarkerToNymName.put("Хипероними", "hyper");
    nymMarkerToNymName.put("Мероним", "mero");
    nymMarkerToNymName.put("Холоним", "holo");
  }

  private final IWiktionaryDataHandler delegate;
  private final DefinitionsWikiModel definitionExpander;
  Set<String> templates = null;

  public BulgarianEntryExtractor(IWiktionaryDataHandler wdh, WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    this.delegate = wdh;
    if (log.isDebugEnabled()) {
      templates = new HashSet<>();
    }
    this.definitionExpander = new DefinitionsWikiModel(wi, locale, imageBaseURL, linkBaseURL, templates);
  }

  public void setPageName(String pageTitle) {
    this.definitionExpander.setPageName(pageTitle);
  }

  /**
   * Extract the lexical entry described by the given template.
   *
   * @param entry a top level template of the Bulgarian section
   * @return true iff the template is a known POS template
   */
  public boolean extractEntry(Template entry) {
    String pos = getPOS(entry.getName().trim());
    if (null == pos) {
      return false;
    }
    delegate.initializeLexicalEntry(pos);

    for (Map.Entry<String, WikiContent> arg : entry.getArgs().entrySet()) {
      String section = arg.getKey();
      WikiContent value = arg.getValue();
      if (section.contains("ЗНАЧЕНИЕ")) {
        extractDefinitions(value);
      } else if (section.contains("ПРЕВОД")) {
        String sectionContent = value.getText().replaceAll("\\[\\[:[^:]*:[^\\|]*\\|\\s*\\(?[^\\)\\]]*\\)?\\s*\\]\\]", "");
        sectionContent = sectionContent.replaceAll("\\[\\[\\s*\\]\\]", "");
        // decode html entities (at&nbsp;{{п|en|home}}), the parser does not consider nbsp as a space
        sectionContent = StringEscapeUtils.unescapeHtml4(sectionContent).replace('\u00A0', ' ');
        // TODO: use a shared instance of the translation parser.
        TranslationsParser tp = new TranslationsParser();
        tp.extractTranslations(sectionContent, delegate);
      } else if (section.contains("ID")) { // ID, same as page name for Bulgarian
      } else if (section.contains("РОД")) { // Gender
      } else if (section.contains("ТИП")) { // Type
      } else if (section.contains("ИЗРАЗИ")) { // Examples
      } else if (section.contains("ЕТИМОЛОГИЯ")) { // Etymology
      } else if (section.contains("ПРОИЗВОДНИ ДУМИ")) { // Derived Terms
      } else if (section.contains("ДРУГИ")) { // Related Words
      } else {
        for (String rt : nymMarkerToNymName.keySet()) {
          if (section.toLowerCase().contains(rt.toLowerCase())) {
            extractNyms(value, nymMarkerToNymName.get(rt));
          }
        }
      }
    }
    return true;
  }

  private void extractNyms(WikiContent value, String nymType) {
    // links may be inside list items
    for (Token t : value.filteredTokens(new ClassBasedFilter().allowInternalLink().enterIndentedItem())) {
      String target = t.asInternalLink().getTargetText().trim();
      if (!target.isEmpty()) {
        delegate.registerNymRelation(target, nymType);
      }
    }
  }

  private void extractDefinitions(WikiContent defSection) {
    for (Token t : defSection.filteredTokens(new ClassBasedFilter().allowNumberedListItem())) {
      IndentedItem item = t.asIndentedItem();
      // only definitions (# and ##), not examples or quotations (#:, #*)
      if (!item.getListPrefix().matches("#{1,2}")) {
        continue;
      }
      String def = definitionExpander.expandAll(item.getContent().getText());
      if (null == def || def.isEmpty() || def.startsWith("Значението на думата все още не е въведено")) {
        continue;
      }
      delegate.registerNewDefinition(def);
    }
  }

  private String getPOS(String templateName) {
    for (String p : bulgarianPOS) {
      if (templateName.startsWith(p)) {
        return p;
      }
    }
    return null;
  }

  public void displayUsedTemplates() {
    if (templates != null && templates.size() > 0) {
      log.debug("in {}: template used : {}", this.delegate.currentPagename(), templates.toString());
    }
  }
}
