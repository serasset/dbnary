package org.getalp.dbnary.languages.nld;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author malick
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


  static Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);
  protected final static String languageSectionPatternString = "\\{\\{=\\s*([^=}]+)\\s*=\\}\\}";

  protected final static String sectionPatternString;


  protected final static String pronPatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";
  private ExpandAllWikiModel defOrExampleExpander;


  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK, PRONBLOCK, DERIVATIONBLOCK
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    defOrExampleExpander =
        new ExpandAllWikiModel(wi, new Locale("nl"), "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
  }

  protected final static Pattern languageSectionPattern;
  protected final static Pattern sectionPattern;
  static Pattern defOrExamplePattern;
  protected final static HashMap<String, String> nymMarkerToNymName;
  protected final static Pattern pronPattern;

  static {

    String examplePatternString = new StringBuilder().append("\\{\\{\\s*")
        .append("([^\\}\\|\n\r]*)\\s*\\|([^\n\r]*)").append("(?:\\}\\})$").toString();

    String defOrExamplePatternString =
        new StringBuilder().append("(?:").append(WikiPatterns.definitionPatternString)
            .append(")|(?:").append(examplePatternString).append(")").toString();

    sectionPatternString = new StringBuilder().append("\\{\\{\\s*-")
        .append("([^\\}\\|\n\r]*)-\\s*(?:\\|([^\\}\n\r]*))?").append("\\}\\}").toString();

    defOrExamplePattern = Pattern.compile(defOrExamplePatternString, Pattern.MULTILINE);
    // defOrExamplePattern = Pattern.compile(examplePatternString);

    languageSectionPattern = Pattern.compile(languageSectionPatternString);

    sectionPattern = Pattern.compile(sectionPatternString);
    pronPattern = Pattern.compile(pronPatternString);

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("synoniems", "syn");
    nymMarkerToNymName.put("Antoniemen", "ant");
    nymMarkerToNymName.put("Hyponiemen", "hypo");
    nymMarkerToNymName.put("syn", "syn");
    nymMarkerToNymName.put("ant", "ant");
    nymMarkerToNymName.put("hypo", "hypo");
    nymMarkerToNymName.put("hyper", "hyper");
    nymMarkerToNymName.put("holo", "holo");
    nymMarkerToNymName.put("mero", "mero");
    /*
     * nymMarkerToNymName.put("Hypernyms", "hyper"); nymMarkerToNymName.put("Meronyms", "mero");
     * nymMarkerToNymName.put("Holonyms", "holo"); nymMarkerToNymName.put("Troponyms", "tropo");
     */

  }

  private Block currentBlock;
  private int blockStart = -1;

  private String currentNym = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String,
   * org.getalp.blexisma.semnet.SemanticNetwork)
   */
  @Override
  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());

    Matcher languageFilter = languageSectionPattern.matcher(pageContent);

    int nldStart = -1;
    // on parcours la page pour trouver la partie netherlandais
    while (languageFilter.find()) {
      if (languageFilter.group(1).equals("nld")) {
        if (nldStart != -1) {
          extractNetherlandData(nldStart, languageFilter.start());
        }
        nldStart = languageFilter.end();
      } else {
        if (nldStart != -1) {
          extractNetherlandData(nldStart, languageFilter.start());
        }
        nldStart = -1;
      }
    }

    // Either the filter is at end of sequence or on netherland language header.
    if (languageFilter.hitEnd()) {
      // There is no netherland data in this page.
      if (nldStart != -1) {
        extractNetherlandData(nldStart, pageContent.length());
      }

    }
    wdh.finalizePageExtraction();
  }

  protected void extractNetherlandData(int startOffset, int endOffset) {
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    wdh.initializeLanguageSection("nl");
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
    String title = m.group(1);
    String nym;
    context.put("start", m.end());

    if (title.equals("pron")) {
      return Block.PRONBLOCK;
    } else if (WiktionaryDataHandler.isValidPOS(title)) {
      // TODO: sometimes, -verb- indicates a verb form (usually with first arg = 0)
      context.put("pos", title);
      return Block.DEFBLOCK;
    } else if (title.equals("trans")) {
      return Block.TRADBLOCK;
    } else if (title.equals("drv")) {
      // TODO: handle derivations
      return Block.DERIVATIONBLOCK;
    } else if (title.equals("expr")) {
      // TODO: handle expressions and sayings as derivations
      return Block.NOBLOCK;
    } else if (null != (nym = nymMarkerToNymName.get(title))) {
      context.put("nym", nym);
      return Block.NYMBLOCK;
    } else if (title.equals("l")
        || (title.length() > 2 && title.substring(0, 2).equals(wdh.getCurrentEntryLanguage()))) {
      // The special -l- template is not a new block, but a continuation of the previous block
      log.trace("Template -{}- in {} is not a section template", title,
          this.getWiktionaryPageName());
      return null;
    } else {
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
      case DERIVATIONBLOCK:
      case PRONBLOCK:
      case TRADBLOCK:
      case ORTHOALTBLOCK:
        break;
      case DEFBLOCK:
        String pos = (String) context.get("pos");
        wdh.initializeLexicalEntry(pos);
        break;
      case NYMBLOCK:
        currentNym = (String) context.get("nym");
        break;
      default:
        assert false : "Unexpected block while parsing: " + getWiktionaryPageName();
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
      case DERIVATIONBLOCK:
        extractDerivationSection(blockStart, end);
    }

    blockStart = -1;
  }

  private void extractTranslations(int startOffset, int endOffset) {
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    macroMatcher.region(startOffset, endOffset);
    Resource currentGloss = null;
    int rank = 1;
    // TODO: there are templates called "qualifier" used to further qualify the translation check
    // and evaluate if extracting its data is useful.
    while (macroMatcher.find()) {
      String g1 = macroMatcher.group(1);

      if (g1.equals("trad")) {
        // DONE: Sometimes translation links have a remaining info after the word, keep it.
        String g2 = macroMatcher.group(2);
        int i1, i2;
        String lang, word;
        if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
          lang = LangTools.normalize(g2.substring(0, i1));

          String usage = null;
          if ((i2 = g2.indexOf('|', i1 + 1)) == -1) {
            word = g2.substring(i1 + 1);
          } else {
            word = g2.substring(i1 + 1, i2);
            usage = g2.substring(i2 + 1);
          }
          // lang=NetherlandLangToCode.threeLettersCode(lang);
          if (lang != null) {
            wdh.registerTranslation(lang, currentGloss, usage, word);
          }
        }
      } else if (g1.equals("trans-top")) {
        // Get the glose that should help disambiguate the source acception
        String g2 = macroMatcher.group(2);
        // Ignore glose if it is a macro
        if (g2 != null && !g2.startsWith("{{")) {
          currentGloss = wdh.createGlossResource(g2, rank++);
        }
      } else if (g1.equals("trans-bottom")) {
        // Forget the current glose
        currentGloss = null;
      }
    }
  }


  private void extractPron(int startOffset, int endOffset) {

    Matcher pronMatcher = pronPattern.matcher(pageContent);
    pronMatcher.region(startOffset, endOffset);
    while (pronMatcher.find()) {
      String pron = pronMatcher.group(1);
      if (null == pron || pron.equals("")) {
        return;
      }

      if (!pron.equals("")) {
        wdh.registerPronunciation(pron, "nl-fonipa");
      }
    }
  }

  @Override
  protected void extractDefinitions(int startOffset, int endOffset) {

    Matcher defOrExampleMatcher = defOrExamplePattern.matcher(pageContent);
    defOrExampleMatcher.region(startOffset, endOffset);
    while (defOrExampleMatcher.find()) {
      if (null != defOrExampleMatcher.group(1)) {
        extractDefinition(defOrExampleMatcher);
      } else if ((null != defOrExampleMatcher.group(3))
          && (defOrExampleMatcher.group(2).equals("bijv-1"))) { // Les exemples commencent toujours
        // par bijv-1
        extractExample(defOrExampleMatcher);
      }
    }

  }

  // TODO : should be the default behaviour for all languages.
  @Override
  public Resource extractDefinition(String definition, int defLevel) {
    defOrExampleExpander.setPageName(getWiktionaryPageName());
    String def = defOrExampleExpander.expandAll(definition, null);
    if (def != null && !def.equals("")) {
      wdh.registerNewDefinition(def, defLevel);
    }
    return null;
  }

  @Override
  public void extractExample(Matcher definitionMatcher) {
    String example = definitionMatcher.group(3);
    extractExample(example);
  }

  // TODO : should be the default behaviour for all languages.
  @Override
  public Resource extractExample(String example) {
    defOrExampleExpander.setPageName(getWiktionaryPageName());
    String ex = defOrExampleExpander.expandAll(example, null);
    if (ex != null && !ex.equals("")) {
      wdh.registerExample(ex, null);
    }
    return null;
  }

  private void extractDerivationSection(int startOffset, int endOffset) {
    WikiText blockText = new WikiText(getWiktionaryPageName(), pageContent, startOffset, endOffset);
    ClassBasedFilter linksInLists = new ClassBasedFilter();
    linksInLists.enterIndentedItem().allowLink().allowTemplates();
    for (Token t : blockText.filteredTokens(linksInLists)) {
      if (t instanceof WikiText.Link) {
        wdh.registerDerivation(t.asLink().getLinkText());
      } else {
        log.trace("Ignoring token {} in derivation section", t);
      }
    }

  }
}
