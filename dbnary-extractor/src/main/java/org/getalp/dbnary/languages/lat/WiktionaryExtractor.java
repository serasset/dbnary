package org.getalp.dbnary.languages.lat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  // NOTE: to subclass the extractor, you need to define how a language section is recognized.
  // then, how are sections recognized and what is their semantics.
  // then, how to extract specific elements from the particular sections
  protected final static String languageSectionPatternString;
  protected final static String entrySectionPatternString;

  // TODO: handle morphological informations e.g. fr-rég template ?
  protected final static String pronunciationPatternString = "\\{\\{pron\\|([^|}]*)\\|([^}]*)}}";

  protected final static String otherFormPatternString = "\\{\\{fr-[^\\}]*\\}\\}";

  private static final HashMap<String, String> posMarkers;
  private static final HashSet<String> ignorablePosMarkers;
  private static final HashSet<String> ignorableSectionMarkers;

  private final static HashMap<String, String> nymMarkerToNymName;

  private static void addPos(String p, String n) {
    posMarkers.put(p, n);
  }

  static {
    // =={{-la-|Rosa}} ==
    languageSectionPatternString = "==\\s*\\{\\{-([^-]*)-\\|?([^\\}]*)\\}\\}\\s*==";

    // ==={{int:wikt-nomen-subst}}===
    entrySectionPatternString = "===?\\s*\\{\\{(?:int:)?([^}|]*)\\|?([^}]*)\\}\\}\\s*=?==";

    posMarkers = new HashMap<>(130);
    ignorablePosMarkers = new HashSet<>(130);

    addPos("wikt-nomen-subst", "nomen-subst");
    addPos("wikt-adverbium", "adverbium");
    addPos("adverbium", "adverbium");
    addPos("wikt-nomen-prop", "nomen-prop");
    addPos("proprium", "proprium");
    addPos("wikt-verbum", "verbum");
    addPos("verbum", "verbum");
    addPos("wikt-coniunctio", "coniunctio");
    addPos("wikt-verbum-tr", "verbum-tr");
    addPos("transitivum", "intransitivum");
    addPos("wikt-verbum-intr", "verbum-intr");
    addPos("intransitivum", "intransitivum");
    addPos("wikt-pronomen", "pronomen");
    addPos("pronomen", "pronomen");
    addPos("wikt-nomen-adj", "nomen-adj");
    addPos("adiectivum", "adiectivum");
    addPos("wikt-participium", "participium");
    addPos("wikt-praep", "praep");
    addPos("wikt-nomen", "nomen");
    addPos("nomen", "nomen");
    addPos("substantivum", "substantivum");
    addPos("abbreviatio", "abbreviatio");
    addPos("cardinalis", "cardinalis");
    addPos("ordinalis", "ordinalis");
    addPos("numerus", "numerus");
    addPos("interiectio", "interiectio");
    addPos("participium", "participium");
    addPos("praepositio", "praepositio");
    addPos("proverbium", "proverbium");

    ignorablePosMarkers.add("-flex-adj-indéf-");

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("-méro-", "mero");
    nymMarkerToNymName.put("-hyper-", "hyper");
    nymMarkerToNymName.put("-hypo-", "hypo");
    nymMarkerToNymName.put("-holo-", "holo");
    nymMarkerToNymName.put("-méton-", "meto");
    nymMarkerToNymName.put("-syn-", "syn");
    nymMarkerToNymName.put("-q-syn-", "qsyn");
    nymMarkerToNymName.put("-ant-", "ant");

    nymMarkerToNymName.put("méronymes", "mero");
    nymMarkerToNymName.put("méro", "mero");
    nymMarkerToNymName.put("hyperonymes", "hyper");
    nymMarkerToNymName.put("hyper", "hyper");
    nymMarkerToNymName.put("hyponymes", "hypo");
    nymMarkerToNymName.put("hypo", "hypo");
    nymMarkerToNymName.put("holonymes", "holo");
    nymMarkerToNymName.put("holo", "holo");
    nymMarkerToNymName.put("synonymes", "syn");
    nymMarkerToNymName.put("syn", "syn");
    nymMarkerToNymName.put("quasi-synonymes", "qsyn");
    nymMarkerToNymName.put("q-syn", "qsyn");
    nymMarkerToNymName.put("quasi-syn", "qsyn");
    nymMarkerToNymName.put("antonymes", "ant");
    nymMarkerToNymName.put("ant", "ant");
    nymMarkerToNymName.put("anto", "ant");
    nymMarkerToNymName.put("troponymes", "tropo");
    nymMarkerToNymName.put("tropo", "tropo");

    // paronymes, gentillés ?

    // Check if these markers still exist in new french organization...
    ignorableSectionMarkers = new HashSet<>(200);
    ignorableSectionMarkers.addAll(posMarkers.keySet());
    ignorableSectionMarkers.addAll(nymMarkerToNymName.keySet());
    ignorableSectionMarkers.add("-étym-");
    ignorableSectionMarkers.add("-voc-");
    ignorableSectionMarkers.add("-trad-");
    ignorableSectionMarkers.add("-note-");
    ignorableSectionMarkers.add("-réf-");
    ignorableSectionMarkers.add("clé de tri");
    ignorableSectionMarkers.add("-anagr-");
    ignorableSectionMarkers.add("-drv-");
    ignorableSectionMarkers.add("-voir-");
    ignorableSectionMarkers.add("-pron-");
    ignorableSectionMarkers.add("-gent-");
    ignorableSectionMarkers.add("-apr-");
    ignorableSectionMarkers.add("-paro-");
    ignorableSectionMarkers.add("-homo-");
    ignorableSectionMarkers.add("-exp-");
    ignorableSectionMarkers.add("-compos-");
    // DONE: prendre en compte la variante orthographique (différences avec -ortho-alt- ?)
    ignorableSectionMarkers.add("-var-ortho-");

    // TODO trouver tous les modèles de section...

    // affixesToDiscardFromLinks = new HashSet<String>();
    // affixesToDiscardFromLinks.add("s");
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }


  protected final static Pattern languageSectionPattern;
  protected final static Pattern entrySectionPattern;
  protected final static Pattern pronunciationPattern;
  protected final static Pattern otherFormPattern;

  static {
    languageSectionPattern = Pattern.compile(languageSectionPatternString);
    entrySectionPattern = Pattern.compile(entrySectionPatternString);
    pronunciationPattern = Pattern.compile(pronunciationPatternString);
    otherFormPattern = Pattern.compile(otherFormPatternString);
  }

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK
  }

  private Block currentBlock = Block.NOBLOCK;
  private int blockStart = -1;

  private String currentNym = null;

  protected ExampleExpanderWikiModel exampleExpander;
  protected DefinitionExtractorWikiModel definitionExpander;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    exampleExpander = new ExampleExpanderWikiModel(wi, new Locale("la"),
        "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    definitionExpander = new DefinitionExtractorWikiModel(this.wdh, this.wi, new Locale("la"),
        "/${image}", "/${title}");
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    exampleExpander.setPageName(this.getWiktionaryPageName());
    definitionExpander.setPageName(this.getWiktionaryPageName());
  }

  private final Set<String> defTemplates = null;

  public String getLanguageInHeader(Matcher m) {
    if (null != m.group(1)) {
      if ("lingua".equals(m.group(1))) {
        Map<String, String> argmap = WikiTool.parseArgs(m.group(2));
        return argmap.get("1");
      }
      return m.group(1);
    }

    return null;
  }

  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    Matcher languageFilter = languageSectionPattern.matcher(pageContent);
    int startSection = -1;

    // exampleExpander = new ExampleExpanderWikiModel(wi, frLocale, this.wiktionaryPageName, "");

    String nextLang, lang = null;

    while (languageFilter.find()) {
      nextLang = getLanguageInHeader(languageFilter);
      extractData(startSection, languageFilter.start(), lang);
      lang = nextLang;
      startSection = languageFilter.end();
    }

    // Either the filter is at end of sequence or on French language header.
    if (languageFilter.hitEnd() && startSection != -1) {
      extractData(startSection, pageContent.length(), lang);
    }
    wdh.finalizePageExtraction();
  }


  protected void extractData(int startOffset, int endOffset, String lang) {
    if (lang == null) {
      return;
    }

    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !"la".equals(lang))
      return;

    wdh.initializeLanguageSection(lang);
    Matcher m = entrySectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);

    // WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are
    // not macro based ?
    // DONE: (priority: top) link the definition node with the current Part of Speech
    // DONE: handle alternative spelling

    currentBlock = Block.NOBLOCK;

    // Iterate on entry sections
    while (m.find()) {
      // We are in a new block
      HashMap<String, Object> context = new HashMap<>();
      Block nextBlock = computeNextBlock(m, context);

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
    String sectionTitle = m.group(1);
    String pos, nym;
    context.put("start", m.end());

    if (sectionTitle != null) {
      if (ignorablePosMarkers.contains(sectionTitle)) {
        return Block.IGNOREPOS;
      } else if ((pos = getPos(m)) != null) {
        context.put("pos", pos);
        return Block.DEFBLOCK;
      } else if (isTranslation(m)) {
        return Block.TRADBLOCK;
      } else if (isAlternate(sectionTitle)) {
        return Block.ORTHOALTBLOCK;
      } else if (null != (nym = getNymHeader(sectionTitle))) {
        context.put("nym", nym);
        return Block.NYMBLOCK;
      } else if (ignorableSectionMarkers.contains(sectionTitle)) {
        return Block.NOBLOCK;
      } else {
        log.debug("Unknown section title {} in {}", m.group(), this.getWiktionaryPageName());
        return Block.NOBLOCK;
      }
    } else {
      log.debug("Null section title in {}", this.getWiktionaryPageName());
      return Block.NOBLOCK;
    }
  }


  private String getPos(Matcher m) {
    String templateName = m.group(1);
    if ("S".equals(templateName)) {
      Map<String, String> argmap = WikiTool.parseArgs(m.group(2));
      templateName = argmap.get("1");
    }
    return templateName == null ? null : posMarkers.get(templateName);
  }

  private boolean isTranslation(Matcher m) {
    String templateName = m.group(1);
    if ("S".equals(templateName)) {
      Map<String, String> argmap = WikiTool.parseArgs(m.group(2));
      templateName = argmap.get("1");
    }
    return "wikt-trans".equals(templateName) || "trans".equals(templateName);
  }


  private void gotoNextBlock(Block nextBlock, HashMap<String, Object> context) {
    currentBlock = nextBlock;
    Object start = context.get("start");
    blockStart = (null == start) ? -1 : (int) start;
    switch (nextBlock) {
      case NOBLOCK:
      case IGNOREPOS:
      case INFLECTIONBLOCK:
        break;
      case DEFBLOCK:
        String pos = (String) context.get("pos");
        wdh.initializeLexicalEntry(pos);
        // if ("-verb-".equals(pos)) {
        // wdh.registerPropertyOnCanonicalForm(LexinfoOnt.verbFormMood, LexinfoOnt.infinitive);
        // }
        break;
      case TRADBLOCK:
        break;
      case ORTHOALTBLOCK:
        break;
      case NYMBLOCK:
        currentNym = (String) context.get("nym");
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

    int end = computeRegionEnd(blockStart, m);

    switch (currentBlock) {
      case NOBLOCK:
      case IGNOREPOS:
        break;
      case INFLECTIONBLOCK:
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
      default:
        assert false
            : "Unexpected block while ending extraction of entry: " + getWiktionaryPageName();
    }

    blockStart = -1;
  }


  private static final Set<String> variantSections = new HashSet<>();

  static {
    variantSections.add("variantes");
    variantSections.add("var");
    variantSections.add("variantes ortho");
    variantSections.add("var-ortho");
    variantSections.add("variantes orthographiques");
    variantSections.add("variantes dialectales");
    variantSections.add("dial");
    variantSections.add("var-dial");
    variantSections.add("variantes dial");
    variantSections.add("variantes dialectes");
    variantSections.add("dialectes");
    variantSections.add("anciennes orthographes");
    variantSections.add("ortho-arch");
    variantSections.add("anciennes ortho");
  }

  private boolean isAlternate(String sectionTitle) {
    return (sectionTitle != null) && variantSections.contains(sectionTitle);
  }


  private String getNymHeader(String sectionTitle) {
    if (sectionTitle != null) {
      return nymMarkerToNymName.get(sectionTitle);
    }

    return null;
  }


  private void extractTranslations(int startOffset, int endOffset) {
    // TODO: parse wiki text and extract using a grammar !!!
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    macroMatcher.region(startOffset, endOffset);
    Resource currentGloss = null;

    while (macroMatcher.find()) {
      String g1 = macroMatcher.group(1);

      if (g1.equals("x") || g1.equals("xlatio") || g1.equals("xlatio-d") || g1.equals("xlatio2")
          || g1.equals("xlatio0") || g1.equals("t+") || g1.equals("t") || g1.equals("t-")) {
        String g2 = macroMatcher.group(2);
        Map<String, String> args = WikiTool.parseArgs(g2);
        String lang = LatinLangtoCode.threeLettersCode(args.get("1"));
        String word = args.get("2");
        args.remove("1");
        args.remove("2");
        String usage = null;
        if (!args.isEmpty()) {
          usage = args.toString();
        }
        if (lang != null) {
          wdh.registerTranslation(lang, currentGloss, usage, word);
        }
      } else if (g1.equals("xlatio-var")) {
        String g2 = macroMatcher.group(2);
        Map<String, String> args = WikiTool.parseArgs(g2);
        String lang = LatinLangtoCode.threeLettersCode(args.get("1"));
        String variant = args.get("2");
        String word = args.get("3");
        args.remove("1");
        args.remove("2");
        args.remove("3");
        args.put("variant", variant);
        String usage = null;
        if (!args.isEmpty()) {
          usage = args.toString();
        }
        if (lang != null) {
          wdh.registerTranslation(lang, currentGloss, usage, word);
        }
      } else if (g1.equals("=sum=")) {
        // The current latin macro does not allow glosses...

      } else if (g1.equals("trans-tab")) {
        // Warn: trans-tab is a template where all translations are part of the second arg.
        // As the template will fail catching it, we should only use the first arg and reparse
        // the rest (that will be part of a translation value).
        Map<String, String> argmap = WikiTool.parseArgs(macroMatcher.group(2));
        String g = argmap.get("1");
        if (null != g)
          currentGloss = wdh.createGlossResource(g);
        int secondArgOffset = macroMatcher.toMatchResult().end(1) + 1; // position of the first arg
        secondArgOffset = secondArgOffset + (null != g ? g.length() : 0) + 1;
        macroMatcher.reset().region(secondArgOffset, endOffset);
      } else if (g1.equals("trans-top")) {
        Map<String, String> argmap = WikiTool.parseArgs(macroMatcher.group(2));
        String g = argmap.get("1");
        if (null != g)
          currentGloss = wdh.createGlossResource(g);
      } else if (g1.equals("=med=") || g1.equals("trans-mid")) {
        // just ignore it
      } else if (g1.equals("=ima=") || g1.equals("trans-bottom")) {
        // Forget the current glose
        currentGloss = null;
      } else if ((g1.length() == 2 || g1.length() == 3)
          && LatinLangtoCode.threeLettersCode(g1) != null) {
        // this a a language identifier, just ignore it as we get the language id from the trad
        // macro parameter.
      } else {
        log.debug("Unexpected template {} in translations for {}", g1, getWiktionaryPageName());
      }
    }
  }


  public Resource extractExample(String example) {
    Set<Pair<Property, RDFNode>> context = new HashSet<>();

    String ex = exampleExpander.expandExample(example, defTemplates, context);
    if (ex != null && !ex.equals("")) {
      wdh.registerExample(ex, context);
    }
    return null;
  }

  @Override
  public Resource extractDefinition(String definition, int defLevel) {
    // TODO: properly handle macros in definitions.
    definitionExpander.parseDefinition(definition, defLevel);
    return null;
  }

}
