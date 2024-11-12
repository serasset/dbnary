package org.getalp.dbnary.languages.swe;

import static org.getalp.dbnary.tools.TokenListSplitter.split;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author malick
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


  static Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);


  protected SwedishMorphologyExtractor morphologyExtractor;
  protected ExampleExpanderWikiModel exampleExtractor;
  protected DefinitionExpanderWikiModel definitionExpander;

  private static Set<String> ignoredTranslationTemplates = new HashSet<>();


  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    morphologyExtractor = new SwedishMorphologyExtractor(wi, wdh);
    exampleExtractor = new ExampleExpanderWikiModel(wi, (WiktionaryDataHandler) wdh);
    definitionExpander = new DefinitionExpanderWikiModel(wi);
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    exampleExtractor.setPageName(this.getWiktionaryPageName());
    definitionExpander.setPageName(this.getWiktionaryPageName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData()
   */
  @Override
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
    if (t instanceof Heading && t.asHeading().getLevel() == 2) {
      String name = t.asHeading().getContent().getText().trim();
      return SwedishLanguageNames.getLanguageCode(name);
    } else {
      return null;
    }
  }

  protected void extractLanguageData(Token language, List<Token> value) {
    if (null == language)
      return;
    String lang = ISO639_3.sharedInstance.getTerm2Code(getLanguageCode(language));

    if (null == lang) {
      log.trace("Ignoring language section {} in {}", language, wdh.currentPagename());
      return;
    }

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
      if (header instanceof Heading) {
        String name = header.asHeading().getContent().getText().trim();
        if (WiktionaryDataHandler.isValidPOS(name)) {
          wdh.initializeLexicalEntry(name);
          extractDefinitions(section.getRight());
          extractMorphology(section.getRight());
          extractPronunciation(section.getRight());
        } else if (name.equals("exam")) {
          // TODO: extract examples
        } else if (name.equals("Översättningar")) {
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
    if (t instanceof Heading) {
      return t.asHeading().getLevel() >= 3;
    }
    return false;
  }

  private void extractMorphology(List<Token> tokens) {

    tokens.stream().filter(t -> t instanceof Template).map(Token::asTemplate)
        .filter(WiktionaryExtractor::isMorphologyTableTemplate).forEach(tmpl -> morphologyExtractor
            .extractMorphologicalData(tmpl.toString(), getWiktionaryPageName()));
  }

  private static boolean isMorphologyTableTemplate(Template template) {
    String name = template.getName();
    return name.startsWith("sv-subst") || name.startsWith("sv-adj") || name.startsWith(("sv-verb"))
        || name.startsWith("sv-adv") || name.startsWith("sv-artikel") || name.startsWith("sv-pron");
  }

  private void extractTranslations(List<Token> tokens) {
    extractTranslations(tokens, null);
  }

  private void extractTranslations(List<Token> tokens, Resource currentGloss) {
    for (Token t : tokens) {
      if (t instanceof Template) {
        Template template = t.asTemplate();
        if (template.getName().equals("ö") || template.getName().equals("ö+")) {
          Map<String, String> args = template.cloneParsedArgs();
          String lang = args.get("1");
          String translation = args.get("2");
          String gram1 = args.get("3");
          String gram2 = args.get("4");
          String tr = args.get("tr");
          if (null != tr) {
            tr += "tr=" + tr; // transliteration
          }
          String usage = Stream.of(gram1, gram2, tr).filter(s -> s != null && !s.isEmpty())
              .collect(Collectors.joining("|"));
          if (null != lang && null != translation)
            wdh.registerTranslation(lang, currentGloss, usage, translation);
          args.remove("1");
          args.remove("2");
          args.remove("3");
          args.remove("4");
          args.remove("text"); // just ignore the text element
          args.remove("tr");
          args.remove("iw"); // just ignore the iw element
          if (!args.isEmpty()) {
            log.debug("Unexpected arguments in t template: {}", args);
          }
        } else if (template.getName().equals("ö-topp")) {
          String g = template.getParsedArg("1");
          if (null != g) {
            currentGloss = wdh.createGlossResource(new StructuredGloss(null, g));
          }
        } else if (template.getName().equals("ö-botten") || template.getName().equals(")")
            || template.getName().equals("bottom")) {
          currentGloss = null;
        } else if (ignoredTranslationTemplates.contains(template.getName())) {
          // ignore
        } else {
          log.debug("Unexpected template in translation section: {}", template);
        }
      } else if (t instanceof IndentedItem) {
        IndentedItem li = t.asIndentedItem();
        List<Token> values = li.getContent().tokens();
        extractTranslations(values, currentGloss);
      } else if (t instanceof InternalLink) {
        // We have to get the language of the translation before registering it
        // wdh.registerTranslation(t.asInternalLink().getLinkText(), null, null, null);
        log.debug("Unhandled internal link in translation section: {} -- {}", t,
            this.getWiktionaryPageName());
      } else if (t instanceof Text) {
        if (t.getText().replaceAll("[\\s:]+", "").isEmpty()) {
          continue;
        }
        log.debug("Unexpected text in translation section: {} -- {}", t,
            this.getWiktionaryPageName());
      } else {
        log.trace("Unexpected token in translation section: {} -- {}", t,
            this.getWiktionaryPageName());
      }
    }
  }


  private void extractDefinitions(List<Token> tokens) {
    Resource target = null;
    for (Token t : tokens) {
      if (t instanceof NumberedListItem) {
        WikiContent content = t.asNumberedListItem().getContent();
        String ex = content.getText();
        if (ex.startsWith(":")) {
          if (content.templates().stream()
              .anyMatch(tmpl -> tmpl.asTemplate().getName().equals("avgränsare"))) {
            // All remaining information will be attached to the lexical entry rather than the last
            // sense
            target = null;
            continue;
          }
          // If there is 2 colon, it should be the translation of the previous example
          if (ex.startsWith("::")) {
            log.debug("Unhandled translation of an example in definition section: {} -- {}", ex,
                this.getWiktionaryPageName());
          } else {
            // It is an example or information line of the target.
            exampleExtractor.processDefinitionLine(ex.substring(1).trim(), target);
          }
        } else {
          String definition = ex.trim();
          target = extractDefinition(definition, t.asNumberedListItem().getLevel());
        }
      } else if (t instanceof IndentedItem) {
        WikiContent content = t.asIndentedItem().getContent();
        String ex = content.getText();
        if (content.templates().stream()
            .anyMatch(tmpl -> tmpl.asTemplate().getName().equals("avgränsare"))) {
          // All remaining information will be attached to the lexical entry rather than the last
          // sense
          target = null;
          continue;
        }
        exampleExtractor.processDefinitionLine(ex, target);
      }
    }
  }

  @Override
  public Resource extractDefinition(String definition, int defLevel) {
    String expandedDefinition = definitionExpander.expandDefinition(definition);
    return super.extractDefinition(expandedDefinition, defLevel);
  }

  private void extractPronunciation(List<Token> tokens) {
    tokens.stream().filter(t -> t instanceof ListItem)
        .flatMap(t -> t.asListItem().getContent().templates().stream())
        .filter(t -> t instanceof Template).filter(t -> t.asTemplate().getName().equals("uttal"))
        .map(t -> t.asTemplate().getParsedArgs().get("ipa")).forEach(
            pron -> wdh.registerPronunciation(pron, wdh.getCurrentEntryLanguage() + "-fonipa"));
    // TODO: a few pronunciations are computed using the ipa template (in esperanto and finish)
  }
}
