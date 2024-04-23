package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.morphology.InflectionData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedWikiModel extends EnglishWikiModel {

  private final Logger log = LoggerFactory.getLogger(CombinedWikiModel.class);

  public enum Mode {
    EXAMPLE, DEFINITION, MORPHOLOGY, PRONUNCIATION, NONE
  }

  // STATIC CONFIGURATIONS FOR EACH MODE
  // DEFINITION MODE
  private static final HashSet<String> ignoredTemplates = new HashSet<>();
  static {
    ignoredTemplates.add("categorize");
    ignoredTemplates.add("catlangname");
    ignoredTemplates.add("catlangcode");
    ignoredTemplates.add("rfex");
    ignoredTemplates.add("rfd-sense");
    ignoredTemplates.add("attention");
    ignoredTemplates.add("attn");
    ignoredTemplates.add("rfclarify");
    ignoredTemplates.add("rfquote");
    ignoredTemplates.add("rfquotek");
    ignoredTemplates.add("rfv-sense");
    ignoredTemplates.add("rfc-sense");
    ignoredTemplates.add("rfquote-sense");
    ignoredTemplates.add("rfdef");
    ignoredTemplates.add("tea room sense");
    ignoredTemplates.add("rfd-redundant");
    ignoredTemplates.add("wikipedia");
    ignoredTemplates.add("wp");
    ignoredTemplates.add("slim-wikipedia");
    ignoredTemplates.add("swp");
    ignoredTemplates.add("slim-wp");
    ignoredTemplates.add("multiple images");
  }
  // EXAMPLE MODE
  static {
    ignoredTemplates.add("rfex");
    ignoredTemplates.add("rfquote");
    ignoredTemplates.add("rfquote-sense");
    ignoredTemplates.add("rfquotek");
  }

  private static final Map<String, String> nyms = new HashMap<>();

  static {
    nyms.put("syn", "syn");
    nyms.put("synonyms", "syn");
    nyms.put("syn-lite", "syn");
    nyms.put("antonyms", "ant");
    nyms.put("antonym", "ant");
    nyms.put("ant", "ant");
    nyms.put("hyponyms", "hypo");
    nyms.put("hypo", "hypo");
    nyms.put("hypernyms", "hyper");
    nyms.put("hyper", "hyper");
    nyms.put("holonyms", "holo");
    nyms.put("holo", "holo");
    nyms.put("hol", "holo");
    nyms.put("meronyms", "mero");
    nyms.put("mero", "mero");
    nyms.put("mer", "mero");
    nyms.put("comeronyms", "comero");
    nyms.put("troponyms", "tropo");
    nyms.put("coordinate terms", "cot");
    nyms.put("coordinate_terms", "cot");
    nyms.put("cot", "cot");
  }

  // PRONUNCIATION MODE
  private static final Pattern narrowDownIPAPattern = Pattern.compile("IPA\\(key\\):([^\n]*)");
  private static final Pattern pronunciationPattern = Pattern.compile("/[^/]*/|\\[[^\\]]*\\]");

  // MORPHOLOGY MODE
  private static final Supplier<EnglishInflectionData> plural =
      () -> new EnglishInflectionData().plural();
  private static final Supplier<EnglishInflectionData> singular =
      () -> new EnglishInflectionData().singular();
  private static final Supplier<EnglishInflectionData> feminine =
      () -> new EnglishInflectionData().feminine();
  private static final Supplier<EnglishInflectionData> masculine =
      () -> new EnglishInflectionData().masculine();
  private static final Supplier<EnglishInflectionData> comparative =
      () -> new EnglishInflectionData().comparative();
  private static final Supplier<EnglishInflectionData> superlative =
      () -> new EnglishInflectionData().superlative();
  private static final Supplier<EnglishInflectionData> pres3Sg =
      () -> new EnglishInflectionData().presentTense().thirdPerson().singular();
  private static final Supplier<EnglishInflectionData> presPtc =
      () -> new EnglishInflectionData().presentTense().participle();
  private static final Supplier<EnglishInflectionData> past =
      () -> new EnglishInflectionData().pastTense();
  private static final Supplier<EnglishInflectionData> pastPtc =
      () -> new EnglishInflectionData().pastTense().participle();

  private static final Map<String, BiConsumer<InflectedFormSet, String>> inflectionDecoder =
      new HashMap<>();

  static BiConsumer<InflectedFormSet, String> ppt = (forms, text) -> {
    forms.add(pastPtc.get(), text);
    forms.add(past.get(), text);
  };

  static {
    inflectionDecoder.put("comparative-form-of",
        (forms, text) -> forms.add(comparative.get(), text));
    inflectionDecoder.put("comparative", (forms, text) -> forms.add(comparative.get(), text));
    inflectionDecoder.put("superlative-form-of",
        (forms, text) -> forms.add(superlative.get(), text));
    inflectionDecoder.put("superlative", (forms, text) -> forms.add(superlative.get(), text));
    inflectionDecoder.put("p-form-of", (forms, text) -> forms.add(plural.get(), text));
    inflectionDecoder.put("plural", (forms, text) -> forms.add(plural.get(), text));
    inflectionDecoder.put("singular", (forms, text) -> forms.add(singular.get(), text));
    inflectionDecoder.put("feminine", (forms, text) -> forms.add(feminine.get(), text));
    inflectionDecoder.put("masculine", (forms, text) -> forms.add(masculine.get(), text));
    inflectionDecoder.put("3|s|pres-form-of", (forms, text) -> forms.add(pres3Sg.get(), text));
    inflectionDecoder.put("pres|ptcp-form-of", (forms, text) -> forms.add(presPtc.get(), text));
    inflectionDecoder.put("past|ptcp-form-of", (forms, text) -> forms.add(pastPtc.get(), text));
    inflectionDecoder.put("past-form-of", (forms, text) -> forms.add(past.get(), text));
    inflectionDecoder.put("past|and|past|ptcp-form-of", ppt);
    inflectionDecoder.put("simple past and past participle", ppt);

    // Do nothing actions
    inflectionDecoder.put("Latn", (forms, text) -> {
    });
    inflectionDecoder.put("lang-en", (forms, text) -> {
    });
    inflectionDecoder.put("form-of", (forms, text) -> {
    });
  }

  // EXAMPLE MODE
  private static final Pattern haniChars = Pattern.compile("\\p{IsHani}");

  private static boolean hasHaniChar(String text) {
    return haniChars.matcher(text).find();
  }

  private static final Pattern kanaChars = Pattern.compile("\\p{IsHira}|\\p{IsKana}");

  private static boolean hasKanaChar(String text) {
    return kanaChars.matcher(text).find();
  }

  private Mode currentMode = Mode.NONE;

  private final WiktionaryDataHandler delegate;
  private Set<Pair<Property, RDFNode>> context;
  private final ExpandAllWikiModel simpleExpander;
  private final Map<Pair<String, Map<String, String>>, MutableInt> citationCallCache =
      new HashMap<>();


  public CombinedWikiModel(WiktionaryDataHandler we, WiktionaryPageSource wi, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
    simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
  }

  /**
   * Parse a mediawiki string and register its plain text rendering as a new definition
   * 
   * @param definition the definition to parse
   * @param defLevel the level of the definition
   */
  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    // log.trace("extracting definitions in {}", this.getPageName());
    currentMode = Mode.DEFINITION;
    log.trace("Parsing definition : ||| {} ||| in {}", definition, delegate.currentPagename());
    String def = null;
    try {
      def = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
    }
    if (null != def && !def.isEmpty()) {
      delegate.registerNewDefinition(def, defLevel);
    }
    currentMode = Mode.NONE;
  }

  public void parsePronunciation(String pronTemplate) {
    // Render the pronunciation template to plain text then extract the computed pronunciation
    log.trace("Parsing pronunciation : ||| {} ||| in {}", pronTemplate, delegate.currentPagename());
    // Do not parse pronunciation for vietnamese entry as the pronunciation code is not
    // compatible with our Lua version and systematically raises an error
    Mode previousMode = currentMode; // the parse pronunciation may occur inside a Lua call
    currentMode = Mode.PRONUNCIATION;
    if ("vi".equals(delegate.getCurrentEntryLanguage()))
      return;
    String expandedPron = null;
    try {
      expandedPron = render(new PlainTextConverter(), pronTemplate).trim();
    } catch (IOException e) {
      log.error("Error while rendering pronunciation.", e);
    }
    if (null != expandedPron && !expandedPron.isEmpty()) {
      Scanner s = new Scanner(expandedPron);
      s.findAll(narrowDownIPAPattern).forEach(ipa -> {
        Scanner pronScanner = new Scanner(ipa.group(1));
        pronScanner.findAll(pronunciationPattern).forEach(mr -> {
          String pron = mr.group(0);
          delegate.registerPronunciation(pron, delegate.getCurrentEntryLanguage() + "-fonipa");
        });
      });

    }
    currentMode = previousMode;
  }

  public void parseMorphology(String morphTemplate) {
    currentMode = Mode.MORPHOLOGY;
    // Render the pronunciation template to plain text then extract the computed pronunciation
    log.trace("Parsing morphology : ||| {} ||| in {}", morphTemplate, delegate.currentPagename());
    String expandedMorph = expandWikiCode(morphTemplate);
    InflectedFormSet forms;
    if (morphTemplate.contains("{{en-")) {
      forms = parseEnTemplatesHTML(expandedMorph);
    } else if (morphTemplate.contains("{{head")) {
      forms = parseHeadHTML(expandedMorph);
    } else {
      log.debug("MORPH: unknown form of morph template call");
      forms = new InflectedFormSet();
    }
    // FOR NOUNS
    // TODO: scan forms to which one is missing (the canonical form) and specify number on
    // the canonical form
    // TODO: add countable feature on the entry if plural is present
    for (Entry<InflectionData, Set<String>> form : forms) {
      delegate.registerInflection(form.getKey(), form.getValue());
    }
    currentMode = Mode.NONE;
  }

  public void expandCitation(String definition, Set<Pair<Property, RDFNode>> context) {
    currentMode = Mode.EXAMPLE;
    this.context = context;
    String text = "";
    try {
      text = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
    }
    String textWithoutErrors = text.replaceAll("TemplateParserError:LuaError", "");
    if (!textWithoutErrors.equals(text)) {
      log.debug("LuaError while expanding citation in {}", getPageName());
    }
    if (!textWithoutErrors.trim().isEmpty()) {
      addNodeToContext(context, DCTerms.bibliographicCitation,
          rdfNode(textWithoutErrors, delegate.getCurrentEntryLanguage()));
    }
    this.context = null;
    currentMode = Mode.NONE;
  }

  public void expandExample(String definition, Set<Pair<Property, RDFNode>> context) {
    currentMode = Mode.EXAMPLE;
    this.context = context;
    String text = "";
    try {
      text = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
    }
    if (!text.trim().isEmpty()) {
      addNodeToContext(context, RDF.value, rdfNode(text, delegate.getCurrentEntryLanguage()));
    }
    this.context = null;
    currentMode = Mode.NONE;
  }


  // OVERRIDES

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    simpleExpander.setPageName(pageTitle);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    switch (currentMode) {
      case EXAMPLE:
        substituteExampleTemplate(templateName, parameterMap, writer);
        break;
      case DEFINITION:
        substituteDefinitionTemplate(templateName, parameterMap, writer);
        break;
      case MORPHOLOGY:
        substituteMorphologyTemplate(templateName, parameterMap, writer);
        break;
      case PRONUNCIATION:
        substitutePronunciationTemplate(templateName, parameterMap, writer);
        break;
      default:
        throw new IllegalStateException("Unexpected mode: " + currentMode);
    }
  }

  private void substitutePronunciationTemplate(String templateName,
      Map<String, String> parameterMap, Appendable writer) throws IOException {
    if (templateName.equals("IPA")) {
      String pronTemplate = parameterMap.get("2");
      if (pronTemplate != null) {
        // TODO: DO I NEED TO RENDER THE VALUE OR JUST REGISTER IT ?
        log.trace("IPA from Lua: {}", pronTemplate);
        parsePronunciation(pronTemplate);
      }
    }
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  private void substituteMorphologyTemplate(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  private void substituteExampleTemplate(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("ux".equals(templateName) || "usex".equals(templateName) || "eg".equals(templateName)
        || "uxi".equals(templateName) || "ux-lite".equals(templateName)
        || "quote".equals(templateName)) {
      // Simple usage example
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.getOrDefault("1", "").trim();
        if (!langCode.equalsIgnoreCase(delegate.getCurrentEntryLanguage())) {
          log.trace("UX Template: incoherent language code {} (expected {}) [{}]", langCode,
              delegate.getCurrentEntryLanguage(), getPageName());
        }
        // TODO: handle script code
        String scriptCode = parameterMap.get("sc");
        if (null != scriptCode) {
          log.trace("UX Template: unhanded script code {} [{}]", scriptCode, getPageName());
        }
      }
      String text = parameterMap.get("2");
      String translation = parameterMap.getOrDefault("t",
          parameterMap.getOrDefault("translation", parameterMap.get("3")));
      String transliteration = parameterMap.getOrDefault("tr", parameterMap.get("transliteration"));
      if (context != null) {
        if (null != text && !text.trim().isEmpty()) {
          addNodeToContext(context, RDF.value, rdfNode(text, delegate.getCurrentEntryLanguage()));
        }
        if (null != translation) {
          addNodeToContext(context, RDF.value,
              rdfNode(translation, delegate.getExtractedLanguage()));
        }
        if (null != transliteration) {
          addNodeToContext(context, RDF.value,
              rdfNode(transliteration, delegate.getCurrentEntryLanguage() + "-Latn"));
        }
      }
    } else if ("ja-usex".equals(templateName) || "ja-x".equals(templateName)
        || "ja-usex-inline".equals(templateName) || "ja-x-inline".equals(templateName)) {
      // Japanese usage example contains the japanese value + transliteration + translation
      String example = parameterMap.get("1");
      if (null == example || example.trim().isEmpty()) {
        return;
      }
      String transliteration = parameterMap.get("2");
      String translation = parameterMap.get("3");
      // The translation is at position 2 if the text only contains kana (hence no transliteration
      // necessary)
      if (hasHaniChar(example)) {
        if (null == transliteration || transliteration.trim().isEmpty()) {
          log.trace("JA-USEX Template: missing transliteration [{}]", getPageName());
          transliteration = null;
        }
        if (null == translation || translation.trim().isEmpty()) {
          log.trace("JA-USEX Template: missing translation [{}]", getPageName());
          translation = null;
        }
      } else if (hasKanaChar(example)) {
        if (null == transliteration || transliteration.trim().isEmpty()
            || !hasKanaChar(transliteration)) {
          translation = transliteration;
          transliteration = null;
        }
      } else {
        log.trace("JA-USEX Template: japanese example with no japanese char [{}]", getPageName());
        translation = example;
        example = null;
      }

      if (context != null) {
        if (null != example && !example.trim().isEmpty()) {
          addNodeToContext(context, RDF.value,
              rdfNode(example, delegate.getCurrentEntryLanguage()));
        }
        if (null != translation) {
          addNodeToContext(context, RDF.value,
              rdfNode(translation, delegate.getExtractedLanguage()));
        }
        if (null != transliteration) {
          addNodeToContext(context, RDF.value,
              rdfNode(transliteration, delegate.getCurrentEntryLanguage() + "-Kana"));
        }
      }
    } else if (nyms.containsKey(templateName)) {
      // HANDLE synonyms
      // TODO: also take gloss template into account as it gives more info on the nym
      String nym = nyms.get(templateName);
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.getOrDefault("1", "").trim();
        if (!langCode.equalsIgnoreCase(delegate.getCurrentEntryLanguage())) {
          log.trace("NYM Template: incoherent language code {} (expected {}) [{}]", langCode,
              delegate.getCurrentEntryLanguage(), getPageName());
        }
      }
      String val;
      for (int i = 2; (val = parameterMap.get(String.valueOf(i))) != null; i++) {
        if (val.contains(":")) {
          String prefix = StringUtils.substringBefore(val, ":");
          if (this.getNamespace().getNamespace(prefix) != null) {
            continue;
          }
        }
        delegate.registerNymRelationOnCurrentSense(val, nym);
      }
    } else if ("seeSynonyms".equals(templateName)) {
      // TODO: HANDLE synonyms in an external page
      log.trace("SHOULD WE HANDLE call: {} --in-- {}", templateName, this.getPageName());
    } else if ("seeCites".equals(templateName) || "seeMoreCites".equals(templateName)
        || "seemoreCites".equals(templateName)) {
      // TODO: HANDLE Citations that are given in another page
    } else if ("quote-book".equals(templateName) || "quote-journal".equals(templateName)
        || "quote-text".equals(templateName) || "quote-video game".equals(templateName)
        || "quote-web".equals(templateName)) {
      String passage = parameterMap.getOrDefault("passage", parameterMap.get("text"));
      // TODO: removing the passage from the text will generate a fake text in some cases
      if (null != passage && !passage.trim().isEmpty()) {
        parameterMap.remove("text");
        parameterMap.remove("passage");
      }
      parameterMap.remove("worklang");
      parameterMap.remove("termlang");
      parameterMap.remove("brackets");


      // If book is a translation, this will be the original text
      String origtext = parameterMap.get("origtext");
      parameterMap.remove("origtext");
      // Remove all other "original" parameters
      parameterMap.remove("orignorm");
      parameterMap.remove("origtr");
      parameterMap.remove("origsubst");
      parameterMap.remove("origts");
      parameterMap.remove("origsc");
      parameterMap.remove("orignormsc");
      parameterMap.remove("origtag");

      // In foreign (or old) quotes, the translation is given in the "t" parameter
      String translation = parameterMap.getOrDefault("t",
          parameterMap.getOrDefault("translation", parameterMap.get("8")));
      parameterMap.remove("translation");
      parameterMap.remove("t");
      parameterMap.remove("8");
      parameterMap.remove("lit");
      parameterMap.remove("footer");
      parameterMap.remove("tr");
      parameterMap.remove("transliteration");
      parameterMap.remove("norm");
      parameterMap.remove("normalization");
      parameterMap.remove("subst");
      parameterMap.remove("ts");
      parameterMap.remove("transcription");
      parameterMap.remove("sc");
      parameterMap.remove("normsc");
      parameterMap.remove("tag");

      Pair<String, Map<String, String>> count = Pair.of(templateName, parameterMap);
      citationCallCache.putIfAbsent(count, new MutableInt(0));
      citationCallCache.get(Pair.of(templateName, parameterMap)).increment();

      StringBuilder str = new StringBuilder();
      super.substituteTemplateCall(templateName, parameterMap, str);
      if (context != null) {
        if (null != passage && !passage.trim().isEmpty()) {
          addNodeToContext(context, RDF.value,
              rdfNode(passage, delegate.getCurrentEntryLanguage()));
        }
        String ref = StringUtils.strip(str.toString(), " \t\\x0B\f\n\r:");
        if (!ref.isEmpty()) {
          addNodeToContext(context, DCTerms.bibliographicCitation,
              rdfNode(ref, delegate.getExtractedLanguage()));
        }
      }
    } else if ("glossary".equals(templateName) || "glink".equals(templateName)) {
      String text = parameterMap.getOrDefault("2", parameterMap.get("1"));
      if (null != text) {
        writer.append(text);
      }
    } else if ("maintenance line".equals(templateName)) {
      // Just ignore maintenance lines
    } else if ("...".equals(templateName)) {
      writer.append(" […] ");
    } else if ("nb...".equals(templateName)) {
      writer.append("\u00A0[…]");
    } else if (templateName.startsWith("tracking/")) {
      // IGNORE
    } else if (templateName.equals("audio")) {
      // IGNORE
    } else {
      log.trace("Template call: {} --in-- {}", templateName, this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  // DEFINITION MODE OVERRIDES
  @SuppressWarnings("StatementWithEmptyBody")
  public void substituteDefinitionTemplate(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // TODO: the examples templates (e.g. ux) sometime appears in the same line as the definition.
    // Currently just expand the definition to get the full text.
    if (templateName.equals("label") || templateName.equals("lb") || templateName.equals("lbl")) {
      // intercept this template as it leads to a very inefficient Lua Script.
      writer.append("(");
      writer.append(parameterMap.get("2"));
      for (int i = 3; i < 9; i++) {
        String p = parameterMap.get(Integer.toString(i));
        // TODO: correctly handle comma in label construction
        if (null != p) {
          writer.append(", ");
          writer.append(p);
        }
      }
      writer.append(") ");
    } else if (templateName.equals("glossary")) {
      String text = parameterMap.get("2");
      if (null == text)
        text = parameterMap.get("1");
      writer.append(text);
    } else if (templateName.equals("gloss")) {
      String text = parameterMap.get("gloss");
      if (null == text)
        text = parameterMap.get("1");
      writer.append("(").append(text).append(")");
    } else if (templateName.equals("context") || templateName.equals("cx")) {
      log.debug("Obsolete Context template in {}", this.getPageName());
    } else if (templateName.equals("l") || templateName.equals("link") || templateName.equals("m")
        || templateName.equals("mention")) {
      String l = parameterMap.get("3");
      if (null == l) {
        l = parameterMap.get("2");
      }
      writer.append(l);
    } else if (templateName.equals("synonym of") || templateName.equals("ellipsis of")
        || templateName.equals("initialism of") || templateName.equals("init of")
        || templateName.equals("acronym of") || templateName.equals("abbr of")
        || templateName.equals("abbreviation of") || templateName.equals("abbrev of")
        || templateName.equals("clipping of") || templateName.equals("clip of")
        || templateName.equals("former name of")) {
      // TODO: handle synonym of by creating the appropriate synonymy relation.
      // catch and expand synonym of template before it is caught by next condition.
      super.substituteTemplateCall(templateName, parameterMap, writer);
    } else if (templateName.endsWith(" of")) {
      log.debug("Ignoring template {} in definition of {}", templateName, this.getPageName());
    } else if (ignoredTemplates.contains(templateName)) {

    } else if (templateName.equals("given name")) {
      writer.append(givenName(parameterMap));
    } else if (templateName.equals("quote-book")) {
      // TODO: example cannot be registered while transcluding as the lexical sense is not available
      // yet.
      // StringWriter quotation = new StringWriter();
      // super.substituteTemplateCall(templateName, parameterMap, quotation);
      // delegate.registerExample(quotation.toString(), null);
    } else if (templateName.equals("non-gloss definition") || templateName.equals("n-g")
        || templateName.equals("ngd") || templateName.equals("non-gloss")
        || templateName.equals("non gloss")) {
      String def = parameterMap.getOrDefault("1", "");
      writer.append(def);
    } else if (templateName.equals("senseid")) {
      // TODO: the sense is given a specific id that may be used in other steps
    } else {
      // log.debug("BEGIN >>> Subtituting template {} in page {}", templateName,
      // delegate.currentLexEntry());
      super.substituteTemplateCall(templateName, parameterMap, writer);
      // log.debug("END <<< Subtituting template {} in page {}", templateName,
      // delegate.currentLexEntry());
    }
  }

  // DEFINITION MODE LOCAL FUNCTIONS
  private String givenName(Map<String, String> parameterMap) {
    String gender = parameterMap.getOrDefault("1", parameterMap.getOrDefault("gender", ""));
    String article = parameterMap.get("A");
    if (null != article && article.isEmpty()) {
      article = null;
    }
    String or = parameterMap.get("or");
    String dimtype = parameterMap.get("dimtype");
    ArrayList<String> equivalents = listArgs(parameterMap, "eq");
    ArrayList<String> diminutives = listArgs(parameterMap, "dim");
    if (diminutives.isEmpty()) {
      diminutives = listArgs(parameterMap, "diminutive");
    }
    // TODO: there is sometimes the origin of the given name (e.g. a Japanese male given name)
    StringBuilder result = new StringBuilder();
    if (null == article) {
      result.append("A ");
    } else {
      result.append(article).append(" ");
    }
    if (!diminutives.isEmpty()) {
      if (null != dimtype) {
        result.append(dimtype);
        result.append(" ");
      }
      result.append("diminutive of the ");
    }
    result.append(gender).append(" ");
    if (null != or) {
      result.append("or ").append(or).append(" ");
    }
    result.append("given name");
    if (diminutives.size() > 1) {
      result.append("s");
    }
    appendList(result, diminutives, " ", "");
    appendList(result, equivalents, ", equivalent to English ", "");
    return result.toString();
  }

  private void appendList(StringBuilder res, ArrayList<String> list, String before, String after) {
    if (!list.isEmpty()) {
      res.append(before);
      res.append(list.get(0));
      for (int i = 1; i < list.size(); i++) {
        if (i == list.size() - 1) {
          res.append(" or ");
        } else {
          res.append(", ");
        }
        res.append(list.get(i));
      }
      res.append(after);
    }
  }

  private ArrayList<String> listArgs(Map<String, String> args, String arg) {
    ArrayList<String> res = new ArrayList<>();
    String eq = args.get(arg);
    int i = 2;
    while (null != eq) {
      res.add(eq);
      eq = args.get(arg + i);
      i++;
    }
    return res;
  }

  // MORPHOLOGY MODE LOCAL FUNCTIONS
  private InflectedFormSet parseHeadHTML(String htmlCode) {
    Document doc = Jsoup.parse(htmlCode);

    InflectedFormSet forms = new InflectedFormSet();
    Elements elts = doc.select("strong.headword, i, b>a");
    String currentInfl = "";
    for (Element elt : elts) {
      if (elt.tagName().equalsIgnoreCase("i")) {
        String flex = elt.text().trim();
        if (!flex.equals("or") && !flex.equals("and")) {
          currentInfl = flex;
        }
      } else if (elt.tagName().equalsIgnoreCase("a")) {

        String href = elt.attr("href").trim();
        if (!href.startsWith("/Appendix:Glossary#")) {
          inflectionFromClass(currentInfl, elt.text(), forms);
        }
      } else if (elt.hasClass("headword")) {
        if (!getPageName().equals(elt.text())) {
          log.debug("MORPH: headword `{}` is not pagename `{}`", elt.text(), getPageName());
        }
      }
    }
    return forms;
  }

  public InflectedFormSet parseEnTemplatesHTML(String htmlCode) {
    Document doc = Jsoup.parse(htmlCode);

    InflectedFormSet forms = new InflectedFormSet();

    String note = null;
    Elements elts =
        doc.select("strong.headword, b.form-of, a, span.qualifier-content, span.ib-content");
    for (Element elt : elts) {
      if (elt.tagName().equalsIgnoreCase("a")) {
        String href = elt.attr("href").trim();
        if (href.startsWith("/Appendix:Glossary#")) {
          String concept = href.substring(1 + href.indexOf('#'));
          if (concept.equals("countable")) {
            delegate.countable();
          } else if (concept.equals("uncountable")) {
            delegate.uncountable();
          } else {
            if (!"comparative".equals(concept) && !"superlative".equals(concept)
                && !"comparable".equals(concept))
              log.trace("MORPH: Ignoring glossary term `{}` in `{}`", concept, getPageName());
          }
        }
      } else if (elt.hasClass("headword")) {
        String text = elt.text().trim();
        if (!getPageName().equals(text)) {
          delegate.addWrittenRep(text);
          log.debug("MORPH: headword `{}` is not pagename `{}`", text, getPageName());
        }
      } else if (elt.hasClass("qualifier-content") || elt.hasClass("ib-content")) {
        note = elt.text().trim();
      } else {
        InflectedFormSet newForms = new InflectedFormSet();
        addInflexions(elt, elt.text(), newForms);
        if (null != note) {
          final String finalNote = note;
          newForms.forEach(e -> ((EnglishInflectionData) e.getKey()).note(finalNote));
          // Assume a note only concerns the following form
          note = null;
        }
        newForms.forEach(e -> forms.add(e.getKey(), e.getValue()));
      }
    }
    return forms;
  }


  private void addInflexions(Element elt, String text, InflectedFormSet forms) {
    for (String clazz : elt.classNames()) {
      clazz = clazz.trim();
      inflectionFromClass(clazz, text, forms);
    }
  }

  private void inflectionFromClass(String clazz, String text, InflectedFormSet forms) {
    BiConsumer<InflectedFormSet, String> action = inflectionDecoder.get(clazz);
    if (null != action) {
      action.accept(forms, text);
    } else {
      log.debug("MORPH: unknown class `{}` for value `{}` in `{}`", clazz, text, getPageName());
    }
  }

  // EXAMPLE MODE LOCAL FUNCTIONS
  private Literal rdfNode(String value, String lang) {
    return rdfNode(value, lang, true);
  }

  private Literal rdfNode(String value, String lang, boolean expand) {
    String val = expand ? expandString(value) : value;
    if (null == val || val.trim().isEmpty()) {
      return null;
    }
    return ResourceFactory.createLangLiteral(val, lang);
  }

  private String expandString(String originalSource) {
    // Expand source to properly resolve links and cosmetic markups
    String expanded = simpleExpander.expandAll(originalSource, this.templates);
    return expanded.replaceAll("TemplateParserError:LuaError", "");
  }

  @Override
  public void addCategory(String categoryName, String sortKey) {
    log.trace("Called addCategory : " + categoryName);
    super.addCategory(categoryName, sortKey);
  }

  private void addNodeToContext(Set<Pair<Property, RDFNode>> context, Property prop,
      Literal rdfNode) {
    if (null != rdfNode) {
      context.add(Pair.of(prop, rdfNode));
    }
  }

  @Override
  public void displayGlobalTrace(String msg) {
    super.displayGlobalTrace(msg);
    IntSummaryStatistics stats = citationCallCache.values().stream()
        .collect(Collectors.summarizingInt(MutableInt::intValue));
    log.trace(
        "CACHE STATS: {} citation calls ({} uniques) with average duplicate of {} (min: {} / max: {})",
        stats.getSum(), stats.getCount(), stats.getAverage(), stats.getMin(), stats.getMax());
    citationCallCache.entrySet().stream().filter(e -> e.getValue().intValue() > 1)
        .forEach(e -> log.trace("CACHE: {} citation call for {} with parameters {}",
            e.getValue().intValue(), e.getKey(), e.getValue().toString().replaceAll("[\r\n]", "")));
  }
}
