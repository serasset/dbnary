/**
 *
 */
package org.getalp.dbnary.languages.ita;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String level2HeaderPatternString = "^==([^=].*[^=])==$";

  protected final static String entrySectionPatternString;

  static {

    entrySectionPatternString = //
        "\\{\\{\\s*-" //
            + "([^\\}\\|\n\r]*)-\\s*(?:\\|([^\\}\n\r]*))?" //
            + "\\}\\}";

  }

  protected final static String wikiSectionPatternString = "={2,4}\\s*([^=]*)\\s*={2,4}";

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK, PRONBLOCK
  }

  // TODO: handle pronounciation
  protected final static String pronounciationPatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  // protected final static Pattern languageSectionPattern;
  // protected final static HashSet<String> nymMarkers;
  protected final static HashMap<String, String> nymMarkerToNymName;

  static {

    // TODO: -acron-, -acronim-, -acronym-, -espr-, -espress- mark locution as phrases

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("syn", "syn");
    nymMarkerToNymName.put("sin", "syn");
    nymMarkerToNymName.put("ant", "ant");
    nymMarkerToNymName.put("ipon", "hypo");
    nymMarkerToNymName.put("iperon", "hypo");
    nymMarkerToNymName.put("Hipônimos", "hypo");
    nymMarkerToNymName.put("Hiperônimos", "hyper");
    nymMarkerToNymName.put("Sinónimos", "syn");
    nymMarkerToNymName.put("Antónimos", "ant");
    nymMarkerToNymName.put("Hipónimos", "hypo");
    nymMarkerToNymName.put("Hiperónimos", "hyper");

  }

  protected final static Pattern sectionPattern;

  // TODO: handle pronunciation in italian
  private final static Pattern pronunciationPattern;
  protected final static Pattern level2HeaderPattern;

  static {
    level2HeaderPattern = Pattern.compile(level2HeaderPatternString, Pattern.MULTILINE);

    sectionPattern = Pattern.compile(entrySectionPatternString);
    pronunciationPattern = Pattern.compile(pronounciationPatternString);
  }

  private Block currentBlock;
  private int blockStart = -1;

  private String currentNym = null;

  private boolean isCorrectPOS;

  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    extractData(page);
    wdh.finalizePageExtraction();
  }

  public void extractData(WikiText page) {
    page.headers(2).stream().map(Token::asHeading).forEach(this::extractLvl2Section);
  }

  private void extractLvl2Section(Heading heading) {
    String lang = getLanguage(heading);
    if ("".equals(lang)) {
      return;
    }
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(lang)) {
      return;
    }
    String normalizedLanguage = validateAndStandardizeLanguageCode(lang);
    if (normalizedLanguage == null) {
      log.trace("Ignoring language section {} for {}", lang, getWiktionaryPageName());
      return;
    }
    wdh.initializeLanguageSection(normalizedLanguage);
    extractLanguageData(heading.getSection());
    wdh.finalizeLanguageSection();

  }

  private String getLanguage(Heading h) {
    Optional<String> lg =
        h.getContent().wikiTokens().stream().filter(tok -> tok instanceof Template)
            .map(Token::asTemplate).filter(tok -> tok.getName().trim().matches("-.*-")).map(tok -> {
              String name = tok.getName().trim();
              return name.substring(1, name.length() - 1);
            }).findFirst();
    if (lg.isPresent()) {
      return lg.get().toLowerCase();
    } else {
      log.debug("No language template in header [{}] in {}", h, getWiktionaryPageName());
      return "";
    }
  }


  private void extractLanguageData(WikiSection section) {
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(section.getBeginIndex(), section.getEndIndex());
    currentBlock = Block.NOBLOCK;
    while (m.find()) {
      HashMap<String, Object> context = new HashMap<>();
      Block nextBlock = computeNextBlock(m, context);

      if (nextBlock == null) {
        continue;
      }
      // If current block is IGNOREPOS, we should ignore everything but a new
      // DEFBLOCK/INFLECTIONBLOCK
      if (Block.IGNOREPOS != currentBlock
          || (Block.DEFBLOCK == nextBlock || Block.INFLECTIONBLOCK == nextBlock)) {
        leaveCurrentBlock(m);
        gotoNextBlock(nextBlock, context);
      }
    }
    // Finalize the entry parsing
    leaveCurrentBlock(m);
  }

  private boolean isItalian(Matcher l1) {
    // log.debug("Considering header == {}",l1.group(1));
    String t = l1.group(1).trim();
    return (t.startsWith("{{-it-") || t.startsWith("{{it"));
  }

  // TODO: variants, pronunciations and other elements are common to the different entries in the
  // page.
  protected void extractItalianData(int startOffset, int endOffset) {
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    wdh.initializeLanguageSection("it");
    currentBlock = Block.NOBLOCK;
    while (m.find()) {
      HashMap<String, Object> context = new HashMap<>();
      Block nextBlock = computeNextBlock(m, context);

      if (nextBlock == null) {
        continue;
      }
      // If current block is IGNOREPOS, we should ignore everything but a new
      // DEFBLOCK/INFLECTIONBLOCK
      if (Block.IGNOREPOS != currentBlock
          || (Block.DEFBLOCK == nextBlock || Block.INFLECTIONBLOCK == nextBlock)) {
        leaveCurrentBlock(m);
        gotoNextBlock(nextBlock, context);
      }
    }
    // Finalize the entry parsing
    leaveCurrentBlock(m);
    wdh.finalizeLanguageSection();
  }

  private Block computeNextBlock(Matcher m, Map<String, Object> context) {
    String title = m.group(1).trim();
    String nym;

    // -card- and -ord- are not section delimiters.
    if (title.startsWith("card") || title.startsWith("ord")) {
      return null;
    }

    context.put("start", m.end());

    if (title.startsWith("trad1")) {
      context.put("start", m.start()); // Keep trad1 in block
      return Block.TRADBLOCK;
    } else if (title.equals("trad")) {
      return Block.TRADBLOCK;
    } else if (title.equals("pron")) {
      return Block.PRONBLOCK;
    } else if (title.equals("var")) {
      return Block.ORTHOALTBLOCK;
    } else if (null != (nym = nymMarkerToNymName.get(title))) {
      context.put("nym", nym);
      return Block.NYMBLOCK;
    } else if (WiktionaryDataHandler.isValidPOS(title)) {
      context.put("pos", title);
      return Block.DEFBLOCK;
    } else {
      // WARN: in previous implementation, L2 headers where considered as ignoredpos.
      log.debug("Ignoring content of section {} in {}", title, this.getWiktionaryPageName());
      return Block.NOBLOCK;
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
      case DEFBLOCK:
        String pos = (String) context.get("pos");
        wdh.initializeLexicalEntry(pos);
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
        assert false : "Unexpected block while parsing: " + this.getWiktionaryPageName();
    }

  }

  private void leaveCurrentBlock(Matcher m) {
    if (blockStart == -1) {
      return;
    }

    int end = computeRegionEnd(blockStart, m);

    switch (currentBlock) {
      case NOBLOCK:
      case IGNOREPOS:
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
      default:
        assert false : "Unexpected block while parsing: " + this.getWiktionaryPageName();
    }

    blockStart = -1;
  }


  // TODO: do not create the gloss resource in the model if no translation is contained in the group
  // (some kind of lazy construction ?)
  private void extractTranslations(int startOffset, int endOffset) {
    WikiText wt = new WikiText(this.getWiktionaryPageName(), pageContent, startOffset, endOffset);
    List<? extends Token> toks = wt.wikiTokens();

    String currentGloss;
    Resource currentStructuredGloss = null;
    int glossRank = 1;
    int ti = 0;
    while (ti < toks.size()) {
      Token t = toks.get(ti);
      if (t instanceof Template) {
        Template tmpl = (Template) t;
        String tmplName = tmpl.getName().trim();
        if (tmplName.equalsIgnoreCase("trad1") || tmplName.equals("(")) {
          Map<String, WikiContent> args = ((Template) t).getArgs();
          currentGloss = (args.get("1") == null) ? null : args.get("1").toString().trim();
          if (isIgnorable(currentGloss)) {
            // Ignore the full translation block
            currentGloss = null;
            ti++;
            while (ti != toks.size() && !isClosingTranslationBlock(toks.get(ti))) {
              ti++;
            }
          } else {
            currentStructuredGloss = glossResource(currentGloss, glossRank++);
          }
        } else if (tmplName.equalsIgnoreCase("trad2") || tmplName.equals(")")) {
          currentGloss = null;
          currentStructuredGloss = null;
        } else if (tmplName.equalsIgnoreCase("Noetim")) {
          // Noetim comes in place of an etymology section and ends the translation section.
          ti = toks.size();
        }
      } else if (t instanceof IndentedItem) {
        // line of translations
        processTranslationLine(currentStructuredGloss, t.asIndentedItem());
      } else if (t instanceof Heading) {
        // Headings indicate the unexpected end of the translation section (error in the page or
        // specific headings)
        // Ignore the remaining data
        ti = toks.size();
      } else if (t instanceof Link) {
        // This only captures the links that are outside of an indentation
        Link l = (Link) t;
        String target = l.getFullTargetText();
        if (target.startsWith("Categoria:") || target.startsWith("File:")
            || target.startsWith("Image:")) {
          // Beginning of links to categories means end of translation section
          ti = toks.size();
        } else {
          // Links outside indentation are simply ignored
          log.trace("Unexpected link {} in {}", t, this.getWiktionaryPageName());
        }
      } else {
        log.debug("Unexpected token {} in {}", t, this.getWiktionaryPageName());
      }
      // TODO : check if entries are using other arguments
      ti++;
    }
  }

  private Resource glossResource(String currentGloss, int i) {
    return wdh.createGlossResource(currentGloss, i);
  }

  private void processTranslationLine(Resource gloss, IndentedItem t) {
    log.trace("Translation line: {} ||| {}", t.toString(), this.getWiktionaryPageName());
    WikiCharSequence line = new WikiCharSequence(t.getContent());
    TranslationLineParser tp = new TranslationLineParser(this.getWiktionaryPageName());
    tp.extractTranslationLine(line, gloss, wdh);
  }

  static String ignorableGlossPatternText = //
      "\\s*(?:" //
          + "(?:(?:\\dª|prima|seconda|terza)\\s+pers(?:ona|\\.)?)\\s+" //
          + "|(?:femminile|participio passato|plurale)\\s+" //
          + "|voce verbale" + ")";
  static Pattern ignorableGlossPattern = Pattern.compile(ignorableGlossPatternText);
  Matcher ignorableGloss = ignorableGlossPattern.matcher("");

  private boolean isIgnorable(String gloss) {
    if (gloss == null) {
      return false;
    }
    ignorableGloss.reset(gloss);
    if (ignorableGloss.lookingAt()) {
      log.debug("Ignoring gloss {} in {}", gloss, this.getWiktionaryPageName());
      return true;
    } else {
      log.debug("Considering gloss {} in {}", gloss, this.getWiktionaryPageName());
      return false;
    }
  }

  private boolean isClosingTranslationBlock(Token token) {
    if (token instanceof Template) {
      String n = ((Template) token).getName().trim();
      return n.equalsIgnoreCase("trad2") || n.equals(")");
    }
    return false;
  }

  protected final static String carPatternString;
  protected final static String macroOrLinkOrcarPatternString;


  static {
    // les caractères visible
    carPatternString = "(.)";

    // TODO: We should suppress multiline xml comments even if macros or line are to be on a single
    // line.
    macroOrLinkOrcarPatternString = "(?:" //
        + WikiPatterns.macroPatternString //
        + ")|(?:" //
        + WikiPatterns.linkPatternString //
        + ")|(?:" //
        + "(:*\\*)" //
        + ")|(?:" //
        + carPatternString //
        + ")";
  }

  protected final static Pattern macroOrLinkOrcarPattern;
  protected final static Pattern carPattern;

  static {
    carPattern = Pattern.compile(carPatternString);
    macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.DOTALL);
  }

  protected final int INIT = 1;
  protected final int LANGUE = 2;
  protected final int TRAD = 3;

  // TODO: delegate translation extraction to the appropriate wiki model
  private void extractTranslationsOld(int startOffset, int endOffset) {
    Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(pageContent);
    macroOrLinkOrcarMatcher.region(startOffset, endOffset);
    int ETAT = INIT;

    Resource currentGlose = null;
    String lang = null, word = "";
    String usage = "";
    String langname = "";
    int rank = 1;

    while (macroOrLinkOrcarMatcher.find()) {

      String g1 = macroOrLinkOrcarMatcher.group(1);
      String g3 = macroOrLinkOrcarMatcher.group(3);
      String g5 = macroOrLinkOrcarMatcher.group(5);
      String g6 = macroOrLinkOrcarMatcher.group(6);

      switch (ETAT) {

        case INIT:
          if (g1 != null) {
            if (g1.equalsIgnoreCase("trad1") || g1.equalsIgnoreCase("(")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGlose = wdh.createGlossResource(g, rank++);
              } else {
                currentGlose = null;
              }

            } else if (g1.equalsIgnoreCase("trad2") || g1.equalsIgnoreCase(")")) {
              currentGlose = null;
            } else if (g1.equalsIgnoreCase("mid")) {
              // ignore
            }
          } else if (g3 != null) {
            // System.err.println("Unexpected link while in INIT state.");
          } else if (g5 != null) {
            ETAT = LANGUE;
          } else if (g6 != null) {
            if (g6.equals(":")) {
              // System.err.println("Skipping ':' while in INIT state.");
            } else if (g6.equals("\n") || g6.equals("\r")) {

            } else if (g6.equals(",")) {
              // System.err.println("Skipping ',' while in INIT state.");
            } else {
              // System.err.println("Skipping " + g5 + " while in INIT state.");
            }
          }

          break;

        case LANGUE:

          if (g1 != null) {
            if (g1.equalsIgnoreCase("trad1") || g1.equalsIgnoreCase("(")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGlose = wdh.createGlossResource(g, rank++);
              } else {
                currentGlose = null;
              }
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else if (g1.equalsIgnoreCase("trad2") || g1.equalsIgnoreCase(")")) {
              currentGlose = null;
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else if (g1.equalsIgnoreCase("mid")) {
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else {
              langname = LangTools.normalize(g1);
            }
          } else if (g3 != null) {
            // System.err.println("Unexpected link while in LANGUE state.");
          } else if (g5 != null) {
            // System.err.println("Skipping '*' while in LANGUE state.");
          } else if (g6 != null) {
            if (g6.equals(":")) {
              lang = langname.trim();
              lang = stripParentheses(lang);
              lang = ItalianLangToCode.threeLettersCode(lang);
              langname = "";
              ETAT = TRAD;
            } else if (g6.equals("\n") || g6.equals("\r")) {
              // System.err.println("Skipping newline while in LANGUE state.");
            } else if (g6.equals(",")) {
              // System.err.println("Skipping ',' while in LANGUE state.");
            } else {
              langname = langname + g6;
            }
          }

          break;
        case TRAD:
          if (g1 != null) {
            if (g1.equalsIgnoreCase("trad1") || g1.equalsIgnoreCase("(")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGlose = wdh.createGlossResource(g, rank++);
              } else {
                currentGlose = null;
              }
              // if (word != null && word.length() != 0) {
              // lang=stripParentheses(lang);
              // wdh.registerTranslation(lang, currentGlose, usage, word);
              // }
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else if (g1.equalsIgnoreCase("trad2") || g1.equalsIgnoreCase(")")) {
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  wdh.registerTranslation(lang, currentGlose, usage, word);
                }
              }
              currentGlose = null;
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else if (g1.equalsIgnoreCase("mid")) {
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  wdh.registerTranslation(lang, currentGlose, usage, word);
                }
              }
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else {
              usage = usage + "{{" + g1 + "}}";
            }
          } else if (g3 != null) {
            word = word + " " + removeAnchor(g3);
          } else if (g5 != null) {
            // System.err.println("Skipping '*' while in LANGUE state.");
          } else if (g6 != null) {
            if (g6.equals("\n") || g6.equals("\r")) {
              usage = usage.trim();
              // System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " +
              // currentGlose);
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  wdh.registerTranslation(lang, currentGlose, usage, word);
                }
              }
              lang = null;
              usage = "";
              word = "";
              ETAT = INIT;
            } else if (g6.equals(",") || g6.equals(";") || g6.equals("/")) {
              usage = usage.trim();
              // System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " +
              // currentGlose);
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  wdh.registerTranslation(lang, currentGlose, usage, word);
                }
              }
              usage = "";
              word = "";
            } else {
              usage = usage + g6;
            }
          }
          break;
        default:
          System.err.println("Unexpected state number:" + ETAT);
          break;
      }


    }
  }

  private String removeAnchor(String g3) {
    if (null == g3) {
      return null;
    }
    int hash = g3.indexOf('#');
    if (-1 == hash) {
      return g3;
    } else {
      return g3.substring(0, hash);
    }
  }

  private void extractPron(int startOffset, int endOffset) {
    String pronCode = pageContent.substring(startOffset, endOffset);
    italianPronunciationExtractorWikiModel.parsePronunciation(pronCode);
  }

  @Override
  public Resource extractDefinition(String definition, int defLevel) {
    // TODO: properly handle macros in definitions.
    italianDefinitionExtractorWikiModel.parseDefinition(definition, defLevel);
    return null;
  }

  @Override
  public Resource extractExample(String example) {
    italianExampleExtractorWikiModel.parseExample(example);
    return null;
  }

  private ItalianDefinitionExtractorWikiModel italianDefinitionExtractorWikiModel;
  private ItalianExampleExtractorWikiModel italianExampleExtractorWikiModel;
  private ItalianPronunciationExtractorWikiModel italianPronunciationExtractorWikiModel;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    italianDefinitionExtractorWikiModel = new ItalianDefinitionExtractorWikiModel(this.wdh, this.wi,
        new Locale("it"), "/${image}", "/${title}");
    italianExampleExtractorWikiModel = new ItalianExampleExtractorWikiModel(this.wdh, this.wi,
        new Locale("it"), "/${image}", "/${title}");
    italianPronunciationExtractorWikiModel = new ItalianPronunciationExtractorWikiModel(this.wdh,
        this.wi, new Locale("it"), "/${image}", "/${title}");
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    italianDefinitionExtractorWikiModel.setPageName(this.getWiktionaryPageName());
    italianExampleExtractorWikiModel.setPageName(this.getWiktionaryPageName());
    italianPronunciationExtractorWikiModel.setPageName(this.getWiktionaryPageName());
  }
}
