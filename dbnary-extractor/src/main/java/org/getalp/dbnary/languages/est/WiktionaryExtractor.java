package org.getalp.dbnary.languages.est;

import static org.getalp.dbnary.tools.TokenListSplitter.splitAndProcessToken;
import static org.getalp.dbnary.tools.TokenListSplitter.splitProcessAndKeepToken;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPattern;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {
  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

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
    if (estWdh.isPartOfSpeech(sname)) {
      if (log.isTraceEnabled() && t.getLevel() > 3)
        log.trace("Part of speech title in a non PoS Header [{}]: {}", this.getWiktionaryPageName(), sname);
      return Pair.of(sname, SectionKind.MAIN);
    }
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
      case "väljend":
      case "sõnaühendid":
        return Pair.of(sname, SectionKind.COMPOUNDS);
      case "tõlked":
        return Pair.of(sname, SectionKind.TRANSLATIONS);
      case "rööpvormid":
      case "variandid":
      case "variant":
      case "tähised":
      case "lühendid":
      case "lühend":
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
      // log.trace("H2 language: '{}' ---> {}", name, languageCode);
      return languageCode != null ? languageCode : ""; // H2 are always tied to a language
    } else if (t instanceof Template && languageTemplateMatcher.reset(t.asTemplate().getName()).matches()) {
      String languageCode = languageTemplateMatcher.group(1).trim();
      // log.trace("Template language: '{}'", languageCode);
      return switch (languageCode) {
        case "jaR" -> "ja-Latn";
        case "lvpn" -> "lv";
        case "nlf" -> "lfn";
        case "bh" -> "bih";
        default -> LangTools.getCode(languageCode);
      };
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
        case TRANSLATIONS:
          extractTranslations(section.getRight());
          break;
        case NYMS:
          extractNymSection(section.getMiddle().getLeft(), section.getRight());
        case PRONUNCIATION:
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

  private sealed

  interface SubSection {
  }

  private record NYM(String nym) implements SubSection {
    }

  private record TRANSLATIONS() implements SubSection {
    }

  private record VARIANTS() implements SubSection {
    }

  private record ALTERNATIVE_FORMS() implements SubSection {
    }

  private record OTHER_NAMES() implements SubSection {
    }

  private record OLD_SPELLING() implements SubSection {
    }

  private record ABBREVIATIONS() implements SubSection {
    }

  private record DIALECTS() implements SubSection {
    }

  private record SYMBOLS() implements SubSection {
    }

  private record SIGNS() implements SubSection {
    }

  private record EXAMPLES() implements SubSection {
    }

  private record COMPOUNDS() implements SubSection {
    }

  private record USAGE() implements SubSection {
    }

  private record SEE_ALSO() implements SubSection {
    }

  private record RECTION() implements SubSection {
    }

  private final NYM SYNONYM = new NYM("syn");
  private final NYM ANTONYM = new NYM("ant");
  private final NYM HYPONYM = new NYM("hypo");
  private final NYM HYPERNYM = new NYM("hyper");
  private final TRANSLATIONS TRANSLATIONS = new TRANSLATIONS();
  private final VARIANTS VARIANTS = new VARIANTS();
  private final ALTERNATIVE_FORMS ALTERNATIVE_FORMS = new ALTERNATIVE_FORMS();
  private final OTHER_NAMES OTHER_NAMES = new OTHER_NAMES();
  private final OLD_SPELLING OLD_SPELLING = new OLD_SPELLING();
  private final ABBREVIATIONS ABBREVIATIONS = new ABBREVIATIONS();
  private final DIALECTS DIALECTS = new DIALECTS();
  private final SYMBOLS SYMBOLS = new SYMBOLS();
  private final SIGNS SIGNS = new SIGNS();
  private final EXAMPLES EXAMPLES = new EXAMPLES();
  private final COMPOUNDS COMPOUNDS = new COMPOUNDS();
  private final USAGE USAGE = new USAGE();
  private final SEE_ALSO SEE_ALSO = new SEE_ALSO();
  private final RECTION RECTION = new RECTION();

  private void extractDefinitions(List<Token> mainSection) {
        Deque<Token> stack = new LinkedList<>(mainSection);
        // Iterate through list tokens and extract definition, examples or other info
        SubSection currentSubsection = null;
        while (!stack.isEmpty()) {
            Token token = stack.pop();
            switch (token) {
                case NumberedListItem t -> {
                    WikiContent content = t.getContent();
                    switch (t.getListPrefix()) {
                        case "#" -> {
                            currentSubsection = null;
                            extractDefinition(content.getText(), 1);
                        }
                        case "#:" -> {
                            // check if it is a subsection title
                            Pair<SubSection, String> subSectionAndTrail = decodeSubSection(t);
                            if (null != subSectionAndTrail.getLeft()) {
                                log.trace("Starting subsection extraction: {}", subSectionAndTrail.getLeft());
                                currentSubsection = subSectionAndTrail.getLeft();
                                if (subSectionAndTrail.getRight() != null && !subSectionAndTrail.getRight().isEmpty()) {
                                    // push back the trailing into the list of item as if they were one after the other
                                    stack.push(new WikiText("FAKEPAGE", "#:* " + subSectionAndTrail.getRight() + "\n").tokens().getFirst());
                                }
                            } else {
                                String right = subSectionAndTrail.getRight();
                                String trailing = right.replaceAll("<br\\s*/?>", "");
                                if (!trailing.equals(right)) {
                                    log.trace("MODIFICATION: {} -> {}", right, trailing);
                                }
                                trailing = StringUtils.strip(trailing, " \t\n\r\f:;");
                                if (trailing.isEmpty())
                                    continue;
                                log.debug("No subsection extraction after a '#:': {}", t.getContent().getText());
                                // Handle the rest as an example
                                stack.push(new WikiText("FAKEPAGE", "#:: " + subSectionAndTrail.getRight() + "\n").tokens().getFirst());
                            }
                        }
                        case "#:*", "#::" -> {
                            // Extract the links and register them depending on the current subsection
                            switch (currentSubsection) {
                                case EXAMPLES _ -> {
                                    extractExample(content);
                                }
                                case TRANSLATIONS _ -> {
                                    extractTranslationLine(t, true);
                                }
                                case NYM(String nym) -> {
                                    extractNymLine(nym, content, true);
                                }
                                case null -> {
                                    // Handle an example by default
                                    extractExample(content.getText());
                                }
                                default -> {
                                    log.debug("No subsection extraction after a '#:*' or '#::': {}", content.getText());
                                }
                            }
                        }
                        case null, default -> {
                        }
                    }
                }
                case IndentedItem indentedItem -> {
                    // Consider it as a definition, when only an indentation, with exceptions
                    switch (indentedItem.getListPrefix()) {
                        case ":", ":#" -> {
                            String text = indentedItem.getContent().getText();
                            if (text.contains("{{kuula")) continue;
                            extractDefinition(text, 0);
                        }
                        case null, default -> log.trace("Got non definition indented item: {}", token);
                    }
                }
                case Text _ -> {
                    if (log.isTraceEnabled()) {
                        String text = token.getText();
                        text = StringUtils.strip(text);
                        if (!text.isEmpty()) {
                            log.trace("Ignoring Text token: {}", text);
                        }
                    }
                }
                case Template template -> {
                    // check if it is a subsection title encoded as a template
                    SubSection subSection = decodeSubSection(template);
                    if (null != subSection) {
                        log.trace("Starting subsection extraction: {}", subSection);
                        currentSubsection = subSection;
                    } else {
                        log.debug("No subsection extraction after a template: {}", template.getName());
                        currentSubsection = null;
                        if (template.getName().endsWith("/tõlked")) {
                            // Handle very specific case where the template contains specific translations
                            // e.g.: {{taevatäht/tõlked}}
                            // Get the template content and extract translations from it, then switch to translation mode to handle
                            // eventual additional translations.
                            String templateSource = this.wi.getTextOfPageWithRedirects("Mall:" + template.getName());
                            if (templateSource != null) {
                                List<Token> templateTokens = new WikiText(template.getName(), templateSource).tokens();
                                ListIterator<Token> it = templateTokens.listIterator(templateTokens.size());
                                while (it.hasPrevious()) {
                                    stack.push(it.previous());
                                }
                            }
                        }
                    }
                }
                case null, default -> log.trace("Other token: {}", token);
            }
        }
    }

    private void extractExample(WikiContent content) {
        // First catch example templates
        List<Template> exampleTemplates = content.wikiTokensStream().filter(tok -> tok instanceof Template).map(Token::asTemplate)
                .filter(tok -> "näide".equals(tok.getName())).toList();
        if (exampleTemplates.isEmpty()) {
          extractExample(content.getText());
        } else {
            for (Template exampleTemplate : exampleTemplates) {
                Set<Pair<Property, RDFNode>> context = new HashSet<>();
                String example = exampleTemplate.getParsedArg("1");
                String translatedExample = exampleTemplate.getParsedArg("2");
                if (null != example && !example.trim().isEmpty()) {
                    context.add(Pair.of(SKOS.example, ResourceFactory.createLangLiteral(translatedExample, wdh.getExtractedLanguage())));
                    wdh.registerExample(example, context);
                }
            }
        }
    }

    private void extractNymLine(String nym, WikiContent content, boolean senseLocal) {
        log.trace("NYM({}): {}", nym, content.getText());
        for (Token t: content.wikiTokens()) {
            switch (t) {
                case WikiText.InternalLink internalLink ->
                        estWdh.registerNymRelation(t.asInternalLink().getTargetText(), nym, senseLocal);
                case Template template when template.getName().equals("sünonüüm") ->
                        estWdh.registerNymRelation(t.asInternalLink().getTargetText(), nym, senseLocal);
                case null, default -> {
                }
            }
        }
    }

    private void extractNymSection(String nym, List<Token> content) {
        for (Token t : content) {
            if (t instanceof IndentedItem) {
                extractNymLine(estWdh.normaliseNym(nym), t.asIndentedItem().getContent(), false);
            }
        }
    }


    private void extractTranslationLine(IndentedItem t, boolean senseLocal) {
        log.trace("Translation line: {}", t.getContent().getText());
        WikiCharSequence line = new WikiCharSequence(t.getContent());
        TranslationLineParser tp = new TranslationLineParser(this.getWiktionaryPageName());
        tp.extractTranslationLine(line, senseLocal, estWdh);
    }


    private final Matcher subSectionHeader = Pattern.compile("\\s*'{0,3}(.*?)(?::'{0,3}|'{1,3}:?|:|$)\\s*(.*)").matcher("");
    private final Map<String, SubSection> SUBSECTION_TEMPLATES = Map.of("T", TRANSLATIONS, "sünonüümid", SYNONYM, "S", SYNONYM, "A", ANTONYM, "H", HYPONYM, "V", VARIANTS, "R", ALTERNATIVE_FORMS, "L", ABBREVIATIONS);
    private final Matcher illUsedTemplate = WikiPattern.compile("\\s*(\\p{Template}):?\\s*(.*)").matcher("");

    private Pair<SubSection, String> decodeSubSection(NumberedListItem t) {
        boolean containsSubSectionHeader = subSectionHeader.reset(t.getContent().getText()).matches();
        if (containsSubSectionHeader) {
            String subSectionTitle = StringUtils.strip(subSectionHeader.group(1), " \t\n\r\f:;").toLowerCase();
            SubSection subSection = decodeSubSection(subSectionTitle);
            String trailing = subSectionHeader.group(2);
            if (null != subSection) {
                trailing = trailing.replaceAll("<.*?>", "");
                trailing = StringUtils.strip(trailing, " \t\n\r\f:;");
                if (trailing.isEmpty()) trailing = null;
                if (null != trailing)
                    log.trace("Non empty trailing in subsection header: {}: {}", subSectionTitle, trailing);
                return Pair.of(subSection, trailing);
            }
        }
        // Check if the list item contains one of the (ill used) templates S, T, L
        WikiCharSequence line = new WikiCharSequence(t.getContent());
        if (illUsedTemplate.reset(line).matches()) {
            Template subSectionTemplateCandidate = line.getToken(illUsedTemplate.group(1)).asTemplate();
            SubSection subSection = SUBSECTION_TEMPLATES.get(subSectionTemplateCandidate.getName());
            if (null != subSection) {
                log.trace("Found ill-used subsection template: {}", t.getContent().getText());
                String trailing = subSectionHeader.group(2);
                trailing = trailing.replaceAll("<.*?>", "");
                trailing = StringUtils.strip(trailing, " \t\n\r\f:;");
                if (trailing.isEmpty()) trailing = null;
                if (null != trailing)
                    log.trace("Non empty trailing in subsection template: {}: {}", subSectionTemplateCandidate.getText(), trailing);
                return Pair.of(subSection, trailing);
            }
        }
        return Pair.of(null, t.getContent().getText());
    }

    @Nullable
    private SubSection decodeSubSection(String subSectionTitle) {
        switch (subSectionTitle) {
            case "tõlked", "tõlkeid", "T", "tõlge", "vasted teistes keeltes", "tõlkima", "tõlkes",
                 "tõlked ja laenud" -> {
                return TRANSLATIONS;
            }
            case "sünonüümid", "sünonüüm", "S", "sümonüüm", "sünonüümod", "sünonüümiga", "sünonüümi", "sünonüumid",
                 "Sünomüümid",
                 "rahvapärased nimed", "stiililiselt lähedased sünonüümid" -> {
                return SYNONYM;
            }
            case "antonüümid", "antonüüm", "A", "antononüümid", "vastandsõna" -> {
                return ANTONYM;
            }
            case "hüponüümid", "hüponüüm", "H", "hüpoüümid" -> {
                return HYPONYM;
            }
            case "hüpernüümid", "hüpernüüm", "hüpernonüümid", "hüperonüümid" -> {
                return HYPERNYM;
            }
            case "variant", "variandid", "V" -> {
                return VARIANTS;
            }
            case "rööpvormid", "rööpvorm", "R", "rööpkuju", "rööpkujud", "teised kujud" -> {
                return ALTERNATIVE_FORMS;
            }
            case "lühend", "lühendid", "L", "lühinimi" -> {
                return ABBREVIATIONS;
            }
            case "tähised", "tähis", "märk" -> {
                return SIGNS;
            }
            case "murdesõnad" -> {
                return DIALECTS;
            }
            case "teised nimetused" -> {
                return OTHER_NAMES;
            }
            case "vaata ka" -> {
                return SEE_ALSO;
            }
            case "vana kirjaviis" -> {
                return OLD_SPELLING;
            }
            case "sümbol", "keemiline sümbol" -> {
                return SYMBOLS;
            }
            case "näiteid" -> {
                return EXAMPLES;
            }
            case "liitsõnad" -> {
                return COMPOUNDS;
            }
            case "rektsioon" -> {
                return RECTION;
            }
            case "kasutamine" -> {
                return USAGE;
            }
            default -> {
                return null;
            }
        }
    }

    private SubSection decodeSubSection(Template t) {
        String name = t.getName().trim();
        return decodeSubSection(name);
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

    private void extractMorphology(String name, List<Token> contents) {
    }

    @Override
    public Resource extractDefinition(String definition, int defLevel) {
        String def = expander.expandAll(definition, null);
        if (!def.isEmpty()) {
            estWdh.registerNewDefinition(def, defLevel);
        }
        return null;
    }

    private void extractTranslations(List<Token> contents) {
        for (Token t : contents) {
            if (t instanceof IndentedItem) {
                extractTranslationLine(t.asIndentedItem(), false);
            }
        }
    }

}
