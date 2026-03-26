package org.getalp.dbnary.languages.est;

import static org.getalp.dbnary.tools.TokenListSplitter.split;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {
  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected WiktionaryDataHandler estWdh;

  protected final static HashSet<String> sectionHeadings = new HashSet<>();
  protected final static HashSet<String> ignoredHeadings = new HashSet<>();

  protected final static boolean exolex = false;

  static {
    sectionHeadings.add("výslovnost"); // PRONUNCIATION
    ignoredHeadings.add("dělení"); // HYPHENATION
    ignoredHeadings.add("etymologie");
    sectionHeadings.add("skloňování"); // MORPHOLOGY
    sectionHeadings.add("význam"); // DEFINITIONS
    sectionHeadings.add("překlady"); // TRANSLATIONS
    sectionHeadings.add("synonyma");
    sectionHeadings.add("antonyma");
    ignoredHeadings.add("související"); // RELATED
    ignoredHeadings.add("slovní spojení"); // COLOCATION
    ignoredHeadings.add("fráze a idiomy"); // PHRASES
    ignoredHeadings.add("přísloví"); // PROVERBS
    ignoredHeadings.add("přísloví, úsloví a pořekadla");
  }

  public BiConsumer<String, List<Token>> sectionFunction(String section) {
    switch (section) {
      case "výslovnost":
        return this::extractPronunciation; // PRONUNCIATION
      case "skloňování":
        return this::extractMorphology; // MORPHOLOGY
      case "význam":
        return this::extractDefinitions; // DEFINITIONS
      case "překlady":
        return this::extractTranslations; // TRANSLATIONS
      case "synonyma":
        return this::extractNyms;
      case "antonyma":
        return this::extractNyms;
      default:
        return (a, b) -> {
        };
    }
  }

  protected final static HashMap<String, String> nyms = new HashMap<>();

  static {
    nyms.put("synonyma", "syn");
    nyms.put("antonyma", "ant");
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    this.estWdh = (WiktionaryDataHandler) wdh;
  }

  public void extractData() {
    estWdh.initializePageExtraction(getWiktionaryPageName());

    WikiText doc = new WikiText(getWiktionaryPageName(), pageContent);

    List<Pair<Token, List<Token>>> languageData =
        split(doc.tokens(), t -> getLanguageCode(t) != null);

    for (Pair<Token, List<Token>> language : languageData) {
      // extractLanguageData(language.getLeft(), language.getRight());
    }

    estWdh.finalizePageExtraction();
  }

  private Matcher languageTemplateMatcher = Pattern.compile("-(\\w{2,3})-").matcher("");

  public String getLanguageCode(Token t) {
    /* language sections are either 2nd level headings or templates named -xx- xx being a lg code */
    if (t instanceof Heading && t.asHeading().getLevel() == 2) {
      String name = t.asHeading().getContent().getText().trim();
      log.debug("H2 language: '{}' ---> {}", name, EstonianLanguageCodes.threeLettersCode(name));
      return null;
    } else if (t instanceof Template
        && languageTemplateMatcher.reset(t.asTemplate().getName()).matches()) {
      String languageCode = languageTemplateMatcher.group(1).trim();
      log.debug("Template language: '{}'", languageCode);
      return LangTools.getCode(languageCode);
    }

    return null;
  }

  private void extractLanguageData(Token language, List<Token> contents) {
    if (null == language)
      return;

    String lang = "est";

    log.trace("'{}': Extracting data for: {}", getWiktionaryPageName(), lang);

    if (null == lang)
      return;

    if (!lang.equals("ces") && !exolex) {
      log.trace("'{}': Exolex is disabled. Ignoring language {}", getWiktionaryPageName(), lang);
      return;
    }


    estWdh.initializeLanguageSection(lang);
    extractLanguageSections(contents, 2);
    estWdh.finalizeLanguageSection();
  }

  private void extractLanguageSections(List<Token> contents, int lvl) {
    List<Pair<Token, List<Token>>> sections = split(contents, t -> isSectionHeader(t, lvl + 1));

    for (Pair<Token, List<Token>> section : sections) {
      Token header = section.getLeft();

      if (header instanceof Heading) {
        String name = header.asHeading().getContent().getText().trim().toLowerCase();

        if (estWdh.isPartOfSpeech(name)) {
          estWdh.initializeLexicalEntry(name);
          /*
           * recursive call because we're sure not to encounter another part of speech
           */
          extractLanguageSections(section.getRight(), lvl + 1);
        } else if (sectionHeadings.contains(name)) {
          sectionFunction(name).accept(name, section.getRight());
        } else if (ignoredHeadings.contains(name)) {
          log.debug("'{}': Ignoring known heading {}", getWiktionaryPageName(), name);
        } else {
          log.debug("'{}': Ignoring unknown heading {} (level {})", getWiktionaryPageName(), name,
              header.asHeading().getLevel());
        }
      } else {
        log.debug("'{}': Unexpected non-heading token after section split: {}",
            getWiktionaryPageName(), header);
      }
    }
  }

  private boolean isSectionHeader(Token t, int lvl) {
    if (t instanceof Heading) {
      Heading h = t.asHeading();

      return h.getLevel() == lvl;
    }

    return false;
  }

  private void extractPronunciation(String name, List<Token> contents) {
    /*
     * we only take the first one, as the subsequent ones are regional pronounciations.
     */
    for (Token t : contents) {
      if (t instanceof Template) {
        Template tm = t.asTemplate();

        if (tm.getName().equals("IPA")) {
          String pron = tm.getParsedArgs().get("1");

          estWdh.registerPronunciation(pron, "ces");

          return;
        }
      }
    }
  }

  private void extractMorphology(String name, List<Token> contents) {}

  private void extractDefinitions(String name, List<Token> contents) {
    for (Token t : contents) {
      if (t instanceof NumberedListItem) {
        NumberedListItem li = t.asNumberedListItem();

        /* examples */
        if (li.getContent().getText().startsWith("*")) {
          /* 'Přiklad' template */
          extractExample(li);
        } else {
          extractDefinition(li.getContent().getText().trim(), li.getLevel());
        }
      }
    }
  }

  @Override
  public Resource extractDefinition(String definition, int defLevel) {
    String def = expander.expandAll(definition, null);
    if (!def.isEmpty()) {
      estWdh.registerNewDefinition(def, defLevel);
    }
    return null;
  }

  private void extractExample(NumberedListItem li) {
    for (Token t : li.getContent().tokens()) {
      if (t instanceof Template) {
        Template tm = t.asTemplate();
        Map<String, String> args = tm.getParsedArgs();

        if (tm.getName().trim().equals("Příklad") && args.size() == 2
            && args.get("1").equals("cs")) {
          super.extractExample(args.get("2"));
        } else {
          log.debug("'{}': Unexpected example template: '{}' ({} arguments)",
              getWiktionaryPageName(), tm.getName(), args.size());
        }
      }
    }
  }

  private void extractTranslations(String name, List<Token> contents) {
    for (Token t : contents) {
      if (t instanceof NumberedListItem) {
        NumberedListItem li = t.asNumberedListItem();

        for (Token t1 : li.getContent().tokens()) {
          extractTranslation(t1);
        }
      } else {
        extractTranslation(t);
      }
    }
  }

  private void extractTranslation(Token t) {
    /* {{ Překlady | význam = <sense> | <lang1> = <trad1> | ... | <langN> = <tradN> }} */
    if (t instanceof Template) {
      Template tm = t.asTemplate();

      if (tm.getName().trim().equals("Překlady")) {
        LinkedHashMap<String, WikiContent> args = tm.getArgs();

        if (args.size() == 0) {
          log.trace("'{}': No translations for this sense.", getWiktionaryPageName());
          return;
        }

        WikiContent sense = args.get("význam");
        String senseText = (sense == null) ? "" : sense.toString().trim();
        Resource gloss = estWdh.createGlossResource(senseText);

        for (Map.Entry<String, WikiContent> entry : args.entrySet()) {
          String lang = entry.getKey();
          WikiContent trans = entry.getValue();

          if (!lang.equals("význam")) {
            for (Token tk : trans.tokens()) {
              // TODO: match on token type
              if (tk instanceof Template) {
                Template tm1 = tk.asTemplate();
                Map<String, String> args1 = tm1.getParsedArgs();

                if (tm1.getName().equals("P") && args1.size() >= 2) {
                  estWdh.registerTranslation(lang, gloss, senseText, args1.get("2"));
                }
              }
            }
          }
        }
      } else {
        log.debug("'{}': Expected 'Překlady' template for translations, found '{}' template",
            getWiktionaryPageName(), tm.getName());
      }
    } else {
      if (!t.getText().trim().equals("")) {
        log.debug("'{}': expected template for translation, found {}", getWiktionaryPageName(), t);
      }
    }
  }

  private void extractNyms(String name, List<Token> contents) {
    String nymrel = nyms.get(name);

    for (Token t : contents) {
      if (t instanceof NumberedListItem) {
        NumberedListItem li = t.asNumberedListItem();

        for (Token tl : li.getContent().tokens()) {
          if (tl instanceof Link) {
            extractNymString(nymrel, tl.asLink().getLinkText());
          }
        }
      }
    }
  }

  private void extractNymString(String nymrel, String nym) {
    if (!nym.trim().equals("—")) {
      /* empty synonym symbol = no register */
      estWdh.registerNymRelation(nym, nymrel);
    }
  }
}
