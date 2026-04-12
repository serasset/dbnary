package org.getalp.dbnary.languages.est;

import static org.getalp.dbnary.tools.TokenListSplitter.splitAndProcessToken;
import static org.getalp.dbnary.tools.TokenListSplitter.splitProcessAndKeepToken;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {
  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected WiktionaryDataHandler estWdh;

  protected enum SectionKind {
    PRONUNCIATION, MORPHOLOGY, MAIN, IGNORED_POS, TRANSLATIONS, NYMS, COMPOUNDS, PROVERBS, DERIVATIVES, IGNORED, ALTERNATIVE_FORMS, ETYMOLOGY
  }

  protected Pair<String, SectionKind> decodeSection(Token t) {
    // As parameter polymorphism is compile time,
    if (t instanceof Heading) {
      return decodeSection(t.asHeading());
    } else if (t instanceof Template) {
      return decodeSection(t.asTemplate());
    }
    return null;
  }

  Matcher numberedPoS = Pattern.compile("^(.*?)\\s*\\(?\\d+\\)?\\s*$").matcher("");

  protected Pair<String, SectionKind> decodeSection(Heading t) {
    String sname = t.getContent().getText().trim().toLowerCase();
    if (numberedPoS.reset(sname).matches()) {
      sname = numberedPoS.group(1).trim();
    }

    if (sname.endsWith("vorm")) {
      log.trace("Assuming {} is a form PoS.", sname);
      return Pair.of(sname, SectionKind.IGNORED_POS); // These are inflected forms
    }
    if (estWdh.isPartOfSpeech(sname))
      return Pair.of(sname, SectionKind.MAIN);
    switch (sname) {
      case "hääldus":
        return Pair.of(sname, SectionKind.PRONUNCIATION);
      case "vormid":
      case "vormid (käänded)":
      case "käänamine":
      case "käänded":
      case "pööramine":
        return Pair.of(sname, SectionKind.MORPHOLOGY);
      case "päritolu":
      case "etümoloogia":
        return Pair.of(sname, SectionKind.ETYMOLOGY);
      case "tuletised":
      case "tuletis":
      case "tuletatud mõisted":
        return Pair.of(sname, SectionKind.DERIVATIVES);
      case "liitsõnad":
      case "liitsõna":
      case "sõnad":
      case "sõnu":
      case "ühendid":
      case "liitsõnad ja fraasid":
      case "fraasid":
      case "nimisõnafraasid":
      case "väljendid":
      case "sõnaühendid":
        return Pair.of(sname, SectionKind.COMPOUNDS);
      case "tõlked":
        return Pair.of(sname, SectionKind.TRANSLATIONS);
      case "rööpvormid":
      case "variandid":
      case "variant":
      case "tähised":
      case "lühendid":
        return Pair.of(sname, SectionKind.ALTERNATIVE_FORMS);
      case "sünonüümid":
      case "sünonüüm":
      case "antonüümid":
      case "antonüüm":
        return Pair.of(sname, SectionKind.NYMS);
      case "vaata ka": // See also
      case "vaata": // See also
      case "vikipeedias": // See also
      case "välislingid": // External Links
      case "välislink": // External Links
      case "märkus": // Note
      case "märkused": // Note
      case "allikad": // Sources
      case "kirjandus": // Literature
      case "stiil": // Style
      case "laenud": // Loans
      case "kasutamine": // Usage
      case "võrded": // Comparatives
      case "rektsioon": // Rection
      case "viited": // References
      case "homofoonid": // Homophones
      case "homofoon": // Homophones
      case "transkriptsioon": // Transcriptions
      case "järglased": // "Offsprings" borrowings in other languages
        return Pair.of(sname, SectionKind.IGNORED);
    }
    log.debug("Could not decode section: " + sname);
    return Pair.of(sname, SectionKind.IGNORED);
  }

  protected Pair<String, SectionKind> decodeSection(Template t) {
    String tname = t.getName().trim();
    if (tname.startsWith("-") && tname.endsWith("-")) {
      tname = tname.substring(1, tname.length() - 1);
      if (tname.endsWith("vorm"))
        return Pair.of(tname, SectionKind.IGNORED_POS); // These are inflected forms

      if (estWdh.isPartOfSpeech(tname))
        return Pair.of(tname, SectionKind.MAIN);

      switch (tname) {
        case "hääldus":
          return Pair.of(tname, SectionKind.PRONUNCIATION);
        case "rööpvormid":
          return Pair.of(tname, SectionKind.ALTERNATIVE_FORMS);
        case "päritolu":
          return Pair.of(tname, SectionKind.ETYMOLOGY);
        case "tõlked":
          return Pair.of(tname, SectionKind.TRANSLATIONS);
        case "käänded":
          return Pair.of(tname, SectionKind.MORPHOLOGY);
      }
      log.trace("Could not decode template: -{}-", tname);
    }
    log.trace("Template not considered as a section: " + tname);
    return null;
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

    List<Pair<String, List<Token>>> languageData = splitAndProcessToken(doc.tokens(), this::getLanguageCode);

    for (Pair<String, List<Token>> language : languageData) {
      extractLanguageData(language.getLeft(), language.getRight());
    }

    estWdh.finalizePageExtraction();
  }

  private final Matcher languageTemplateMatcher = Pattern.compile("-(\\w{2,4})-").matcher("");

  public String getLanguageCode(Token t) {
    /* language sections are either 2nd level headings or templates named -xx- xx being a lg code */
    if (t instanceof Heading && t.asHeading().getLevel() == 2) {
      String name = t.asHeading().getContent().getText().trim();
      String languageCode = EstonianLanguageCodes.threeLettersCode(name);
      log.trace("H2 language: '{}' ---> {}", name, languageCode);
      return languageCode != null ? languageCode : ""; // H2 are always tied to a language
    } else if (t instanceof Template && languageTemplateMatcher.reset(t.asTemplate().getName()).matches()) {
      String languageCode = languageTemplateMatcher.group(1).trim();
      log.trace("Template language: '{}'", languageCode);
      switch (languageCode) {
        case "jaR":
          return "ja-Latn";
        case "lvpn":
          return "lv";
        case "nlf":
          return "lfn";
        case "bh":
          return "bih";
      }
      return LangTools.getCode(languageCode);
    }
    // null means it is not a language
    return null;
  }

  private void extractLanguageData(String language, List<Token> contents) {
    if (null == language || language.isEmpty()) {
      return;
    }

    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !wdh.getExtractedLanguage().equals(language))
      return;

    // The language is always defined when arriving here, but we should check if we extract it
    String normalizedLanguage = validateAndStandardizeLanguageCode(language);
    if (normalizedLanguage == null) {
      log.trace("Ignoring language section {} for {}", language, getWiktionaryPageName());
      return;
    }
    wdh.initializeLanguageSection(normalizedLanguage);
    extractLanguageSections(contents, 2);
    estWdh.finalizeLanguageSection();
  }

  private void extractLanguageSections(List<Token> contents, int lvl) {
    List<Triple<Token, Pair<String, SectionKind>, List<Token>>> sections = splitProcessAndKeepToken(contents, this::decodeSection);

    for (Triple<Token, Pair<String, SectionKind>, List<Token>> section : sections) {
      SectionKind sectionKind = section.getMiddle().getRight();

      switch (sectionKind) {
        case MAIN:
          estWdh.initializeLexicalEntry(section.getMiddle().getLeft());
          extractDefinitions(section.getRight());
          break;
        case IGNORED_POS:
          estWdh.voidPartOfSpeech();
          break;
        case PRONUNCIATION:
        case TRANSLATIONS:
        case DERIVATIVES:
        case PROVERBS:
        case MORPHOLOGY:
        case COMPOUNDS:
        case IGNORED:
          break;
      }
      // if (header instanceof Heading) {
      // String name = header.asHeading().getContent().getText().trim().toLowerCase();

      // if (estWdh.isPartOfSpeech(name)) {
      // estWdh.initializeLexicalEntry(name);
      // /*
      // * recursive call because we're sure not to encounter another part of speech
      // */
      // extractLanguageSections(section.getRight(), lvl + 1);
      // } else if (sectionHeadings.contains(name)) {
      // sectionFunction(name).accept(name, section.getRight());
      // } else if (ignoredHeadings.contains(name)) {
      // log.debug("'{}': Ignoring known heading {}", getWiktionaryPageName(), name);
      // } else {
      // log.debug("'{}': Ignoring unknown heading {} (level {})", getWiktionaryPageName(), name,
      // header.asHeading().getLevel());
      // }
      // } else {
      // log.debug("'{}': Unexpected non-heading token after section split: {}",
      // getWiktionaryPageName(), header);
      // }
    }
  }

  private void extractDefinitions(List<Token> mainSection) {
    // Iterate through list tokens and extract definition, examples or other info
    String currentSubsection = null;
    for (Token token : mainSection) {
      if (token instanceof NumberedListItem) {
        NumberedListItem t = token.asNumberedListItem();
        WikiContent content = t.getContent();
        if ("#".equals(t.getListPrefix())) {
          extractDefinition(content.getText(), 1);
        } else if ("#:".equals(t.getListPrefix())) {
          // check if it is a subsection title
          String subSection = decodeSubSection(t);
          if (null != subSection) {
            log.trace("Starting subsection extraction: {}", subSection);
            currentSubsection = subSection;
          } else {
            log.debug("No subsection extraction after a '#:': {}", t.getContent().getText());
          }
        } else if ("#:*".equals(t.getListPrefix())) {
          // Extract the links and register them depending on the current subsection
          log.trace("Extracting as a subsection: {}", t.getContent().getText());
        } else if ("#::".equals(t.getListPrefix())) {
          // Extract an example
          log.debug("UNIMPLEMENTED Example extraction of {}", t.getContent().getText());
        }
      } else if (token instanceof IndentedItem) {
        log.trace("Got non definition indented item: {}", token);
      } else if (token instanceof Text) {
        if (log.isTraceEnabled()) {
          String text = token.getText();
          text = StringUtils.strip(text);
          if (!text.isEmpty()) {
            log.trace("Ignoring Text token: {}", text);
          }
        }
      } else {
        log.trace("Other token: {}", token);
      }
    }
  }

  private Matcher subSectionHeader = Pattern.compile("\\s*'''(.*)''':\\s*").matcher("");

  private String decodeSubSection(NumberedListItem t) {
    boolean containsSubSectionHeader = subSectionHeader.reset(t.getContent().getText()).matches();
    if (containsSubSectionHeader) {
      return subSectionHeader.group(1);
    }
    return null;
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

  // TODO: translations in definitions: template T or #:'''Tolked''' or some specific template calls
  // (see 'täht')
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

        if (tm.getName().trim().equals("Příklad") && args.size() == 2 && args.get("1").equals("cs")) {
          super.extractExample(args.get("2"));
        } else {
          log.debug("'{}': Unexpected example template: '{}' ({} arguments)", getWiktionaryPageName(), tm.getName(), args.size());
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
        log.debug("'{}': Expected 'Překlady' template for translations, found '{}' template", getWiktionaryPageName(), tm.getName());
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
