package org.getalp.dbnary.languages.swe;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author malick
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


  static Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString = "==([^=\\s]*)==";

  protected final static String sectionPatternString = "={2,4}\\s*([^=]*)\\s*={2,4}";

  protected final static String pronPatternString =
      "\\*\\{\\{\\s*([^\\}\\|\n\r]*)\\s*\\|ipa=([^\\}\\|]*)\\|?"; // detecte le bloc de
  // prononciation

  public final static String examplePatternString = "^#:\\s*(.*)$";

  protected final static String defOrExamplePatternString;

  protected final static String nymSensePatternString = ";(.*)";

  protected final static String nymPatternString;

  protected final static String nymAndUsagePatternString;

  protected SwedishMorphologyExtractor morphologyExtractor;


  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK, PRONBLOCK
  }

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected final static Pattern languageSectionPattern;
  protected final static Pattern sectionPattern;
  static Pattern defOrExamplePattern;
  protected final static HashMap<String, String> nymMarkerToNymName;
  protected final static Pattern pronPattern;
  protected final static Pattern nymPattern;


  static {

    nymAndUsagePatternString =
        new StringBuilder().append("\\[\\[").append("([^\\]\\|\n\r]*)(?:\\|([^\\]\n\r]*))?")
            .append("\\]\\]\\s*(''\\(([^\\)]*)\\)'')?(?:\\(''([^\\)]*)''\\))?").toString();

    defOrExamplePatternString = new StringBuilder().append("(?:")
        .append(WikiPatterns.definitionPatternString).append(")|(?:").append(examplePatternString)
        .append(")|(?:").append(pronPatternString).append(")").toString();

    nymPatternString = new StringBuilder().append("(?:").append(nymSensePatternString)
        .append(")|(?:").append(nymAndUsagePatternString).append(")").toString();

    defOrExamplePattern = Pattern.compile(defOrExamplePatternString, Pattern.MULTILINE);

    languageSectionPattern = Pattern.compile(languageSectionPatternString);

    sectionPattern = Pattern.compile(sectionPatternString);
    pronPattern = Pattern.compile(pronPatternString);
    nymPattern = Pattern.compile(nymPatternString);

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("Synonymer", "syn");
    nymMarkerToNymName.put("Antonymer", "ant");
    nymMarkerToNymName.put("Hyponiemen", "hypo");


  }

  private Block currentBlock;
  private int blockStart = -1;

  private String currentNym = null;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    morphologyExtractor = new SwedishMorphologyExtractor(wdh, wi);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String,
   * org.getalp.blexisma.semnet.SemanticNetwork)
   */
  @Override
  public void extractData() {

    wdh.initializePageExtraction(getWiktionaryPageName());
    Matcher languageFilter = sectionPattern.matcher(pageContent);
    while (languageFilter.find() && !languageFilter.group(1).equals("Svenska")) {
      ;
    }
    // Either the filter is at end of sequence or on Swenden language header.
    if (languageFilter.hitEnd()) {
      // There is no sweden data in this page.
      return;
    }
    int swedenSectionStartOffset = languageFilter.end();
    // Advance till end of sequence or new language section
    while (languageFilter.find() && languageFilter.group().charAt(2) == '=') {
      ;
    }
    // languageFilter.find();
    int swedenSectionEndOffset =
        languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractSwedishData(swedenSectionStartOffset, swedenSectionEndOffset);
    wdh.finalizePageExtraction();
  }

  protected void extractSwedishData(int startOffset, int endOffset) {
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    wdh.initializeLanguageSection("sv");
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
    String title = (m.group(1) != null) ? m.group(1) : m.group(2);
    String nym;
    context.put("start", m.end());
    if (title.equals("utta")) {
      return Block.PRONBLOCK;
    } else if (WiktionaryDataHandler.isValidPOS(title)) {
      context.put("pos", title);
      return Block.DEFBLOCK;
    } else if (title.equals("Översättningar")) {
      return Block.TRADBLOCK;
    } else if (null != (nym = nymMarkerToNymName.get(title))) {
      context.put("nym", nym);
      return Block.NYMBLOCK;
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
        extractMorphology(blockStart, end);
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
        assert false : "Unexpected block while parsing: " + getWiktionaryPageName();
    }

    blockStart = -1;
  }

  private void extractMorphology(int blockStart, int end) {
    WikiText txt = new WikiText(getWiktionaryPageName(), pageContent.substring(blockStart, end));

    txt.templatesOnUpperLevel().stream().map(Token::asTemplate)
        .filter(WiktionaryExtractor::isMorphologyTableTemplate).forEach(tmpl -> morphologyExtractor
            .extractMorphologicalData(tmpl.toString(), getWiktionaryPageName()));
  }

  private static boolean isMorphologyTableTemplate(Template template) {
    String name = template.getName();
    return name.startsWith("sv-subst") || name.startsWith("sv-adj") || name.startsWith(("sv-verb"))
        || name.startsWith("sv-adv") || name.startsWith("sv-artikel") || name.startsWith("sv-pron");
  }

  private void extractTranslations(int startOffset, int endOffset) {
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    macroMatcher.region(startOffset, endOffset);
    Resource currentGloss = null;
    // {{ö+|en|passport}}
    int rank = 1;

    while (macroMatcher.find()) {
      String g1 = macroMatcher.group(1);

      if (g1.equals("ö+") || g1.equals("ö")) {
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
      } else if (g1.equals("ö-topp")) {
        // Get the glose that should help disambiguate the source acception
        String g2 = macroMatcher.group(2);
        // Ignore glose if it is a macro
        if (g2 != null && !g2.startsWith("{{")) {
          currentGloss = wdh.createGlossResource(g2, rank++);
        }
      } else if (g1.equals("ö-botten")) {
        // on remet le gloss à null à la fin du bloc de traduction
        currentGloss = null;
      }
    }
  }


  @Override
  protected void extractDefinitions(int startOffset, int endOffset) {
    String pron;
    Matcher defOrExampleMatcher = defOrExamplePattern.matcher(pageContent);
    defOrExampleMatcher.region(startOffset, endOffset);
    // TODO : Swedish definition that contain ordered sublists are indeed mathematical definitions
    // where sublists are part of the definition
    while (defOrExampleMatcher.find()) {
      if (null != defOrExampleMatcher.group(1)) { // extraire les definitions
        extractDefinition(defOrExampleMatcher);
      } else if ((null != defOrExampleMatcher.group(2))) { // extraire les exemples
        extractExample(defOrExampleMatcher);
      } else if ((null != defOrExampleMatcher.group(4))) { // extraire les pronontiations
        pron = defOrExampleMatcher.group(4);
        if (defOrExampleMatcher.group(3).equals("uttal") && !pron.equals(" ")) // les prononciations
        // commencent
        // toujours par uttal
        {
          wdh.registerPronunciation(pron, "sv-fonipa");
        }
      }
    }

  }


  @Override
  protected void extractNyms(String synRelation, int startOffset, int endOffset) {

    Matcher nymSenseMatcher = nymPattern.matcher(this.pageContent);
    nymSenseMatcher.region(startOffset, endOffset);
    Resource gloss = null;

    while (nymSenseMatcher.find()) {
      if (nymSenseMatcher.group(1) != null) {
        gloss = wdh.createGlossResource(nymSenseMatcher.group(1));
      } else {
        String leftGroup = nymSenseMatcher.group(2);
        String usage = (nymSenseMatcher.group(5) != null) ? nymSenseMatcher.group(5)
            : nymSenseMatcher.group(6);
        if (leftGroup != null && !leftGroup.equals("") && !leftGroup.startsWith("Wikisaurus:")
            && !leftGroup.startsWith("Catégorie:") && !leftGroup.startsWith("#")) {

          wdh.registerNymRelation(leftGroup, synRelation, gloss, usage);
          usage = null;

        }
      }
    }
  }


}
