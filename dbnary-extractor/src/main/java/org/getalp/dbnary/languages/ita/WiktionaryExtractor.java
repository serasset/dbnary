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

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK, PRONBLOCK
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

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
  protected final static Pattern level2HeaderPattern;

  static {
    level2HeaderPattern = Pattern.compile(level2HeaderPatternString, Pattern.MULTILINE);

    sectionPattern = Pattern.compile(entrySectionPatternString);
  }

  private ItalianExpandAllWikiModel expander;
  private ItalianDefinitionExtractorWikiModel italianDefinitionExtractorWikiModel;
  private ItalianExampleExtractorWikiModel italianExampleExtractorWikiModel;
  private ItalianPronunciationExtractorWikiModel italianPronunciationExtractorWikiModel;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    expander = new ItalianExpandAllWikiModel(this.wi, new Locale("it"), "/${image}", "/${title}");
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
    String pagename = this.getWiktionaryPageName();
    expander.setPageName(pagename);
    italianDefinitionExtractorWikiModel.setPageName(pagename);
    italianExampleExtractorWikiModel.setPageName(pagename);
    italianPronunciationExtractorWikiModel.setPageName(pagename);
  }

  private Block currentBlock;
  private int blockStart = -1;

  private String currentNym = null;

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
      case ORTHOALTBLOCK:
      case TRADBLOCK:
      case PRONBLOCK:
        break;
      case DEFBLOCK:
        String pos = (String) context.get("pos");
        wdh.initializeLexicalEntry(pos);
        break;
      case NYMBLOCK:
        currentNym = (String) context.get("nym");
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
            do {
              ti++;
            } while (ti != toks.size() && !isClosingTranslationBlock(toks.get(ti)));
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
    if (currentGloss != null) {
      // Use a shared ExpandAllWikiModel to expand the currentGloss
      expander.expandAll(currentGloss, null);
    }
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

}
