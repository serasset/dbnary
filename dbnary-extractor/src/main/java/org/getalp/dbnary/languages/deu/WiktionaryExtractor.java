package org.getalp.dbnary.languages.deu;

import java.util.List;
import org.getalp.dbnary.ExtractionFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPattern;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String senseNumberRegExp =
      "(?:(?:(?:<tt>)?[IV]+(?:</tt>)?|\\d)*\\.?[abcdefghijklmn]?)";
  protected final static String senseNumberOrRangeRegExp =
      "(?:(?:(?:<tt>)?[IV]+(?:</tt>)?|\\d|-|\u2013|,| |&nbsp;)*\\.?[abcdefghij]?)"; // long dash


  protected final static String languageSectionPatternString =
      "={2}\\s*([^(\r\n]*)\\(\\{\\{Sprache\\|([^}]*)}}\\s*\\)\\s*={2}";

  // === {{Wortart|Substantiv|Deutsch}}, {{n}} ===
  protected final static String partOfSpeechPatternString =
      "={3}[^\\{]*\\{\\{Wortart\\|([^\\}\\|]*)(?:\\|([^\\}]*))?\\}\\}.*={3}";
  protected final static String subSection4PatternString = "={4}\\s*(.*)\\s*={4}";
  protected final static String germanDefinitionPatternString =
      "^:{1,3}\\s*(?:\\[(" + senseNumberRegExp + "*)\\])?([^\n\r]*)$";
  protected final static String germanNymLinePatternString =
      "^:{1,3}\\s*(?:\\[(" + senseNumberOrRangeRegExp + "*)\\])?([^\n\r]*)$";

  // :{{IPA}} {{Lautschrift|ˈɛçoˌloːt}}
  // or multiple variants!
  // :{{IPA}} bundesdeutsches Hochdeutsch: {{Lautschrift|çeˈmiː}} (im norddeutschen
  // Gebrauchsstandard auch {{Lautschrift|ʃeˈmiː}}), Schweizer und österreichisches Hochdeutsch:
  // {{Lautschrift|keˈmiː}} (im Schweizer Gebrauchsstandard auch {{Lautschrift|xeˈmiː}})

  protected final static String pronPatternString = "\\{\\{Lautschrift\\|([^\\}\\|]*)\\}\\}";

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected GermanMorphologyExtractor morphologyExtractor;
  private GermanDefinitionExpander definitionExpander;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    morphologyExtractor = new GermanMorphologyExtractor(wdh, wi);
    definitionExpander = new GermanDefinitionExpander(wi);
  }

  protected final static String macroOrPOSPatternString;
  protected final static String posHeaderElementsPatternString;

  protected final static Pattern languageSectionPattern;
  protected final static Pattern germanDefinitionPattern;
  protected final static Pattern germanNymLinePattern;
  protected final static String multilineMacroPatternString;
  protected final static Pattern macroOrPOSPattern; // Combine macro pattern and pos pattern.
  protected final static Pattern posHeaderElementsPattern;
  // protected final static HashSet<String> posMarkers;
  protected final static HashSet<String> ignorableSectionMarkers;
  protected final static HashSet<String> nymMarkers;
  protected final static HashMap<String, String> nymMarkerToNymName;
  // protected final static HashSet<String> inflectionMarkers;

  protected final static Pattern pronPattern;

  static {

    // languageSectionPattern =
    // Pattern.compile(languageSectionPatternString);

    languageSectionPattern = Pattern.compile(languageSectionPatternString);
    multilineMacroPatternString = "\\{\\{" //
        + "([^\\}\\|]*)(?:\\|([^\\}]*))?" //
        + "\\}\\}";

    macroOrPOSPatternString = "(?:" //
        + WikiPatterns.macroPatternString //
        + ")|(?:" //
        + partOfSpeechPatternString //
        + ")|(?:" //
        + subSection4PatternString //
        + ")|(?:" //
        + multilineMacroPatternString //
        + ")";

    macroOrPOSPattern = Pattern.compile(macroOrPOSPatternString);

    posHeaderElementsPatternString =
        "(?:" + WikiPatterns.macroPatternString + ")|(?:" //
            + "''(.*)''" //
            + ")|(?:" //
            + "[,/\\(\\)]\\s*" //
            + ")|(?:" //
            + "((?:ohne\\s+)?[^\\s,/\\(\\)]+)" //
            + ")";
    posHeaderElementsPattern = Pattern.compile(posHeaderElementsPatternString);

    germanDefinitionPattern = Pattern.compile(germanDefinitionPatternString, Pattern.MULTILINE);
    germanNymLinePattern = Pattern.compile(germanNymLinePatternString, Pattern.MULTILINE);

    pronPattern = Pattern.compile(pronPatternString);

    // posMarkers = new HashSet<String>(20);
    // posMarkers.add("Substantiv"); // Should I get the
    // // Toponym/Vorname/Nachname additional
    // // info ?
    // posMarkers.add("Adjektiv");
    // posMarkers.add("Absolutadjektiv");
    // posMarkers.add("Partizip");
    // posMarkers.add("Adverb");
    // posMarkers.add("Wortverbindung");
    // posMarkers.add("Verb");

    ignorableSectionMarkers = new HashSet<>(20);
    ignorableSectionMarkers.add("Silbentrennung");
    ignorableSectionMarkers.add("Aussprache");
    ignorableSectionMarkers.add("Worttrennung");
    ignorableSectionMarkers.add("Herkunft");
    ignorableSectionMarkers.add("Gegenworte");
    ignorableSectionMarkers.add("Beispiele");
    ignorableSectionMarkers.add("Redewendungen");
    ignorableSectionMarkers.add("Abgeleitete Begriffe");
    ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
    ignorableSectionMarkers.add("Dialektausdrücke (Deutsch)");
    ignorableSectionMarkers.add("Referenzen");
    ignorableSectionMarkers.add("Ähnlichkeiten");
    ignorableSectionMarkers.add("Anmerkungen");
    // TODO: Integrate these in alternative spelling ?
    ignorableSectionMarkers.add("Alte Rechtschreibung");
    ignorableSectionMarkers.add("Nebenformen");
    ignorableSectionMarkers.add("Vokalisierung");
    ignorableSectionMarkers.add("Grammatische Merkmale");
    // TODO: Integrate these in alternative spelling ?
    ignorableSectionMarkers.add("Abkürzungen");
    // TODO: related words (should I keep it ?)
    ignorableSectionMarkers.add("Sinnverwandte Wörter");
    ignorableSectionMarkers.add("Weibliche Wortformen");
    ignorableSectionMarkers.add("Männliche Wortformen");
    // TODO: Diminutif... qu'en faire ?
    ignorableSectionMarkers.add("Verkleinerungsformen");
    ignorableSectionMarkers.add("Vergrößerungsformen");
    ignorableSectionMarkers.add("Kurzformen");
    ignorableSectionMarkers.add("Koseformen");
    ignorableSectionMarkers.add("Kurz- und Koseformen");
    ignorableSectionMarkers.add("Namensvarianten");
    ignorableSectionMarkers.add("Weibliche Namensvarianten");
    ignorableSectionMarkers.add("Männliche Namensvarianten");
    ignorableSectionMarkers.add("Bekannte Namensträger");
    ignorableSectionMarkers.add("Sprichwörter");

    // Others
    ignorableSectionMarkers.add("Anmerkung");
    ignorableSectionMarkers.add("Anmerkung Küpper");
    ignorableSectionMarkers.add("Anmerkung Steigerung");
    // TODO: Refers to another entry... Should keep the info.
    ignorableSectionMarkers.add("Lemmaverweis");
    ignorableSectionMarkers.add("Veraltete Schreibweisen");
    ignorableSectionMarkers.add("Steigerbarkeit Adjektiv");
    ignorableSectionMarkers.add("Wortbildungen");
    ignorableSectionMarkers.add("Symbole");

    nymMarkers = new HashSet<>(20);
    nymMarkers.add("Synonyme");
    nymMarkers.add("Gegenwörter");
    nymMarkers.add("Gegenworte");
    nymMarkers.add("Oberbegriffe");
    nymMarkers.add("Unterbegriffe");
    nymMarkers.add("Meronyms"); // TODO: Any meronym/metonym info in German
    // ?

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("Synonyme", "syn");
    nymMarkerToNymName.put("Gegenwörter", "ant");
    nymMarkerToNymName.put("Gegenworte", "ant");
    nymMarkerToNymName.put("Unterbegriffe", "hypo");
    nymMarkerToNymName.put("Oberbegriffe", "hyper");
    nymMarkerToNymName.put("Meronyms", "mero");

  }

  /*
   * (non-Javadoc)
   *
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang .String,
   * org.getalp.blexisma.semnet.SemanticNetwork)
   */
  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    Matcher languageFilter = languageSectionPattern.matcher(pageContent);
    while (languageFilter.find() && !isGermanLanguageHeader(languageFilter)) {
      // nop
    }
    // Either the filter is at end of sequence or on German language header.
    if (languageFilter.hitEnd()) {
      // There is no German data in this page.
      return;
    }
    int germanSectionStartOffset = languageFilter.end();
    // Advance till end of sequence or new language section
    // WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) -
    // languageFilter.start()) != 2) {
    languageFilter.find();
    // languageFilter.find();
    int germanSectionEndOffset =
        languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractGermanData(germanSectionStartOffset, germanSectionEndOffset);
    wdh.finalizePageExtraction();
  }

  private boolean isGermanLanguageHeader(Matcher m) {
    return m.group(2).equals("Deutsch");
  }

  // TODO: Prise en compte des diminutifs (Verkleinerungsformen)
  // TODO: Prise en compte des "concepts dérivés" ? (Abgeleitete Begriffe)
  // TODO: supprimer les "Deklinierte Form" des catégories extraites.

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, POSBLOCK, NYMBLOCK, PRONBLOCK
  }

  private Block currentBlock = Block.NOBLOCK;

  int blockStart = -1;

  private String currentNym = null;

  private void extractGermanData(int startOffset, int endOffset) {
    Matcher m = macroOrPOSPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    wdh.initializeLanguageSection("de");
    currentBlock = Block.IGNOREPOS;

    while (m.find()) {
      HashMap<String, Object> context = new HashMap<>();
      Block nextBlock = computeNextBlock(m, context);

      if (nextBlock == null) {
        continue;
      }
      // If current block is IGNOREPOS, we should ignore everything but a new
      // DEFBLOCK/INFLECTIONBLOCK
      if (Block.IGNOREPOS != currentBlock || (Block.POSBLOCK == nextBlock)) {
        leaveCurrentBlock(m);
        gotoNextBlock(nextBlock, context);
      }
    }
    // Finalize the entry parsing
    leaveCurrentBlock(m);
    wdh.finalizeLanguageSection();

  }

  private Block computeNextBlock(Matcher m, Map<String, Object> context) {
    String pos, nym;
    context.put("start", m.end());

    if (null != m.group(1)) {
      // go to the good block
      String template = m.group(1).trim();
      if (template.equals("Bedeutungen")) {
        return Block.DEFBLOCK;
      }
      if (template.equals("Aussprache")) {
        return Block.PRONBLOCK;
      } else if (template.equals("Alternative Schreibweisen")) {
        return Block.ORTHOALTBLOCK;
      } else if (nymMarkers.contains(template)) {
        context.put("nym", nymMarkerToNymName.get(template));
        return Block.NYMBLOCK;
      } else if (ignorableSectionMarkers.contains(template)) {
        return Block.NOBLOCK;
      } else if (isInflexionMacro(template)) {
        context.put("start", m.start());
        return Block.INFLECTIONBLOCK;
        // the followed comentary permit the recognition of page which are containing inflected form
        // }else if(m.group(1).equals("Lemmaverweis") || m.group(1).equals("Grundformverweis")){
        // if(inflectedform && null!=m.group(2)){
        // TODO : adding a parser for this kind of page
        // }
      } else {
        return null;
      }
    } else if (null != m.group(3)) {
      String fullHeader = m.group().trim();
      if (fullHeader.contains("{{Wortart|Deklinierte Form")
          || fullHeader.contains("{{Wortart|Konjugierte Form")) {
        // TODO: Should I extract morphological data from deklinierte formen ?
        return Block.IGNOREPOS;
      }
      context.put("posHeader", m.group());
      // log.debug("Entry Section: {} --in-- {}", m.group(), this.wiktionaryPageName);
      // TODO: language in group 4 is the language of origin of the entry. Maybe we should keep it.
      // TODO: filter out ignorable part of speech;
      return Block.POSBLOCK;
    } else if (null != m.group(5)) {
      if (m.group(5).trim().equals("Übersetzungen")
          || m.group(5).trim().equals("{{Übersetzungen}}")) {
        return Block.TRADBLOCK;
      } else {
        return null;
      }
      // inflection block
    } else if (null != m.group(6)) {
      // TODO: this condition captures more than the expected macros.
      if (isInflexionMacro(m.group(6))) {
        context.put("start", m.start());
        return Block.INFLECTIONBLOCK;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private boolean isInflexionMacro(String macro) {
    macro = macro.trim();
    if (macro.startsWith("Lit-")) {
      return false;
    } else if (macro.contains("(Deutsch)") || macro.contains("Deutschland")
        || macro.contains("Deutsche")) {
      return false;
    } else if (macro.startsWith("Ü-")) {
      return false;
    } else if ((macro.contains("Deutsch")) || (macro.contains("Tabelle"))) {
      // log.debug("Inflexion Macro: {} in {}", macro, this.wiktionaryPageName);
      return true;
    } else {
      return false;
    }
  }


  private void gotoNextBlock(Block nextBlock, HashMap<String, Object> context) {
    currentBlock = nextBlock;
    Object start = context.get("start");
    blockStart = (null == start) ? -1 : (int) start;
    switch (nextBlock) {
      case NOBLOCK:
      case IGNOREPOS:
        break;
      case POSBLOCK:
        String header = (String) context.get("posHeader");
        registerAllPosInformation(header);
        // wdh.addPartOfSpeech(pos);
        break;
      case INFLECTIONBLOCK:
        break;
      case DEFBLOCK:
        break;
      case TRADBLOCK:
        break;
      case ORTHOALTBLOCK:
        break;
      case NYMBLOCK:
        currentNym = (String) context.get("nym");
        break;
      case PRONBLOCK:
        break;
      default:
        assert false
            : "Unexpected block while ending extraction of entry: " + getWiktionaryPageName();
    }

  }

  private void leaveCurrentBlock(Matcher m) {
    if (blockStart == -1) {
      return;
    }
    // log.trace("Leaving block {} while parsing entry {}", currentBlock.name(),
    // this.wiktionaryPageName);

    int end = computeRegionEnd(blockStart, m);
    switch (currentBlock) {
      case NOBLOCK:
      case IGNOREPOS:
      case POSBLOCK:
        break;
      case DEFBLOCK:
        extractDefinitions(blockStart, end);
        break;
      case TRADBLOCK:
        extractTranslations(blockStart, end);
        break;
      case ORTHOALTBLOCK:
        extractOrthoAlt(blockStart, end);
        break;
      case NYMBLOCK:
        extractNyms(currentNym, blockStart, end);
        currentNym = null;
        break;
      case PRONBLOCK:
        extractPron(blockStart, end);
        break;
      case INFLECTIONBLOCK:
        extractInflections(blockStart, end);
        blockStart = end;
        break;
      default:
        assert false
            : "Unexpected block while ending extraction of entry: " + getWiktionaryPageName();
    }

    blockStart = -1;

  }

  private void registerAllPosInformation(String header) {
    header = header.replaceAll("===", "");
    Matcher m = posHeaderElementsPattern.matcher(header);
    StringBuffer unhandledData = new StringBuffer();
    ArrayList<String> partOfSpeeches = new ArrayList<>();
    HashSet<String> additionalInfo = new HashSet<>();

    while (m.find()) {
      m.appendReplacement(unhandledData, "");
      if (null != m.group(1)) {
        Map<String, String> args = WikiTool.parseArgs(m.group(2));
        String template = m.group(1);
        // Template
        switch (template) {
          case "Wortart":
            if (null == args.get("1")) {
              log.error("No part of speech in Wortart macro in {}", this.getWiktionaryPageName());
            }
            partOfSpeeches.add(args.get("1"));
            break;
          default:
            additionalInfo.add(template);
        }
      } else if (null != m.group(3)) {
        // Italics
        additionalInfo.addAll(Arrays.asList(m.group(3).split(",")));
      } else if (m.group(4) != null) {
        additionalInfo.add(m.group(4));
      }
    }
    m.appendTail(unhandledData);
    String unhandledDataString = unhandledData.toString().trim();
    if (unhandledDataString.length() != 0) {
      log.debug("Unhandled POS data: ", m.toString());
    }
    // TODO: maybe use all POS instead of the first one to create the entry label...
    wdh.initializeLexicalEntry(partOfSpeeches.remove(0));
    // TODO register other POS links + additional information
    WiktionaryDataHandler dwdh = (WiktionaryDataHandler) wdh;
    for (String pos : partOfSpeeches) {
      dwdh.addExtraPartOfSpeech(pos);
    }
    for (String info : additionalInfo) {
      dwdh.addExtraInformation(info);
    }

  }

  private static HashSet<String> verbMarker;

  static {
    verbMarker = new HashSet<>();
    verbMarker.add("Verb");
    verbMarker.add("Hilfsverb");
  }

  private void extractInflections(int startOffset, int endOffset) {
    if (wdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
      return;
    }
    parseInflectionTables(startOffset, endOffset);
  }

  private void parseInflectionTables(int startOffset, int endOffset) {
    String region = pageContent.substring(startOffset, endOffset);
    morphologyExtractor.extractMorphologicalData(region, getWiktionaryPageName());
  }

  static final String glossOrMacroPatternString;
  static final Pattern glossOrMacroPattern;

  static {

    // glossOrMacroPatternString =
    // "(?:\\[([^\\]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|?([^\\}]*)\\}\\})";
    glossOrMacroPatternString =
        "(?:\\[([^\\]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|?([^\\}]*)\\}\\})";

    glossOrMacroPattern = Pattern.compile(glossOrMacroPatternString);
  }

  protected Resource globalGloss = null;

  private void extractTranslations(int startOffset, int endOffset) {
    WikiText wt = new WikiText(getWiktionaryPageName(), pageContent, startOffset, endOffset);
    List<? extends WikiText.Token> toks = wt.wikiTokens();

    for (WikiText.Token t : toks) {
      if (t instanceof WikiText.Template
          && ((WikiText.Template) t).getName().trim().equals("Ü-Tabelle")) {
        WikiText.Template tmpl = t.asTemplate();
        Map<String, WikiText.WikiContent> args = tmpl.getArgs();
        // DONE: General gloss is in arg G // Meaning number in arg 1
        globalGloss = wdh.createGlossResource(
            new StructuredGloss(tmpl.getParsedArg("1"), tmpl.getParsedArg("G")));
        extractTranslationsFromItems(args.get("Ü-links"));
        extractTranslationsFromItems(args.get("Ü-rechts"));
        extractTranslationsFromItems(args.get("Ü-Liste"));
        // extractTranslationsFromItems(args.get("Dialekttabelle"));
        // extractTranslationsFromItems(args.get("D-rechts"));
      }
      // TODO : check if entries are using other arguments
    }
  }

  private void extractTranslationsFromItems(WikiText.WikiContent wc) {
    if (null == wc) {
      return;
    }
    List<? extends Token> toks = wc.wikiTokens();

    for (WikiText.Token li : toks) {
      if (li instanceof WikiText.IndentedItem) {
        extractTranslationFromItem(li.asIndentedItem());
      }
    }
  }

  private void extractTranslationFromItem(WikiText.IndentedItem li) {
    extractTranslationsFromListContent(li.getContent());
  }

  // TODO: handle dialekts and translations as links
  private static String translationTokenizer = "(?<TMPLLANG>^\\p{Template}\\s*:\\s*)|" //
      + "(?<CTLANG>^(?<LANG>.*)\\s*:\\s*)|" //
      + "(?<ITALICS>'{2,3}.*?'{2,3})|" //
      + "(?<PARENS>\\(\\P{Reserved}*?\\))|" //
      + "(?<SPECIALPARENS>\\(.*?\\))|" //
      + "\\[(?<GLOSS>\\P{Reserved}*?)\\]|" //
      + "(?<TMPL>\\p{Template})|" //
      + "(?<LINK>\\p{InternalLink})";

  private static final Pattern tokenizer = WikiPattern.compile(translationTokenizer);

  private static <T> T or(T _1, T _2) {
    return null == _1 ? _2 : _1;
  }

  // TODO: faire une analyse plus poussée des traduction, car il y a des entrées comme cela :
  // se {{Ü|fr|mettre}} {{Ü|fr|à}} {{Ü|fr|couler}} qui est extrait en 3 traductions différentes
  // TODO: Certains liens sont entre crochet pour annoter la traduction suivante ou précédente
  // comme dans la traduction anglais de ‘Präsidentin’ ([female] chairperson),
  // où female ne doit pas être extrait.
  private void extractTranslationsFromListContent(WikiText.WikiContent content) {

    // log.trace("Translation line = {}", content);

    WikiCharSequence line = new WikiCharSequence(content);

    Matcher lexer = tokenizer.matcher(line);
    Resource currentGloss = null;

    int rank = 1;

    // TODO: Support usage notes as : {{Ü...}} {{m}}
    while (lexer.find()) {
      String g;
      String lang;
      if (null != (g = lexer.group("TMPLLANG"))) {
        // Ignoring languages as a template as translations will be given
      } else if (null != (g = lexer.group("CTLANG"))) {
        // Clear Text language -- ignore it for the moment. To be processed when dialekts will be
        // added.
        lang = lexer.group("LANG");
      } else if (null != (g = lexer.group("ITALICS"))) {
        // ignore ?
      } else if (null != (g = lexer.group("PARENS"))) {
        // ignore ?
      } else if (null != (g = lexer.group("SPECIALPARENS"))) {
        log.debug("Template or link inside parens: {}",
            line.getSourceContent(lexer.group("SPECIALPARENS")));
        // TODO: some are only additional usage notes, other are alternate translation, decide
        // between them and handle the translation cases.
      } else if (null != (g = lexer.group("GLOSS"))) {
        currentGloss = wdh.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
      } else if (null != (g = lexer.group("LINK"))) {
        log.debug("Translation as link : {}", line.getToken(lexer.group("LINK")));
      } else if (null != (g = lexer.group("TMPL"))) {
        WikiText.Template t = (WikiText.Template) line.getToken(g);
        String lg;
        String lemma;
        String linkText;
        String usage;
        String vocalization;
        switch (t.getName()) {
          case "Ü":
          case "Ü?":
            lg = GermanLangToCode.threeLettersCode(LangTools.normalize(t.getParsedArgs().get("1")));
            lemma = t.getParsedArgs().get("2");
            linkText = t.getParsedArgs().get("3");
            usage = null;
            if (null != linkText) {
              usage = "alt=" + linkText + "|";
            }
            if (null != lg && null != lemma && !"".equals(lemma)) {
              wdh.registerTranslation(lg, or(currentGloss, globalGloss), usage, lemma);
            }
            break;
          case "Üt":
          case "Üt?":
            lg = GermanLangToCode.threeLettersCode(LangTools.normalize(t.getParsedArgs().get("1")));
            lemma = t.getParsedArgs().get("2");
            String transcript = t.getParsedArgs().get("3");
            linkText = t.getParsedArgs().get("4");
            usage = null;
            if (null != transcript && transcript.trim().length() > 0) {
              usage = "t=" + transcript + "|";
            }
            if (null != linkText) {
              usage = "alt=" + linkText + "|";
            }
            if (null != lg && null != lemma && !"".equals(lemma)) {
              wdh.registerTranslation(lg, or(currentGloss, globalGloss), usage, lemma);
            }
            break;
          case "Üxx4":
          case "Üxx4?":
            lg = GermanLangToCode.threeLettersCode(LangTools.normalize(t.getParsedArgs().get("1")));
            lemma = t.getParsedArgs().get("2");
            linkText = t.getParsedArgs().get("3");
            vocalization = t.getParsedArgs().get("v");
            transcript = t.getParsedArgs().get("d");
            String bedeutung = t.getParsedArgs().get("b");
            usage = null;
            if (null != transcript) {
              usage = "t=" + transcript + "|";
            }
            if (null != linkText) {
              usage = "alt=" + linkText + "|";
            }
            if (null != vocalization) {
              usage = "v=" + vocalization + "|";
            }
            if (null != bedeutung) {
              usage = "b=" + bedeutung + "|";
            }
            if (null != lg && null != lemma && !"".equals(lemma)) {
              wdh.registerTranslation(lg, or(currentGloss, globalGloss), usage, lemma);
            }
            break;
          case "Üxx5":
          case "Üxx5?":
            lg = GermanLangToCode.threeLettersCode(LangTools.normalize(t.getParsedArgs().get("1")));
            lemma = t.getParsedArgs().get("3");
            vocalization = t.getParsedArgs().get("4");
            transcript = t.getParsedArgs().get("2");
            usage = null;
            if (null != transcript) {
              usage = "t=" + transcript + "|";
            }
            if (null != vocalization) {
              usage = "v=" + vocalization + "|";
            }
            if (null != lg && null != lemma && !"".equals(lemma)) {
              wdh.registerTranslation(lg, or(currentGloss, globalGloss), usage, lemma);
            }
          default:
            log.trace("Translation: Ignoring template {} || {}", t, getWiktionaryPageName());
        }
      }
    }
  }

  @Override
  protected void extractNyms(String synRelation, int startOffset, int endOffset) {

    Matcher nymLineMatcher = germanNymLinePattern.matcher(this.pageContent);
    nymLineMatcher.region(startOffset, endOffset);

    String currentLevel1SenseNumber = "";
    String currentLevel2SenseNumber = "";
    while (nymLineMatcher.find()) {
      String nymLine = nymLineMatcher.group(2);
      String senseNum = nymLineMatcher.group(1);
      if (null != senseNum) {
        senseNum = senseNum.trim();
        senseNum = senseNum.replaceAll("<[^>]*>", "");
        if (nymLineMatcher.group().length() >= 2 && nymLineMatcher.group().charAt(1) == ':') {
          if (nymLineMatcher.group().length() >= 3 && nymLineMatcher.group().charAt(2) == ':') {
            // Level 3
            log.debug("Level 3 definition: \"{}\" in entry {}", nymLineMatcher.group(),
                this.getWiktionaryPageName());
            if (!senseNum.startsWith(currentLevel2SenseNumber)) {
              senseNum = currentLevel2SenseNumber + senseNum;
            }
            log.debug("Sense number is: {}", senseNum);
          } else {
            // Level 2
            // log.debug("Level 2 definition: \"{}\" in entry {}", definitionMatcher.group(),
            // this.wiktionaryPageName);
            if (!senseNum.startsWith(currentLevel1SenseNumber)) {
              senseNum = currentLevel1SenseNumber + senseNum;
            }
            currentLevel2SenseNumber = senseNum;
          }
        } else {
          // Level 1 definition
          currentLevel1SenseNumber = senseNum;
          currentLevel2SenseNumber = senseNum;
        }
      }

      if (nymLine != null && !nymLine.equals("")) {

        Matcher linkMatcher = WikiPatterns.linkPattern.matcher(nymLine);
        while (linkMatcher.find()) {
          // It's a link, only keep the alternate string if present.
          String leftGroup = linkMatcher.group(1);
          if (leftGroup != null && !leftGroup.equals("") && !leftGroup.startsWith("Wikisaurus:")
              && !leftGroup.startsWith("Catégorie:") && !leftGroup.startsWith("#")) {
            if (null == senseNum) {
              wdh.registerNymRelation(leftGroup, synRelation);
            } else {
              wdh.registerNymRelation(leftGroup, synRelation,
                  wdh.createGlossResource(glossFilter.extractGlossStructure(senseNum)));
            }
          }
        }

      }
    }


  }

  protected void extractPron(int startOffset, int endOffset) {
    Matcher pronMatcher = pronPattern.matcher(this.pageContent);
    pronMatcher.region(startOffset, endOffset);
    // this might be an overkill: but we would like to
    // add multiple pronunciations in the reverse order
    // so in the result show up in the original order again
    ArrayList<String> matches = new ArrayList<>();
    while (pronMatcher.find()) {
      String pron = pronMatcher.group(1);
      if (null == pron || pron.equals("")) {
        return;
      }
      matches.add(pron);
    }
    ListIterator<String> li = matches.listIterator(matches.size());
    while (li.hasPrevious()) {
      String pron = li.previous();
      wdh.registerPronunciation(pron, "de-fonipa");
    }
  }

  @Override
  protected void extractDefinitions(int startOffset, int endOffset) {
    // TODO: The definition pattern is the only one that changes. Hence, we should normalize this
    // processing and put the macro in language specific parameters.
    Matcher definitionMatcher = germanDefinitionPattern.matcher(this.pageContent);
    definitionMatcher.region(startOffset, endOffset);

    String currentLevel1SenseNumber = "";
    String currentLevel2SenseNumber = "";
    while (definitionMatcher.find()) {
      String def = expandDefinition(definitionMatcher.group(2));
      String senseNum = definitionMatcher.group(1);
      if (null == senseNum) {
        log.debug("Null sense number in definition\"{}\" for entry {}", def,
            this.getWiktionaryPageName());
      } else {
        senseNum = senseNum.trim();
        senseNum = senseNum.replaceAll("<[^>]*>", "");
        if (definitionMatcher.group().length() >= 2 && definitionMatcher.group().charAt(1) == ':') {
          if (definitionMatcher.group().length() >= 3
              && definitionMatcher.group().charAt(2) == ':') {
            // Level 3
            log.debug("Level 3 definition: \"{}\" in entry {}", definitionMatcher.group(),
                this.getWiktionaryPageName());
            if (!senseNum.startsWith(currentLevel2SenseNumber)) {
              senseNum = currentLevel2SenseNumber + senseNum;
            }
            log.debug("Sense number is: {}", senseNum);
          } else {
            // Level 2
            // log.debug("Level 2 definition: \"{}\" in entry {}", definitionMatcher.group(),
            // this.wiktionaryPageName);
            if (!senseNum.startsWith(currentLevel1SenseNumber)) {
              senseNum = currentLevel1SenseNumber + senseNum;
            }
            currentLevel2SenseNumber = senseNum;
          }
        } else {
          // Level 1 definition
          currentLevel1SenseNumber = senseNum;
          currentLevel2SenseNumber = senseNum;
        }

        if (def != null && !def.equals("")) {
          wdh.registerNewDefinition(def, senseNum);
        }
      }
    }
  }


  private String expandDefinition(String group) {
    if (group == null)
      return "";
    definitionExpander.setPageName(getWiktionaryPageName());
    return definitionExpander.expandAll(group.trim(), null);
  }

}
