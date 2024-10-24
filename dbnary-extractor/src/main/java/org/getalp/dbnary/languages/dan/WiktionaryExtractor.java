package org.getalp.dbnary.languages.dan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {
  private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  private static final Set<String> ignoredTemplates = new HashSet<>();
  static {
    ignoredTemplates.add("(");
    ignoredTemplates.add(")");
    ignoredTemplates.add("-");
    ignoredTemplates.add("top");
    ignoredTemplates.add("midt");
    ignoredTemplates.add("bund");
  }

  private final WiktionaryDataHandler daWdh;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    daWdh = (WiktionaryDataHandler) wdh;
  }

  private List<Pair<Token, List<Token>>> split(List<Token> tokens, Predicate<Token> predicate) {
    List<Pair<Token, List<Token>>> splits = new ArrayList<>();
    List<Token> currentSplit = new ArrayList<>();
    for (Token t : tokens) {
      if (predicate.test(t)) {
        currentSplit = new ArrayList<>();
        splits.add(Pair.of(t, currentSplit));
      } else {
        currentSplit.add(t);
      }
    }
    return splits;
  }


  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText doc = new WikiText(getWiktionaryPageName(), pageContent);
    // Iterate over tokens and separate by languages
    List<Pair<Token, List<Token>>> languageData =
        split(doc.tokens(), t -> getLanguageCode(t) != null);

    for (Pair<Token, List<Token>> languageSection : languageData) {
      extractLanguageData(languageSection.getLeft(), languageSection.getRight());
    }
    wdh.finalizePageExtraction();
  }

  public String getLanguageCode(Token t) {
    if (t instanceof Template) {
      String name = t.asTemplate().getName();
      if (name.startsWith("=") && name.endsWith("=")) {
        return name.substring(1, name.length() - 1);
      } else if (name.startsWith("-") && name.endsWith("-") && name.length() > 2) {
        String potentialLanguageCode = name.substring(1, name.length() - 1);
        if (ISO639_3.sharedInstance.getLang(potentialLanguageCode) != null) {
          return potentialLanguageCode;
        }
      }
      return null;
    } else {
      return null;
    }
  }

  private void extractLanguageData(Token language, List<Token> value) {
    if (null == language)
      return;
    String lang = ISO639_3.sharedInstance.getTerm2Code(getLanguageCode(language));

    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(lang))
      return;

    wdh.initializeLanguageSection(lang);
    List<Pair<Token, List<Token>>> sections = split(value, this::isSectionHeader);
    for (Pair<Token, List<Token>> section : sections) {
      Token header = section.getLeft();
      if (header instanceof Template) {
        Template t = header.asTemplate();
        // remove the dashes before end after
        String name = t.getName().substring(1, t.getName().length() - 1);
        if (daWdh.isPartOfSpeech(name)) {
          wdh.initializeLexicalEntry(name);
          extractDefinitions(section.getRight());
        } else if (name.equals("exam")) {
          // TODO: extract examples
        } else if (name.equals("trans")) {
          extractTranslations(section.getRight());
        } else if (name.equals("etym")) {
          // TODO: extract etymology
        } else if (name.equals("pronun")) {
          // TODO: extract pronunciation
        } else if (name.equals("derv") || name.equals("rel") || name.equals("desc")) {
          // TODO: extract derivations/related terms
        } else if (name.equals("expr")) {
          // TODO: extract proverbs and expressions
        } else if (name.equals("ref") || name.equals("srce") || name.equals("also")
            || name.equals("anag")) {
          // TODO: ignore references/sources
        } else if (daWdh.isNym(name)) {
          extractNyms(name, section.getRight());
        } else if (name.equals("decl")) {
          // TODO: extract morphology
        } else {
          log.trace("Unexpected section template: {}", t);
        }
      } else {
        log.error("Unexpected non template token after section split: {}", header);
      }
    }
    wdh.finalizeLanguageSection();
  }

  private boolean isSectionHeader(Token t) {
    if (t instanceof Template) {
      Template tmpl = t.asTemplate();
      String name = tmpl.getName();
      return name.length() >= 2 && name.startsWith("-") && name.endsWith("-");
    }
    return false;
  }

  private void extractNyms(String name, List<Token> tokens) {
    for (Token t : tokens) {
      if (t instanceof Template) {
        if (ignoredTemplates.contains(t.asTemplate().getName())) {
          continue;
        }
        log.error("Unexpected template in Nym section: {}", t);
      } else if (t instanceof IndentedItem) {
        IndentedItem li = t.asIndentedItem();
        List<Token> values = li.getContent().tokens();
        extractNyms(name, values);
      } else if (t instanceof InternalLink) {
        wdh.registerNymRelation(t.asInternalLink().getLinkText(), name);
      } else if (t instanceof Text) {
        if (t.getText().replaceAll("\\s", "").isEmpty()) {
          continue;
        }
        log.debug("Unexpected text in nym section: {}", t);
      }
    }
  }

  private void extractDefinitions(List<Token> tokens) {
    for (Token t : tokens) {
      if (t instanceof NumberedListItem) {
        NumberedListItem li = t.asNumberedListItem();
        if (((NumberedListItem) t).getContent().getText().startsWith(":")) {
          // It is an example of the latest word sense.
          String example = li.getContent().getText().substring(1).trim();
          super.extractExample(example);
        } else {
          String definition = li.getContent().getText().trim();
          extractDefinition(definition, li.getLevel());
        }
      }
    }
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    // Render the definition to plain text using a wiktionary model
    String def = expander.expandAll(definition, null);
    if (!def.isEmpty()) {
      wdh.registerNewDefinition(def, defLevel);
    }
  }


  private void extractTranslations(List<Token> tokens) {
    for (Token t : tokens) {
      if (t instanceof Template) {
        Template template = t.asTemplate();
        if (ignoredTemplates.contains(template.getName())) {
          // ignore
        } else if (template.getName().equals("trad")) {
          Map<String, String> args = template.cloneParsedArgs();
          String lang = args.get("1");
          String translation = args.get("2");
          if (translation.startsWith("#")) {
            // it is a sektion marker, the translation should be the next arg
            translation = args.get("3");
            args.remove("3");
          }
          wdh.registerTranslation(lang, null, null, translation);
          args.remove("1");
          args.remove("2");
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in trad template: {}", args);
          }
        } else if (template.getName().equals("t")) {
          Map<String, String> args = template.cloneParsedArgs();
          String lang = args.get("1");
          String translation = args.get("2");
          wdh.registerTranslation(lang, null, null, translation);
          args.remove("1");
          args.remove("2");
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in t template: {}", args);
          }
        } else {
          log.error("Unexpected template in translation section: {}", template);
        }
      } else if (t instanceof IndentedItem) {
        IndentedItem li = t.asIndentedItem();
        List<Token> values = li.getContent().tokens();
        extractTranslations(values);
      } else if (t instanceof InternalLink) {
        // We have to get the language of the translation before registering it
        // wdh.registerTranslation(t.asInternalLink().getLinkText(), null, null, null);
      } else if (t instanceof Text) {
        if (t.getText().replaceAll("\\s", "").isEmpty()) {
          continue;
        }
        log.debug("Unexpected text in translation section: {}", t);
      }
    }
  }

}
