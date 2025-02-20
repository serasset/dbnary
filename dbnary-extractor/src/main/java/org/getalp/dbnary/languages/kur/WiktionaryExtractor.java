package org.getalp.dbnary.languages.kur;

import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiEventFilter.Action;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.*;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ayouba Deba
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString =
      "^={2}\\s*\\{\\{([^=]+)\\}\\}\\s*={2}";

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected final static Pattern languageSectionPattern;
  protected final static HashSet<String> partOfSpeechMarkers;
  private DefinitionExpanderWikiModel definitionExpander;
  private ExpandAllWikiModel pronunciationExpander;

  static {

    languageSectionPattern = Pattern.compile(languageSectionPatternString, Pattern.MULTILINE);

    partOfSpeechMarkers = new HashSet<String>(40);

    partOfSpeechMarkers.add("Navdêr");
    partOfSpeechMarkers.add("Serenav");
    partOfSpeechMarkers.add("Lêker");
    partOfSpeechMarkers.add("Hoker");
    partOfSpeechMarkers.add("Cînav");
    partOfSpeechMarkers.add("Baneşan");
    partOfSpeechMarkers.add("Daçek");
    partOfSpeechMarkers.add("Pêşdaçek");
    partOfSpeechMarkers.add("Paşdaçek");
    partOfSpeechMarkers.add("Bazinedaçek");
    partOfSpeechMarkers.add("Girêdek");
    partOfSpeechMarkers.add("Artîkel");
    partOfSpeechMarkers.add("Navgir");
    partOfSpeechMarkers.add("Paşgir");
    partOfSpeechMarkers.add("Pêşgir");
    // partOfSpeechMarkers.add("Reh");
    // partOfSpeechMarkers.add("Rehekî lêkerê");
    partOfSpeechMarkers.add("Biwêj");
    partOfSpeechMarkers.add("Hevok");
    partOfSpeechMarkers.add("Gotineke pêşiyan");
    partOfSpeechMarkers.add("Hejmar");
    partOfSpeechMarkers.add("Tîp");
    partOfSpeechMarkers.add("Sembol");
    partOfSpeechMarkers.add("Kurtenav");
    partOfSpeechMarkers.add("Rengdêr"); // adjectif
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExpander =
        new DefinitionExpanderWikiModel(wi, Locale.forLanguageTag("ku"), "/images", "/link");
    pronunciationExpander =
        new ExpandAllWikiModel(wi, Locale.forLanguageTag("ku"), "/images", "/link");

  }

  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    definitionExpander.setPageName(getWiktionaryPageName());
    pronunciationExpander.setPageName(getWiktionaryPageName());

    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    page.headers(2).stream().map(Token::asHeading).forEach(h -> {
      WikiSection section2 = h.getSection();
      String lang = getHeaderLanguage(h);
      if (lang != null) {
        extractLanguageData(lang, section2.getContent());
      }
    });

    // Matcher languageFilter = languageSectionPattern.matcher(pageContent);
    // while (languageFilter.find() && !languageFilter.group(1).equals("ziman|ku")) {
    // }
    // // Either the filter is at end of sequence or on German language header.
    // if (languageFilter.hitEnd()) {
    // // There is no Turkish data in this page.
    // return;
    // }
    // int kurdeSectionStartOffset = languageFilter.end();
    // // Advance till end of sequence or new language section
    // // WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) -
    // // languageFilter.start()) != 2) {
    // languageFilter.find();
    // // languageFilter.find();
    // int kurdeSectionEndOffset =
    // languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
    //
    // extractKurdishData(kurdeSectionStartOffset, kurdeSectionEndOffset);
    wdh.finalizePageExtraction();
  }

  private void extractLanguageData(String lang, WikiContent content) {
    String l2 = LangTools.getShortCode(lang);
    if (l2 == null)
      return;
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(l2)) {
      return;
    }

    wdh.initializeLanguageSection(l2);
    for (Token evt : content.headers(3)) {
      WikiSection section = evt.asHeading().getSection();
      String header = section.getHeading().getContent().toString().trim();

      if (header.equals("Bilêvkirin")) {
        extractPron(section.getContent());
      } else if (header.equals("Wate")) {
        // to do (signification)
      } else if (header.startsWith("Formeke")) {
        // ignore
      } else if (partOfSpeechMarkers.contains(header)) {
        // Part of speech
        wdh.initializeLexicalEntry(header);
        // Extract definitions
        extractDefinitions(section.getContent());
        section.getContent().wikiTokens().stream()
            .filter(wikiToken -> wikiToken instanceof WikiSection).map(Token::asWikiSection)
            .forEach(ws -> {
              String h4 = ws.getHeading().getContent().toString().trim().toLowerCase();
              if ("werger".equals(h4)) {
                extractTranslations(ws.getContent());
              } else if (WiktionaryDataHandler.nymMap.containsKey(h4)) {
                extractNymSection(WiktionaryDataHandler.nymMap.get(h4), ws.getContent());
              } else {
                log.debug("Unhandled sub section {} in {}", h4, getWiktionaryPageName());
              }
            });
      } else {
        log.debug("Unexpected header {} in {}", header, getWiktionaryPageName());
      }
    }
    wdh.finalizeLanguageSection();
  }

  private String getHeaderLanguage(Heading h) {
    Optional<Token> langTmpl = h.getContent().templates().stream()
        .filter(t -> t.asTemplate().getName().equals("ziman")).findFirst();
    return langTmpl.isPresent() ? langTmpl.get().asTemplate().getParsedArg("1") : null;
  }

  private void extractNymSection(String nym, WikiContent content) {
    WikiEventsSequence templateOrLinks =
        content.templates().or(tok -> (tok instanceof InternalLink) ? Action.KEEP : Action.VOID);
    for (Token tok : templateOrLinks) {
      if (tok instanceof Template) {
        Template t = tok.asTemplate();
        if ("g".equals(t.getName())) {
          // link template
          if (wdh.getCurrentEntryLanguage().equals(t.getParsedArg("1"))) {
            wdh.registerNymRelation(t.getParsedArg("2"), nym);
          } else {
            log.debug("Link Template to another language in a nym relation {} in {}", t.getText(),
                wdh.currentPagename());
          }
        } else if (t.getName().startsWith("kol") || "pêk".equals(t.getName())) {
          int valueIndex = 2;
          String lang = t.getParsedArg("1").trim();
          if (!lang.equals(wdh.getCurrentEntryLanguage())) {
            log.debug("Link Template to another language in a nym relation {} in {}", t.getText(),
                wdh.currentPagename());
            continue;
          }
          String value;
          while (null != (value = t.getParsedArg(String.valueOf(valueIndex)))) {
            wdh.registerNymRelation(value.trim(), nym);
            valueIndex++;
          }
        } else if ("stûn".equals(t.getName())) {
          extractNymSection(nym, t.getArg("1"));
        } else {
          log.trace("NYM: Ignoring template {} in nym section in {}", t.getText(),
              wdh.currentPagename());
        }
      } else if (tok instanceof InternalLink) {
        wdh.registerNymRelation(tok.asInternalLink().getTargetText(), nym);
      }
    }
  }

  protected void extractDefinitions(WikiContent wk) {
    WikiEventsSequence indentationsOrTemplates =
        wk.filteredTokens(new ClassBasedFilter().allowIndentedItem().allowTemplates());
    for (Token indent : indentationsOrTemplates) {
      if (isAnExample(indent)) {
        String nli = indent.asIndentedItem().getContent().getText();
        String expandedExample = definitionExpander.expandAll(nli, null);
        wdh.registerExample(expandedExample.replace("\n", ""), null);
      } else if (indent instanceof NumberedListItem) {
        // It's a definition
        NumberedListItem nli = indent.asNumberedListItem();
        String expandedDefinition =
            definitionExpander.expandAll(nli.getContent().getText().trim(), null);
        wdh.registerNewDefinition(expandedDefinition.replace("\n", ""));
      } else if (indent instanceof Template) {
        String tname = indent.asTemplate().getName();
        log.debug("In Def[{}] - got template {}", getWiktionaryPageName(), tname);
      } else {
        // TODO: test and handle these !
        log.debug("Unhandled indented item in def[{}]: {}", getWiktionaryPageName(),
            indent.toString());
      }
    }
  }

  /**
   * States if an indented item in the definition sections is an exemple or not
   * 
   * @param indent an indented item in the definition section
   * @return true if the indented item is an example specification
   */
  private boolean isAnExample(Token indent) {
    String content;
    if (indent instanceof Indentation) {
      return true;
    }
    if (indent instanceof NumberedListItem) {
      if ((content = indent.asNumberedListItem().getContent().getText()).startsWith(":")
          || content.startsWith("*")) {
        return true;
      }
    }
    return false;
  }

  public void extractTranslations(WikiContent wk) {

    Resource globalGlossResource = null;
    String globalGloss = "";
    int rank = 1; // }

    for (Token tok : wk.templates()) {
      Template template = (Template) tok;
      if ("Z".equals(template.getName()))
        continue;
      Map<String, String> args = template.cloneParsedArgs();
      if ("werger-ser".equals(template.getName())) {
        if (template.getParsedArgs().get("1") != null) {
          globalGloss = args.get("1");
          globalGlossResource = wdh.createGlossResource(globalGloss, rank++);
        } else {
          globalGloss = "";
          globalGlossResource = null;
        }
      } else if ("werger-bin".equals(template.getName())) {
        globalGloss = "";
        globalGlossResource = null;
      } else if ("W".equals(template.getName()) || "W+".equals(template.getName())
          || "W-".equals(template.getName())) {
        String lang = LangTools.getCode(args.get("1"));
        String word = args.get("2");
        args.remove("1");
        args.remove("2");
        String usage = null;
        if (!args.isEmpty()) {
          usage = args.toString();
        }
        if (null != lang && null != word && word.trim().length() > 0) {
          wdh.registerTranslation(lang, globalGlossResource, usage, word);
        }
      }

    }
  }

  private void extractPron(WikiContent wk) {
    // WARN: this may be called from level 3 (no entry defined yet or as level 4 from inside an
    // entry)
    log.debug("Pronunciation Section : {}", wk.toString());
    for (Token tok : wk.templates()) {
      Template template = tok.asTemplate();
      String name = template.getName();
      switch (name) {
        case "ku-IPA":
        case "IPA-ku":
        case "IPA":
          String pron;
          if ("ku-IPA".equals(name)) {
            pron = template.getParsedArgs().get("1");
          } else {
            pron = template.getParsedArgs().get("2");
          }
          if (null == pron || pron.trim().length() == 0) {
            pron = pronunciationExpander.expandAll(template.toString(), null);
            if (null != pron && pron.startsWith("IPA(kilîd): "))
              pron = pron.substring("IPA(kilîd): ".length());
          }
          if (null != pron && pron.trim().length() > 0) {
            for (String p : pron.split(",")) {
              if (null != p && (p = p.trim()).length() > 0) {
                // TODO: normalize pronunciation (remove or ensure '/' around pron)
                wdh.registerPronunciation(p, "kur-fonipa");
              }
            }
          }
          break;
      }
    }
  }
}


