package org.getalp.dbnary.languages.dan;

import static org.getalp.dbnary.tools.TokenListSplitter.split;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Item;
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
    ignoredTemplates.add("top3");
    ignoredTemplates.add("top4");
    ignoredTemplates.add("midt");
    ignoredTemplates.add("mid");
    ignoredTemplates.add("mid3");
    ignoredTemplates.add("mid4");
    ignoredTemplates.add("bund");
    ignoredTemplates.add("bottom");
    ignoredTemplates.add("trans-mid");
  }

  private final WiktionaryDataHandler daWdh;
  private final static Set<String> knownSections = new HashSet<>();
  static {
    // These legimate known sections clash with language codes and should not be interpreted as
    // languages
    knownSections.add("ant");
    knownSections.add("abr");
    knownSections.add("adj");
    knownSections.add("adv");
    knownSections.add("afl");
    knownSections.add("alt");
    knownSections.add("art");
    knownSections.add("end");
    knownSections.add("lyd");
    knownSections.add("num");
    knownSections.add("phr");
    knownSections.add("ref");
    knownSections.add("rel");
    knownSections.add("syn");

  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    daWdh = (WiktionaryDataHandler) wdh;
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
        if (name.equals("=sprog=")) {
          // TODO: extract language name from Danish value
          return "unknown";
        }
        return name.substring(1, name.length() - 1);
      } else if (name.startsWith("-") && name.endsWith("-") && name.length() > 2) {
        String potentialLanguageCode = name.substring(1, name.length() - 1);
        if (knownSections.contains(potentialLanguageCode)) {
          return null;
        }
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

    if (null == lang)
      return;

    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(lang))
      return;

    wdh.initializeLanguageSection(lang);
    extractLanguageSections(value);
    wdh.finalizeLanguageSection();
  }

  private void extractLanguageSections(List<Token> value) {
    List<Pair<Token, List<Token>>> sections = split(value, this::isSectionHeader);
    for (Pair<Token, List<Token>> section : sections) {
      Token header = section.getLeft();
      if (header instanceof Template || header instanceof Heading) {
        String name;
        if (header instanceof Heading) {
          name = header.asHeading().getContent().getText().trim();
        } else {
          Template t = header.asTemplate();
          // remove the dashes before and after
          name = t.getName().substring(1, t.getName().length() - 1);
          if ("expr".equals(name) && t.getArg("1") != null)
            name = "Udtryk";
        }
        if (daWdh.isPartOfSpeech(name)) {
          wdh.initializeLexicalEntry(name);
          extractDefinitions(section.getRight());
        } else if (name.equals("exam")) {
          // TODO: extract examples
        } else if (name.equals("trans")) {
          extractTranslations(section.getRight());
        } else if (name.equals("etym") || name.startsWith("Etymologi")) {
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
          log.trace("Unexpected section name: {}", name);
        }
      } else {
        log.error("Unexpected non Template/Heading token after section split: {}", header);
      }
    }
  }

  private boolean isSectionHeader(Token t) {
    if (t instanceof Template) {
      Template tmpl = t.asTemplate();
      String name = tmpl.getName();
      return name.length() >= 2 && name.startsWith("-") && name.endsWith("-");
    } else if (t instanceof Heading) {
      return t.asHeading().getLevel() <= 3;
    }
    return false;
  }

  private void extractNyms(String name, List<Token> tokens) {
    for (Token t : tokens) {
      if (t instanceof Template) {
        if (ignoredTemplates.contains(t.asTemplate().getName())) {
          // ignore
        } else if (t.asTemplate().getName().equals("l")) {
          Map<String, String> args = t.asTemplate().cloneParsedArgs();
          String nym = args.get("2");
          wdh.registerNymRelation(nym, name);
          args.remove("1");
          args.remove("2");
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in l template: {}", args);
          }
        } else {
          log.error("Unexpected template in Nym section: {}", t);
        }
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
  public Resource extractDefinition(String definition, int defLevel) {
    // Render the definition to plain text using a wiktionary model
    String def = expander.expandAll(definition, null);
    if (!def.isEmpty()) {
      wdh.registerNewDefinition(def, defLevel);
    }
    return null;
  }

  private void extractTranslations(List<Token> tokens) {
    Resource currentGloss = null;
    extractTranslations(tokens, currentGloss);
  }

  private void extractTranslations(List<Token> tokens, Resource currentGloss) {
    for (Token t : tokens) {
      if (t instanceof Template) {
        Template template = t.asTemplate();
        if (template.getName().equals("trad")) {
          Map<String, String> args = template.cloneParsedArgs();
          String lang = args.get("1");
          String translation = args.get("2");
          if (translation.startsWith("#")) {
            // it is a sektion marker, the translation should be the next arg
            translation = args.get("3");
            args.remove("3");
          }
          if (null != lang && null != translation)
            wdh.registerTranslation(lang, currentGloss, null, translation);
          args.remove("1");
          args.remove("2");
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in trad template: {}", args);
          }
        } else if (template.getName().equals("t") || template.getName().equals("O")
            || template.getName().equals("t+")) {
          Map<String, String> args = template.cloneParsedArgs();
          String lang = args.get("1");
          String translation = args.get("2");
          if (null != lang && null != translation)
            wdh.registerTranslation(lang, currentGloss, null, translation);
          args.remove("1");
          args.remove("2");
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in t template: {}", args);
          }
        } else if (template.getName().equals("trans-top")) {
          String g = template.getParsedArg("1");
          if (null != g) {
            currentGloss = wdh.createGlossResource(new StructuredGloss(null, g));
          }
        } else if (template.getName().equals("trans-bottom") || template.getName().equals(")")
            || template.getName().equals("bottom")) {
          currentGloss = null;
        } else if (ignoredTemplates.contains(template.getName())) {
          // ignore
        } else {
          log.debug("Unexpected template in translation section: {}", template);
        }
      } else if (t instanceof Item) {
        Item glossItem = t.asItem();
        String g = glossItem.getContent().getText();
        if (null != g) {
          g = g.trim();
          currentGloss = wdh.createGlossResource(new StructuredGloss(null, g));
        } else {
          currentGloss = null;
        }
      } else if (t instanceof IndentedItem) {
        IndentedItem li = t.asIndentedItem();
        List<Token> values = li.getContent().tokens();
        extractTranslations(values, currentGloss);
      } else if (t instanceof InternalLink) {
        // We have to get the language of the translation before registering it
        // wdh.registerTranslation(t.asInternalLink().getLinkText(), null, null, null);
      } else if (t instanceof Text) {
        if (t.getText().replaceAll("[\\s:]+", "").isEmpty()) {
          continue;
        }
        log.debug("Unexpected text in translation section: {}", t);
      }
    }
  }

}
