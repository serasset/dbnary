package org.getalp.dbnary.languages.pol;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String senseNumberRegExp = "[^\\)]*";
  // == cebula ({{język polski}}) ==
  protected final static String languageSectionPatternString =
      "={2}\\s*([^\\(]*)\\((.*)\\)\\s*={2}";

  protected final static String partOfSpeechPatternString = "''(.*)''";
  protected final static String subSection4PatternString = "={4}\\s*(.*)\\s*={4}";
  protected final static String polishCitationPatternString = "<ref>(.*)</ref>";
  protected final static String polishDefinitionPatternString =
      "^:{1,3}\\s*(?:\\((" + senseNumberRegExp + ")\\))?\\s*([^\n\r]*)$";

  protected WiktionaryDataHandler polwdh;

  private final int NODATA = 0;
  private final int TRADBLOCK = 1;
  private final int DEFBLOCK = 2;
  private final int ORTHOALTBLOCK = 3;
  private final int NYMBLOCK = 4;
  private final int PRONBLOCK = 5;
  private final int EXAMPLEBLOCK = 6;
  private final int IGNOREPOS = 7;

  protected enum SectionType {
    DEFS, NYMS, TRANS, PRON, MORPH, EXAMPLES, IGNORE, NOTASECTION
  }

  protected ExpandAllWikiModel definitionExpander;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    this.polwdh = (WiktionaryDataHandler) wdh;

  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExpander = new DefinitionExpanderWikiModel(wi, new Locale("pl"), "", "");
  }

  protected final static Pattern languageSectionPattern;
  protected final static Pattern polishDefinitionPattern;
  protected final static Pattern polishNymLinePattern;
  protected final static Pattern polishExampleLinePattern;
  protected final static Pattern polishCitationPattern;

  protected final static Pattern sectionPattern; // Combine macro pattern
  // and pos pattern.
  protected final static HashMap<String, SectionType> validSectionTemplates;

  protected final static HashMap<String, String> nymMarkerToNymName;
  protected final static HashMap<String, Resource> definitionSenseLink;

  static {
    // languageSectionPattern =
    // Pattern.compile(languageSectionPatternString);

    languageSectionPattern = Pattern.compile(languageSectionPatternString);

    sectionPattern = WikiPatterns.macroPattern;

    String nymLinePattern = new StringBuilder().append("(?:").append(polishDefinitionPatternString)
        .append(")|(?:").append("^(.*)$").append(")").toString();

    polishNymLinePattern = Pattern.compile(nymLinePattern, Pattern.MULTILINE);

    String defPattern = new StringBuilder().append("(?:").append(polishDefinitionPatternString)
        .append(")|(?:").append(partOfSpeechPatternString).append(")|(?:").append("^(.*)$")
        .append(")").toString();

    polishDefinitionPattern = Pattern.compile(defPattern, Pattern.MULTILINE);

    String ExamplePattern = new StringBuilder().append("(?:").append(polishDefinitionPatternString)
        .append(")|(?:").append(partOfSpeechPatternString).append(")|(?:").append("^(.*)$")
        .append(")").toString();
    polishExampleLinePattern = Pattern.compile(ExamplePattern, Pattern.MULTILINE);

    polishCitationPattern = Pattern.compile(polishCitationPatternString);

    validSectionTemplates = new HashMap<>(20);
    validSectionTemplates.put("wymowa", SectionType.PRON);
    validSectionTemplates.put("znaczenia", SectionType.DEFS);
    validSectionTemplates.put("odmiana", SectionType.MORPH);
    validSectionTemplates.put("przykłady", SectionType.EXAMPLES);
    validSectionTemplates.put("składnia", SectionType.IGNORE); // SYNTAX
    validSectionTemplates.put("kolokacje", SectionType.IGNORE); // COLLOCATIONS
    validSectionTemplates.put("synonimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("antonimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("hiperonimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("hiponimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("holonimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("meronimy", SectionType.NYMS); // SYNTAX
    validSectionTemplates.put("pokrewne", SectionType.IGNORE); // RELATED TERMS
    validSectionTemplates.put("frazeologia", SectionType.IGNORE); // COLOCATION/PHRASES
    validSectionTemplates.put("etymologia", SectionType.IGNORE); // ETYMOLOGY
    validSectionTemplates.put("uwagi", SectionType.IGNORE); // COMMENTS
    validSectionTemplates.put("tłumaczenia", SectionType.TRANS);
    validSectionTemplates.put("źródła", SectionType.IGNORE); // SOURCES

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("synonimy", "syn");
    nymMarkerToNymName.put("antonimy", "ant");
    nymMarkerToNymName.put("hiponimy", "hypo");
    nymMarkerToNymName.put("hiperonimy", "hyper");
    nymMarkerToNymName.put("holonimy", "holo");
    nymMarkerToNymName.put("meronimy", "mero");

    definitionSenseLink = new HashMap<>(40);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang .String,
   * org.getalp.blexisma.semnet.SemanticNetwork)
   */
  @Override
  public void extractData() {
    polwdh.initializePageExtraction(getWiktionaryPageName());
    definitionExpander.setPageName(getWiktionaryPageName());
    // System.out.println(pageContent);
    Matcher languageFilter = languageSectionPattern.matcher(pageContent);

    // Either the filter is at end of sequence or on Polish language header.
    while (languageFilter.find() && !languageFilter.group(2).contains("polski")) {
      // nop
    }

    if (languageFilter.hitEnd()) {
      return;
    }

    int polishSectionStartOffset = languageFilter.end();

    // Advance till end of sequence or new language section
    languageFilter.find();
    int polishSectionEndOffset =
        languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractPolishData(polishSectionStartOffset, polishSectionEndOffset);
    polwdh.finalizePageExtraction();
  }

  int state = NODATA;
  int definitionBlockStart = -1;
  int orthBlockStart = -1;
  int translationBlockStart = -1;
  int exampleBlockStart = -1;
  private int nymBlockStart = -1;
  private String currentNym = null;

  private int pronBlockStart = -1;

  void registerNewPartOfSpeech(Matcher m) {
    polwdh.initializeLexicalEntry(m.group(3));
  }

  void gotoNoData(Matcher m) {
    state = NODATA;
  }

  void gotoDefBlock(Matcher m) {
    state = DEFBLOCK;
    definitionBlockStart = m.end();
  }

  void leaveDefBlock(Matcher m) {
    extractDefinitions(definitionBlockStart, computeRegionEnd(definitionBlockStart, m));
    definitionBlockStart = -1;
  }

  void gotoTradBlock(Matcher m) {
    translationBlockStart = m.end();
    state = TRADBLOCK;
  }

  void leaveTradBlock(Matcher m) {
    extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
    translationBlockStart = -1;
  }

  void gotoOrthoAltBlock(Matcher m) {
    state = ORTHOALTBLOCK;
    orthBlockStart = m.end();
  }

  void leaveOrthoAltBlock(Matcher m) {
    extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
    orthBlockStart = -1;
  }

  private void gotoNymBlock(Matcher m) {
    state = NYMBLOCK;
    currentNym = nymMarkerToNymName.get(m.group(1));
    nymBlockStart = m.end();
  }

  private void leaveNymBlock(Matcher m) {
    extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
    currentNym = null;
    nymBlockStart = -1;
  }

  private void gotoPronBlock(Matcher m) {
    state = PRONBLOCK;
    pronBlockStart = m.end();
  }

  private void leavePronBlock(Matcher m) {
    extractPron(pronBlockStart, computeRegionEnd(pronBlockStart, m));

  }

  void gotoExampleBlock(Matcher m) {
    exampleBlockStart = m.end();
    state = EXAMPLEBLOCK;
  }

  void leaveExampleBlock(Matcher m) {
    extractExample(exampleBlockStart, computeRegionEnd(exampleBlockStart, m));
    exampleBlockStart = -1;
  }

  private void extractPolishData(int startOffset, int endOffset) {

    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    polwdh.initializeLanguageSection("pl");
    gotoNoData(m);
    while (m.find()) {
      SectionType t = getSectionType(m.group(1));
      switch (state) {
        case NODATA:
          switch (t) {
            case DEFS:
              gotoDefBlock(m);
              break;
            case NYMS:
              gotoNymBlock(m);
              break;
            case TRANS:
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              gotoExampleBlock(m);
              break;
            case MORPH:
              gotoNoData(m);
              break;
            case PRON:
              gotoPronBlock(m);
              break;
            case IGNORE:
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;
        case DEFBLOCK:
          switch (t) {
            case DEFS:
              leaveDefBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leaveDefBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leaveDefBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leaveDefBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leaveDefBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leaveDefBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leaveDefBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;
        case TRADBLOCK:
          switch (t) {
            case DEFS:
              leaveTradBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leaveTradBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leaveTradBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leaveTradBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leaveTradBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leaveTradBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leaveTradBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;
        case EXAMPLEBLOCK:
          switch (t) {
            case DEFS:
              leaveExampleBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leaveExampleBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leaveExampleBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leaveExampleBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leaveExampleBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leaveExampleBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leaveExampleBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;

        case ORTHOALTBLOCK:
          switch (t) {
            case DEFS:
              leaveOrthoAltBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leaveOrthoAltBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leaveOrthoAltBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leaveOrthoAltBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leaveOrthoAltBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leaveOrthoAltBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leaveOrthoAltBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;

        case NYMBLOCK:
          switch (t) {
            case DEFS:
              leaveNymBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leaveNymBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leaveNymBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leaveNymBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leaveNymBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leaveNymBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leaveNymBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
          }
          break;
        case PRONBLOCK:
          switch (t) {
            case DEFS:
              leavePronBlock(m);
              gotoDefBlock(m);
              break;
            case NYMS:
              leavePronBlock(m);
              gotoNymBlock(m);
              break;
            case TRANS:
              leavePronBlock(m);
              gotoTradBlock(m);
              break;
            case EXAMPLES:
              leavePronBlock(m);
              gotoExampleBlock(m);
              break;
            case MORPH:
              leavePronBlock(m);
              gotoNoData(m);
              break;
            case PRON:
              leavePronBlock(m);
              gotoPronBlock(m);
              break;
            case IGNORE:
              leavePronBlock(m);
              gotoNoData(m);
              break;
            case NOTASECTION:
              break;
            default:
              break;
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
      case EXAMPLEBLOCK:
        leaveTradBlock(m);
        break;
      case ORTHOALTBLOCK:
        leaveOrthoAltBlock(m);
        break;
      case NYMBLOCK:
        leaveNymBlock(m);
        break;
      case PRONBLOCK:

      default:
        assert false : "Unexpected state while extracting translations from dictionary.";
    }
    polwdh.finalizeLanguageSection();
  }


  private SectionType getSectionType(String m) {
    SectionType st = validSectionTemplates.get(m);
    return (null == st) ? SectionType.NOTASECTION : st;
  }

  static final String glossOrMacroPatternString;
  static final Pattern glossOrMacroPattern;
  static final String linePatternString;
  static final Pattern linePattern;
  static final String bulletListPatternString;
  static final Pattern bulletListPattern;

  static {
    glossOrMacroPatternString =
        "(?:\\[([^\\]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|?([^\\}]*)\\}\\})";
    glossOrMacroPattern = Pattern.compile(glossOrMacroPatternString);
    linePatternString = "^[^\n\r]*$";
    linePattern = Pattern.compile(linePatternString, Pattern.MULTILINE);
    bulletListPatternString = "^\\*\\s*([^:]*):([^\n\r]*)$";
    bulletListPattern = Pattern.compile(bulletListPatternString);
  }


  private void extractTranslations(int startOffset, int endOffset) {
    Matcher lineMatcher = linePattern.matcher(pageContent);
    lineMatcher.region(startOffset, endOffset);

    while (lineMatcher.find()) {
      String currentLine = lineMatcher.group();
      if (currentLine.length() == 0) {
        continue;
      }
      Matcher translationMatcher = bulletListPattern.matcher(currentLine);

      if (translationMatcher.matches()) {
        String language = translationMatcher.group(1);
        String lang;
        if ((lang = PolishLangToCode.threeLettersCode(language)) != null) {
          language = lang;
        } else {
          log.debug("Unknown Language : {}", language);
        }
        String translations = translationMatcher.group(2);
        extractTranslationLine(language, translations);
      } else {
        log.debug("INCORRECT Translation Line : {}", currentLine);
      }


    }
  }

  static final String translationLexerString;
  static final Pattern translationLexer;

  static {
    translationLexerString = new StringBuffer().append("(?:").append("\\(([\\d\\.\\-—–\\,\\s]*)\\)")
        .append(")|(?:").append(WikiPatterns.linkPatternString).append(")|(?:")
        .append(WikiPatterns.macroPatternString).append(")|(?:").append("\\(([^\\)]*)\\)")
        .append(")|(?:").append("(.)").append(")").toString();

    translationLexer = Pattern.compile(translationLexerString);

  }

  private void extractTranslationLine(String lang, String translations) {
    Matcher lexer = translationLexer.matcher(translations + ";");
    String currentGloss = null;
    String currentTranslation = "";
    String currentUsage = "";

    while (lexer.find()) {
      if (lexer.group(1) != null) {
        // Sense number
        currentGloss = lexer.group(1);
      } else if (lexer.group(2) != null) {
        // A link (group 2 = target; group 3 = form)
        currentTranslation = currentTranslation + " " + lexer.group(2);
      } else if (lexer.group(4) != null) {
        // A macro (group 4 = macro name, group 5 = parameters)
        // TODO: handle brazport macros.
        if ("furi".equals(lexer.group(4))) {
          Map<String, String> args = WikiTool.parseArgs(lexer.group(5));
          currentTranslation += args.get("1");
        } else {
          currentUsage = currentUsage + "{{" + lexer.group(4) + "}}";
          if (lexer.group(5) != null) {
            log.debug("non empty parameter in {} macro: {}", lexer.group(4), lexer.group(5));
          }
        }
      } else if (lexer.group(6) != null) {
        // A usage note in between parenthesis...
        currentUsage = currentUsage + "(" + lexer.group(6) + ")";
      } else if (lexer.group(7) != null) {
        // A char...
        String character = lexer.group(7);

        if (character.equals(",") || character.equals(";")) {
          polwdh.registerTranslation(lang, currentGloss, currentUsage.trim(),
              currentTranslation.trim());
          currentTranslation = "";
          currentUsage = "";
        } else {
          currentUsage += character;
        }
      }
    }

  }

  @Override
  protected void extractDefinitions(int startOffset, int endOffset) {
    Matcher definitionMatcher = polishDefinitionPattern.matcher(this.pageContent);
    definitionMatcher.region(startOffset, endOffset);
    while (definitionMatcher.find()) {
      if (definitionMatcher.group(2) != null && polwdh.posIsValid()) {
        // It's a definition
        HashSet<String> defTemplates = null;
        if (log.isTraceEnabled()) {
          defTemplates = new HashSet<>();
        }
        String def = definitionExpander.expandAll(definitionMatcher.group(2), defTemplates);
        if (log.isTraceEnabled()) {
          for (String t : defTemplates) {
            log.trace("Encountered template in definition : {}", t);
          }
        }
        // Cleanup remaining html flags from definition expansion...
        def = def.replaceAll("<[^>]*>", "");
        def = def.replaceAll("&nbsp;", " ");
        def = def.replaceAll("&lt;", "<");
        def = def.replaceAll("&gt;", ">");
        String senseNum = definitionMatcher.group(1);
        if (null == senseNum) {
          log.debug("Null sense number in definition\"{}\" for entry {}", def,
              this.getWiktionaryPageName());
          if (def != null && !def.equals("")) {
            Resource res_sense = polwdh.registerNewDefinition(def);
            definitionSenseLink.put(senseNum, res_sense);
          }
        } else {
          senseNum = senseNum.trim();
          senseNum = senseNum.replaceAll("<[^>]*>", "");
          if (def != null && !def.equals("")) {
            Resource res_sense = polwdh.registerNewDefinition(def, senseNum);
            definitionSenseLink.put(senseNum, res_sense);
          }
        }
      } else if (definitionMatcher.group(3) != null) {
        // It's a part of speech
        polwdh.initializeLexicalEntry(definitionMatcher.group(3));
      } else if (definitionMatcher.group(4) != null
          && definitionMatcher.group(4).trim().length() > 0) {
        log.debug("UNKNOWN LINE: \"{}\" in \"{}\"", definitionMatcher.group(4),
            this.getWiktionaryPageName());
      }
    }
  }

  @Override
  protected void extractNyms(String synRelation, int startOffset, int endOffset) {
    Matcher nymLineMatcher = polishNymLinePattern.matcher(this.pageContent);
    nymLineMatcher.region(startOffset, endOffset);
    while (nymLineMatcher.find()) {
      if (nymLineMatcher.group(2) != null) {
        // It's a line with a sense number
        String senseNum = nymLineMatcher.group(1);
        if (null == senseNum) {
          log.debug("Null sense number in nym line\"{}\" for entry {}", nymLineMatcher.group(),
              this.getWiktionaryPageName());
          // TODO: attach the nym to the Vocable
        } else {
          senseNum = senseNum.trim();
          senseNum = senseNum.replaceAll("<[^>]*>", "");
          // Extract all links
          Matcher linkMatcher = WikiPatterns.linkPattern.matcher(nymLineMatcher.group(2));
          while (linkMatcher.find()) {
            // It's a link, only keep the alternate string if present.
            String leftGroup = linkMatcher.group(1);
            if (leftGroup != null && !leftGroup.equals("") && !leftGroup.startsWith("Wikisaurus:")
                && !leftGroup.startsWith("Catégorie:") && !leftGroup.startsWith("#")) {
              polwdh.registerNymRelation(leftGroup, synRelation, senseNum);
            }
          }
        }
      } else if (nymLineMatcher.group(3) != null && nymLineMatcher.group(3).trim().length() > 0) {
        log.debug("UNKNOWN LINE: \"{}\" in \"{}\"", nymLineMatcher.group(3),
            this.getWiktionaryPageName());
      }
    }

  }

  protected void extractExample(int startOffset, int endOffset) {
    Matcher exampleLineMatcher = polishExampleLinePattern.matcher(this.pageContent);
    exampleLineMatcher.region(startOffset, endOffset);
    Set<Pair<Property, RDFNode>> context = new HashSet<>();

    while (exampleLineMatcher.find()) {
      if (exampleLineMatcher.group(2) != null) {
        // It's a line with a sense number

        String example = exampleLineMatcher.group(2);
        String ref = null;

        Matcher exampleCitationMatcher = polishCitationPattern.matcher(example);
        if (exampleCitationMatcher.find()) {
          ref = exampleCitationMatcher.group(1);
          example = example.substring(0, exampleCitationMatcher.start());
          ref = definitionExpander.expandAll(ref, null);
        }

        example = definitionExpander.expandAll(example, null);

        // Cleanup remaining html flags from definition expansion...
        example = example.replaceAll("<[^>]*>", "");
        example = example.replaceAll("&nbsp;", " ");
        example = example.replaceAll("&lt;", "<");
        example = example.replaceAll("&gt;", ">");

        if (ref != null && !ref.isEmpty()) {
          context.add(Pair.of(DCTerms.bibliographicCitation,
              ResourceFactory.createLangLiteral(ref, wdh.getCurrentEntryLanguage())));
        }

        String senseNum = exampleLineMatcher.group(1);
        if (null == senseNum) {
          log.debug("Null sense number in example\"{}\" for entry {}", example,
              this.getWiktionaryPageName());
        } else {
          senseNum = senseNum.trim();
          senseNum = senseNum.replaceAll("<[^>]*>", "");
          if (example != null && !example.equals("")) {
            wdh.registerExampleOnResource(example, context, definitionSenseLink.get(senseNum));
            context.clear();
          }
        }
      } else if (exampleLineMatcher.group(3) != null
          && !exampleLineMatcher.group(3).trim().isEmpty()) {
        log.debug("UNKNOWN LINE: \"{}\" in \"{}\"", exampleLineMatcher.group(3),
            this.getWiktionaryPageName());
      }
    }
  }


  private void extractPron(int startOffset, int endOffset) {
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(this.pageContent);
    macroMatcher.region(startOffset, endOffset);

    while (macroMatcher.find()) {
      if (macroMatcher.group(1).startsWith("IPA")) {
        Map<String, String> args = WikiTool.parseArgs(macroMatcher.group(2));
        for (int i = 1; i <= 5; i++) {
          if (null != args.get(Integer.toString(i))) {
            polwdh.registerPronunciation(args.get(Integer.toString(i)), "pl-ipa");
          }
        }
        if (args.get("6") != null) {
          log.debug("More than 5 pronunciations: {} in {}", args, this.getWiktionaryPageName());
        }
      } else {
        log.debug("UNKNOWN PRONOUNCIATION MACRO: \"{}\" in \"{}\"", macroMatcher.group(1),
            this.getWiktionaryPageName());
      }
    }
  }

}
