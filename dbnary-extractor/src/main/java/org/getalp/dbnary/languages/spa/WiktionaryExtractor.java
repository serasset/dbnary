package org.getalp.dbnary.languages.spa;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  // TODO: {{grafía alternativa|hogaño|nota1=más usada|leng=es}}: 2450 résultats, 500 pour *
  // '''Variante''', {{variantes si la prononciation est différente}}.

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString;
  protected final static String headerPatternString;
  protected final static String spanishDefinitionPatternString;
  private final WiktionaryDataHandler spaWdh;

  private enum EXTRACTION_STATE {
    NODATA, TRADBLOCK, DEFBLOCK, HEADERBLOCK, IGNOREPOS, ETYMOLOGY
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    assert wdh instanceof WiktionaryDataHandler;
    spaWdh = (WiktionaryDataHandler) wdh;
  }


  protected final static String sectionPatternString;
  // protected final static Pattern languageSectionPattern;
  protected final static Pattern spanishDefinitionPattern;
  protected final static String multilineMacroPatternString;
  protected final static Pattern sectionPattern; // Combine macro pattern
  // and pos pattern.
  protected final static HashSet<String> posMarkersPrefixes;
  protected final static HashSet<String> ignorableSectionMarkers;
  protected final static HashMap<String, String> nymMarkerToNymName;

  protected SpanishDefinitionExtractorWikiModel definitionExpander;
  protected SpanishHeaderExtractorWikiModel headerExtractor;
  protected SpanishTranslationExtractorWikiModel translationExtractor;
  protected ExpandAllWikiModel exampleExpander;

  static {

    languageSectionPatternString = "(?:" + "^\\s*\\{\\{" //
        + "([\\p{Upper}\\-]*)(?:\\|([^}]*))?" //
        + "\\}\\}" //
        + ")|(?:" //
        + "^==\\s*\\{\\{lengua\\|(.*)}}\\s*==\\s*$"//
        + ")";

    // languageSectionPattern = Pattern.compile(languageSectionPatternString, Pattern.MULTILINE);
    multilineMacroPatternString = "\\{\\{" + "([^\\}\\|]*)(?:\\|([^\\}]*))?" //
        + "\\}\\}";

    headerPatternString = "(?:" + "^==([^=].*)==\\s*$" //
        + ")|(?:" + "^===([^=].*)===\\s*$" + ")|(?:" //
        + "^====([^=].*)====\\s*$" + ")";

    sectionPatternString = "(?m)(?:" + headerPatternString + ")";

    sectionPattern = Pattern.compile(sectionPatternString);

    spanishDefinitionPatternString = "(?:" + "^;([^:]*):([^\n\r]*)$" + ")|(?:"
        + WikiPatterns.macroPatternString + ")|(?:" + "^:([^\n\r]*)$" + ")";
    spanishDefinitionPattern = Pattern.compile(spanishDefinitionPatternString, Pattern.MULTILINE);

    posMarkersPrefixes = new HashSet<>(20);
    posMarkersPrefixes.add("sustantivo");
    posMarkersPrefixes.add("adjetivo");
    posMarkersPrefixes.add("sustantivo propio");
    posMarkersPrefixes.add("adverbio");
    posMarkersPrefixes.add("verbo");
    posMarkersPrefixes.add("interjección");

    // posMarkersPrefixes.add("adjetivo cardinal");
    // posMarkersPrefixes.add("adjetivo demostrativo");
    // posMarkersPrefixes.add("adjetivo indefinido");
    // posMarkersPrefixes.add("adjetivo indeterminado");
    // posMarkersPrefixes.add("adjetivo interrogativo");
    // posMarkersPrefixes.add("adjetivo numeral");
    // posMarkersPrefixes.add("adjetivo ordinal");
    // posMarkersPrefixes.add("adjetivo posesivo");
    // posMarkersPrefixes.add("adjetivo relativo");
    // posMarkersPrefixes.add("adverbio comparativo");
    // posMarkersPrefixes.add("adverbio de afirmación");
    // posMarkersPrefixes.add("adverbio de cantidad");
    // posMarkersPrefixes.add("adverbio de duda");
    // posMarkersPrefixes.add("adverbio de lugar");
    // posMarkersPrefixes.add("adverbio de modo");
    // posMarkersPrefixes.add("adverbio de negación");
    // posMarkersPrefixes.add("adverbio de orden");
    // posMarkersPrefixes.add("adverbio de tiempo");
    // posMarkersPrefixes.add("adverbio demostrativo");
    // posMarkersPrefixes.add("adverbio interrogativo");
    // posMarkersPrefixes.add("adverbio relativo");
    posMarkersPrefixes.add("afijo");
    posMarkersPrefixes.add("artículo");
    // posMarkersPrefixes.add("artículo determinado");
    // posMarkersPrefixes.add("artículo indeterminado");

    posMarkersPrefixes.add("circunfijo");
    posMarkersPrefixes.add("conjunción");
    // posMarkersPrefixes.add("conjunción adversativa");
    // posMarkersPrefixes.add("conjunción ilativa");
    posMarkersPrefixes.add("determinante");
    posMarkersPrefixes.add("dígrafo");
    posMarkersPrefixes.add("expresión");
    posMarkersPrefixes.add("letra");
    posMarkersPrefixes.add("locución");
    // posMarkersPrefixes.add("locución adjetiva");
    // posMarkersPrefixes.add("locución adverbial");
    // posMarkersPrefixes.add("locución conjuntiva");
    // posMarkersPrefixes.add("locución interjectiva");
    // posMarkersPrefixes.add("locución prepositiva");
    // posMarkersPrefixes.add("locución pronominal");
    // posMarkersPrefixes.add("locución sustantiva");
    // posMarkersPrefixes.add("locución verbal");
    posMarkersPrefixes.add("onomatopeya");
    posMarkersPrefixes.add("partícula");
    posMarkersPrefixes.add("postposición");
    posMarkersPrefixes.add("prefijo");
    posMarkersPrefixes.add("preposición");
    // posMarkersPrefixes.add("preposición de ablativo");
    // posMarkersPrefixes.add("preposición de acusativo");
    // posMarkersPrefixes.add("preposición de acusativo o ablativo");
    // posMarkersPrefixes.add("preposición de genitivo");
    posMarkersPrefixes.add("pronombre");
    // posMarkersPrefixes.add("pronombre demostrativo");
    // posMarkersPrefixes.add("pronombre indefinido");
    // posMarkersPrefixes.add("pronombre interrogativo");
    // posMarkersPrefixes.add("pronombre personal");
    // posMarkersPrefixes.add("pronombre posesivo");
    // posMarkersPrefixes.add("pronombre relativo");
    posMarkersPrefixes.add("refrán");
    posMarkersPrefixes.add("sigla");
    posMarkersPrefixes.add("sufijo");
    // posMarkersPrefixes.add("sufijo flexivo");
    // posMarkersPrefixes.add("sustantivo ambiguo");
    // posMarkersPrefixes.add("sustantivo animado");
    // posMarkersPrefixes.add("sustantivo común");
    // posMarkersPrefixes.add("sustantivo femenino");
    // posMarkersPrefixes.add("sustantivo femenino y masculino");
    // posMarkersPrefixes.add("sustantivo inanimado");
    // posMarkersPrefixes.add("sustantivo masculino");
    // posMarkersPrefixes.add("sustantivo neutro");
    // posMarkersPrefixes.add("sustantivo neutro y masculino");
    // posMarkersPrefixes.add("sustantivo propio/pruebas");
    posMarkersPrefixes.add("símbolo");
    // posMarkersPrefixes.add("verbo auxiliar");
    // posMarkersPrefixes.add("verbo bitransitivo");
    // posMarkersPrefixes.add("verbo enclítico");
    // posMarkersPrefixes.add("verbo impersonal");
    // posMarkersPrefixes.add("verbo intransitivo");
    // posMarkersPrefixes.add("verbo modal");
    // posMarkersPrefixes.add("verbo perfectivo");
    // posMarkersPrefixes.add("verbo pronominal");
    // posMarkersPrefixes.add("verbo transitivo");

    ignorableSectionMarkers = new HashSet<>(20);
    ignorableSectionMarkers.add("Silbentrennung");
    ignorableSectionMarkers.add("Aussprache");
    ignorableSectionMarkers.add("Herkunft");
    ignorableSectionMarkers.add("Gegenworte");
    ignorableSectionMarkers.add("Beispiele");
    ignorableSectionMarkers.add("Redewendungen");
    ignorableSectionMarkers.add("Dialektausdrücke (Deutsch)");
    ignorableSectionMarkers.add("Referenzen");
    ignorableSectionMarkers.add("Ähnlichkeiten");
    ignorableSectionMarkers.add("Anmerkung");
    ignorableSectionMarkers.add("Anmerkungen");
    ignorableSectionMarkers.add("Alte Rechtschreibung"); // TODO: Integrate
    // these in
    // alternative
    // spelling ?
    ignorableSectionMarkers.add("Nebenformen");
    ignorableSectionMarkers.add("Vokalisierung");
    ignorableSectionMarkers.add("Grammatische Merkmale");
    ignorableSectionMarkers.add("Abkürzungen"); // TODO: Integrate these in
    // alternative spelling ?
    ignorableSectionMarkers.add("Sinnverwandte Wörter"); // TODO: related
    // words (should I
    // keep it ?)
    ignorableSectionMarkers.add("Weibliche Wortformen");
    ignorableSectionMarkers.add("Männliche Wortformen");
    ignorableSectionMarkers.add("Verkleinerungsformen"); // TODO:
    // Diminutif...
    // qu'en faire ?
    ignorableSectionMarkers.add("Vergrößerungsformen");
    ignorableSectionMarkers.add("Kurzformen");
    ignorableSectionMarkers.add("Koseformen");
    ignorableSectionMarkers.add("Kurz- und Koseformen");
    ignorableSectionMarkers.add("Namensvarianten");
    ignorableSectionMarkers.add("Weibliche Namensvarianten");
    ignorableSectionMarkers.add("Männliche Namensvarianten");
    ignorableSectionMarkers.add("Bekannte Namensträger");
    ignorableSectionMarkers.add("Sprichwörter");
    ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
    ignorableSectionMarkers.add("Abgeleitete Begriffe");

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("sinónimo", "syn");
    nymMarkerToNymName.put("sinónimos", "syn");
    nymMarkerToNymName.put("antónimo", "ant");
    nymMarkerToNymName.put("antónimos", "ant");
    nymMarkerToNymName.put("hipónimo", "hypo");
    nymMarkerToNymName.put("hipónimos", "hypo");
    nymMarkerToNymName.put("hiperónimo", "hyper");
    nymMarkerToNymName.put("hiperónimos", "hyper");

  }


  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExpander = new SpanishDefinitionExtractorWikiModel(this.wdh, this.wi,
        new Locale("es"), "/${image}", "/${title}");
    headerExtractor = new SpanishHeaderExtractorWikiModel(this.wdh, this.wi, new Locale("es"),
        "/${image}", "/${title}");
    translationExtractor = new SpanishTranslationExtractorWikiModel(this.wdh, this.wi,
        new Locale("es"), "/${image}", "/${title}");
    exampleExpander = new ExpandAllWikiModel(wi, new Locale("es"), "/${image}", "/${title}");
  }


  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    extractData(page);
    wdh.finalizePageExtraction();
  }

  public void extractData(WikiText page) {
    WikiEventsSequence tokens =
        page.filteredTokens(new ClassBasedFilter().allowHeading().allowTemplates());
    int startOfLgSection = -1;
    String sectionLanguage = null;
    for (Token t : tokens) {
      String lg;
      if (null != (lg = languageHeading(t))) {
        if (-1 != startOfLgSection) {
          extractLanguageData(startOfLgSection, t.getBeginIndex(), sectionLanguage);
        }
        startOfLgSection = t.getEndIndex();
        sectionLanguage = lg;
      }
    }
    if (-1 != startOfLgSection) {
      extractLanguageData(startOfLgSection, pageContent.length(), sectionLanguage);
    }
  }

  private static final Pattern obsoleteLanguageTmplPattern =
      Pattern.compile("(\\p{Upper}{2,3})-ES");
  private static final Matcher obsoleteLanguage = obsoleteLanguageTmplPattern.matcher("");

  /**
   * return the language code iff the token represent a header for a language section. return null
   * in all other cases. returns "" if the language code is unknown but a language section is found.
   *
   * @param t : the token to be checked and from which the language should be extracted
   * @return the language code or null
   */
  private String languageHeading(Token t) {
    if (t instanceof Template) {
      if ("ES".equals(t.asTemplate().getName())) {
        log.trace("LANGUAGE_HEADING: ES template found in {}", getWiktionaryPageName());
        return "es";
      }
      obsoleteLanguage.reset(t.asTemplate().getName());
      if (obsoleteLanguage.matches()) {
        String lg = obsoleteLanguage.group(1);
        log.trace("LANGUAGE_HEADING: Obsolete language template {} found in {}", lg,
            getWiktionaryPageName());
        return lg.toLowerCase();
      }
    } else if (t instanceof Heading) {
      Heading h = t.asHeading();
      // Look for the language template
      Optional<Template> lt =
          h.getContent().wikiTokens().stream().filter(tok -> tok instanceof Template)
              .map(Token::asTemplate).filter(tok -> "lengua".equals(tok.getName())).findFirst();
      if (lt.isPresent()) {
        String lg = lt.get().getParsedArg("1");
        if (null != lg) {
          return lg.toLowerCase();
        } else {
          log.debug("lengua template with no language code {} in {}", lt, getWiktionaryPageName());
          return "";
        }
      } else {
        if (h.getLevel() == 2) {
          log.trace("lvl 2 heading with no language {} in {}", h, getWiktionaryPageName());
        }
        return null;
      }
    }
    return null;
  }


  private EXTRACTION_STATE state = EXTRACTION_STATE.NODATA;
  private int definitionBlockStart = -1;
  private int translationBlockStart = -1;
  private int translationLevel = -1;
  private int headerBlockStart = -1;
  private int etymologyBlockStart = -1;


  void gotoNoData(Matcher m) {
    state = EXTRACTION_STATE.NODATA;
  }

  private int getHeaderLevel(Matcher m) {
    String l2 = m.group(1);
    String l3 = m.group(2);
    String l4 = m.group(3);

    if (l2 != null) {
      return 2;
    }
    if (l3 != null) {
      return 3;
    }
    if (l4 != null) {
      return 4;
    }
    return 0;
  }

  private String getHeaderLabel(Matcher m) {
    String l2 = m.group(1);
    String l3 = m.group(2);
    String l4 = m.group(3);

    if (l2 != null) {
      return l2;
    }
    if (l3 != null) {
      return l3;
    }
    return l4;
  }

  private boolean isHeader(Matcher m) {
    return getHeaderLabel(m) != null;
  }

  private boolean isEtymology(Matcher m) {
    String headerLabel = getHeaderLabel(m);
    if (null == headerLabel)
      return false;
    return headerLabel.toLowerCase().trim().startsWith("etimología");
  }

  // Part of speech section (Def block)

  private String getValidPOS(Matcher m) {
    String h = getHeaderLabel(m);
    if (null == h) {
      return null;
    }
    int l = getHeaderLevel(m);
    if (l != 3 && l != 4) {
      return null; // Only keep lvl 3 or 4 headings...
    }
    String head = h.trim().toLowerCase();
    if ((head.startsWith("forma")) || head.startsWith("{{forma")) {
      return "";
    }
    if (head.contains("{{")) {
      WikiText complexHeading = new WikiText(head);
      Optional<Template> posTmpl =
          complexHeading.wikiTokens().stream().filter(tok -> tok instanceof Template)
              .map(Token::asTemplate).filter(tok -> posMarkersPrefixes.stream()
                  .anyMatch(prefix -> tok.getName().startsWith(prefix)))
              .findFirst();
      if (posTmpl.isPresent()) {
        return posTmpl.get().getName();
      }
    }
    return posMarkersPrefixes.stream().anyMatch(head::startsWith) ? head : null;
  }

  void gotoDefBlock(Matcher m, String pos) {
    state = EXTRACTION_STATE.DEFBLOCK;
    definitionBlockStart = m.end();
    wdh.initializeLexicalEntry(pos);
  }

  void leaveDefBlock(Matcher m) {
    int end = computeRegionEnd(definitionBlockStart, m);
    extractDefinitions(definitionBlockStart, end);
    definitionBlockStart = -1;
  }

  // Translation section
  private boolean isTranslation(Matcher m) {
    String head = getHeaderLabel(m);
    if (null == head) {
      return false;
    }
    int lvl = getHeaderLevel(m);
    if (lvl != 2 && lvl != 3 && lvl != 4) {
      return false;
    }
    head = head.trim().toLowerCase();
    if (log.isTraceEnabled() && head.contains("traducci")) {
      log.trace("Translation header found in {}", getWiktionaryPageName());
    }
    return "traducciones".equals(head) || "traducción".equals(head);
  }

  void gotoTradBlock(Matcher m) {
    translationBlockStart = m.end();
    translationLevel = getHeaderLevel(m);
    state = EXTRACTION_STATE.TRADBLOCK;
  }

  void leaveTradBlock(Matcher m) {
    extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m),
        translationLevel);
    translationBlockStart = -1;
    translationLevel = -1;
  }

  private void gotoHeaderBlock(Matcher m) {
    state = EXTRACTION_STATE.HEADERBLOCK;
    // The header starts at the beginning of the region.
    headerBlockStart = m.regionStart();
  }

  private void leaveHeaderBlock(Matcher m) {
    extractHeaderInfo(headerBlockStart, computeRegionEnd(headerBlockStart, m));
    headerBlockStart = -1;
  }

  private void gotoEtymologyBlock(Matcher m) {
    state = EXTRACTION_STATE.ETYMOLOGY;
    // The header starts at the beginning of the region.
    etymologyBlockStart = m.regionStart();
    spaWdh.startNewEtymologySection();
  }

  private void leaveEtymologyBlock(Matcher m) {
    spaWdh.extractEtymology();
    etymologyBlockStart = -1;
  }

  private void gotoIgnorePos() {
    state = EXTRACTION_STATE.IGNOREPOS;
  }

  // TODO: variants, pronunciations and other elements are common to the different entries in the
  // page.
  private void extractLanguageData(int startOffset, int endOffset, String language) {
    if (null == language || language.isEmpty()) {
      return;
    }
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !"es".equals(language)) {
      return;
    }
    String normalizedLanguage = validateAndStandardizeLanguageCode(language);
    if (normalizedLanguage == null) {
      log.trace("Ignoring language section {} for {}", language, getWiktionaryPageName());
      return;
    }

    wdh.initializeLanguageSection(language);
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    gotoHeaderBlock(m);
    String pos;
    while (m.find()) {
      switch (state) {
        case NODATA:
          if (isTranslation(m)) {
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            // Level 2 header that are not a correct POS, or Pronunciation are
            // considered as ignorable POS.
            // unknownHeaders.add(m.group(0));
            gotoNoData(m);
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        case DEFBLOCK:
          // Iterate until we find a new section
          if (isTranslation(m)) {
            leaveDefBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            leaveDefBlock(m);
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            leaveDefBlock(m);
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            leaveDefBlock(m);
            gotoNoData(m);
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        case TRADBLOCK:
          if (isTranslation(m)) {
            leaveTradBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            leaveTradBlock(m);
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            leaveTradBlock(m);
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            leaveTradBlock(m);
            gotoNoData(m);
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        case HEADERBLOCK:
          if (isTranslation(m)) {
            leaveHeaderBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            leaveHeaderBlock(m);
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            leaveHeaderBlock(m);
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            leaveHeaderBlock(m);
            gotoNoData(m);
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        case IGNOREPOS:
          if (isTranslation(m)) {
          } else if (null != (pos = getValidPOS(m))) {
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            // gotoIgnorePos();
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        case ETYMOLOGY:
          if (isTranslation(m)) {
            leaveEtymologyBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            leaveEtymologyBlock(m);
            if (pos.isEmpty()) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isEtymology(m)) {
            leaveEtymologyBlock(m);
            gotoEtymologyBlock(m);
          } else if (isHeader(m)) {
            leaveEtymologyBlock(m);
            gotoNoData(m);
          } else {
            // unknownHeaders.add(m.group(0));
          }
          break;
        default:
          assert false : "Unexpected state while extracting translations from dictionary.";
      }
    }
    // Finalize the entry parsing
    switch (state) {
      case NODATA:
        break;
      case DEFBLOCK:
        leaveDefBlock(m);
        break;
      case TRADBLOCK:
        leaveTradBlock(m);
        break;
      case HEADERBLOCK:
        leaveHeaderBlock(m);
        break;
      case IGNOREPOS:
        break;
      default:
        assert false
            : "Unexpected state while ending extraction of entry: " + getWiktionaryPageName();
    }
    wdh.finalizeLanguageSection();
  }


  private void extractTranslations(int startOffset, int endOffset, int translationLevel) {
    if (log.isTraceEnabled()) {
      log.trace("TranslationLevel = {} in {}", translationLevel, getWiktionaryPageName());
    }
    // TODO: maybe take the translation level into account (see issue #87)
    String transCode = pageContent.substring(startOffset, endOffset);
    translationExtractor.setPageName(getWiktionaryPageName());
    translationExtractor.parseTranslationBlock(transCode);
  }

  private static final Pattern senseNumPattern = Pattern.compile("(\\d+)");
  private static final Pattern nonMacroRelationPattern =
      Pattern.compile("\\*\\s*'''([^']*)'''(.*)$");
  private static final Pattern exampleTraductionPattern =
      Pattern.compile("(?:traducción|trad)=([^\r\n}]*)");

  @Override
  protected void extractDefinitions(int startOffset, int endOffset) {
    Matcher definitionMatcher = spanishDefinitionPattern.matcher(this.pageContent);
    definitionMatcher.region(startOffset, endOffset);

    while (definitionMatcher.find()) {
      if (definitionMatcher.group(1) != null) {
        // Get a new definition
        String def = definitionMatcher.group(2);
        String senseNumber = "";
        String term = definitionMatcher.group(1);
        if (null == term || term.isEmpty()) {
          log.debug("Null sense number in definition\"{}\" for entry {}", def,
              this.getWiktionaryPageName());
        } else {
          term = term.trim();
          Matcher s = senseNumPattern.matcher(term);
          if (s.find()) {
            if (s.start() != 0) {
              log.debug("Sense number match after beginning of region \"{}\" for entry {}", term,
                  this.getWiktionaryPageName());
              def = term + "| " + def;
            } else {
              senseNumber = s.group(1);
              term = term.substring(s.end());
              term = term.trim();
              if (!term.isEmpty()) {
                def = term + "| " + def;
              }
            }
          } else {
            log.debug("No sense number in region \"{}\" for entry {}", term,
                this.getWiktionaryPageName());
            def = term + "| " + def;
          }

          if (def != null && !def.equals("")) {
            extractDefinition(def, senseNumber);
          }
        }
      } else if (definitionMatcher.group(3) != null) {
        // It's a macro not inside a definition...
        String macro = definitionMatcher.group(3);
        String nym;
        if (null != (nym = nymMarkerToNymName.get(macro))) {
          Map<String, String> args = WikiTool.parseArgs(definitionMatcher.group(4));
          for (int i = 1; i <= 40; i++) {
            String val = args.get("" + i);
            String nota = args.get("nota" + i);
            String alt = args.get("alt" + i);
            if (val != null) {
              wdh.registerNymRelationOnCurrentSense(val, nym);
              if (null != nota || null != alt) {
                log.debug("Non null alternate/nota \"{}/{}\"on nym relation for entry {}", nota,
                    alt, this.getWiktionaryPageName());
              }
            }
          }
          // TODO: several other nyms may be found on the line, after the macro...
          int offset = definitionMatcher.end();
          while (offset != endOffset && this.pageContent.charAt(offset) != '\n'
              && this.pageContent.charAt(offset) != '\r') {
            offset++;
          }
          String vals = this.pageContent.substring(definitionMatcher.end(), offset);
          extractNyms(nym, vals);
          definitionMatcher.region(offset, endOffset);
        }
        if (macro.equals("ejemplo")) {
          Map<String, String> args = WikiTool.parseArgs(definitionMatcher.group(4));
          String example = args.get("1");
          example = exampleExpander.expandAll(example, null);
          if (example != null) {
            Set<Pair<Property, RDFNode>> context = new HashSet<>();
            Matcher exTradMatch = exampleTraductionPattern.matcher(definitionMatcher.group());
            String ref = null;
            String exampleLangTrad = null;

            if (example.contains(" – ")){
              log.trace("Example: traduction possible ;{}; found in {}",example, getWiktionaryPageName());
            }

            if (exTradMatch.find()) {
              ref = definitionMatcher.group().replace("|" + exTradMatch.group(), "");
              exampleLangTrad = exampleExpander.expandAll(exTradMatch.group(1), null);
            } else {
              ref = definitionMatcher.group();
            }

            ref = exampleExpander.expandAll(ref, null);
            ref = ref.replace("Ejemplo:", "");
            ref = ref.replace(example, "").trim();
            if (ref != null && !ref.isEmpty()) {
              context.add(Pair.of(DCTerms.bibliographicCitation,
                  ResourceFactory.createLangLiteral(ref, wdh.getCurrentEntryLanguage())));
            }
            if (exampleLangTrad != null && !exampleLangTrad.isEmpty()) {
              context.add(
                  Pair.of(RDF.value, ResourceFactory.createLangLiteral(exampleLangTrad, "es")));
            }
            wdh.registerExample(example, context);
          }
        }
      } else if (definitionMatcher.group(5) != null) {
        Matcher m = nonMacroRelationPattern.matcher(definitionMatcher.group(5));
        if (m.matches()) {
          String rel = m.group(1);
          rel = rel.replace(":", "").toLowerCase();
          String nym = nymMarkerToNymName.get(rel);
          String vals = m.group(2);
          if (null != nym) {
            extractNyms(nym, vals);
          }
        }
      }
    }
  }

  public void extractDefinition(String definition, String senseNumber) {
    definitionExpander.setPageName(getWiktionaryPageName());
    exampleExpander.setPageName(getWiktionaryPageName());
    definitionExpander.parseDefinition(definition, senseNumber);
  }

  Pattern nymValuesPattern = Pattern
      .compile("(?:" + WikiPatterns.linkPatternString + ")|(?:" + "(\\([^\\)]*\\))" + ")|(,)");

  private void extractNyms(String nym, String vals) {
    Matcher m = nymValuesPattern.matcher(vals);
    while (m.find()) {
      if (m.group(1) != null) {
        // Link
        wdh.registerNymRelationOnCurrentSense(m.group(1), nym);
      } else if (m.group(3) != null) {
        // Parens : just ignore it
      } else if (m.group(4) != null) {
        // Comma
      }
    }
  }

  private void extractHeaderInfo(int startOffset, int endOffset) {
    String transCode = pageContent.substring(startOffset, endOffset);
    headerExtractor.setPageName(getWiktionaryPageName());
    headerExtractor.parseHeaderBlock(transCode);
  }

}
