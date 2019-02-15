/**
 *
 */
package org.getalp.dbnary.eng;

import static org.getalp.dbnary.IWiktionaryDataHandler.Feature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.Pair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.ClassBasedSequenceFilter;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiSection;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset, pantaleo
 */
// TODO: deal with {{compound|word1={{t|la|in}}|word2={{...}}}}
// TODO: deal with onomatopoietic in etymology
// TODO: deal with equivalent to compound in etymology
// TODO: register alternative forms section
// TODO: PARSE * and lemmas like bheh2ǵos
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  // DONE: Handle Wikisaurus entries.
  // DONE: extract pronunciation
  // TODO: attach multiple pronounciation correctly
  static Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString = "==\\s*([^=]*)\\s*==";
  protected final static String sectionPatternString = "={2,5}\\s*([^=]*)\\s*={2,5}";
  protected final static String pronPatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK, CONJUGATIONBLOCK, ETYMOLOGYBLOCK, DERIVEDBLOCK, DESCENDANTSBLOCK, PRONBLOCK
  }

  private WiktionaryDataHandler ewdh; // English specific version of the data handler.

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    if (wdh instanceof WiktionaryDataHandler) {
      ewdh = (WiktionaryDataHandler) wdh;
    } else {
      log.error("English Wiktionary Extractor instanciated with a non english data handler!");
    }
  }

  protected static Pattern languageSectionPattern;
  protected final static Pattern sectionPattern;
  protected final static Pattern pronPattern;

  static {
    languageSectionPattern = Pattern.compile(languageSectionPatternString);

    sectionPattern = Pattern.compile(sectionPatternString);
    pronPattern = Pattern.compile(pronPatternString);

    // TODO: Treat Abbreviations and Acronyms and contractions and Initialisms
    // TODO: Alternative forms
    // TODO: Extract quotations from definition block + from Quotations section

  }

  private Block currentBlock;
  private int blockStart = -1;

  private String currentNym = null;
  private ExpandAllWikiModel wikiExpander;
  protected EnglishDefinitionExtractorWikiModel definitionExpander;
  private WikisaurusExtractor wikisaurusExtractor;


  @Override
  public void setWiktionaryIndex(WiktionaryIndex wi) {
    super.setWiktionaryIndex(wi);
    wikiExpander =
        new ExpandAllWikiModel(wi, Locale.ENGLISH, "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    definitionExpander = new EnglishDefinitionExtractorWikiModel(this.wdh, this.wi,
        new Locale("en"), "/${image}", "/${title}");
    wikisaurusExtractor = new WikisaurusExtractor(this.ewdh);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String,
   * org.getalp.blexisma.semnet.SemanticNetwork)
   */
  @Override
  public void extractData() {
    // TODO: adapt extractor to allow extraction of foreign data.
    wdh.initializePageExtraction(wiktionaryPageName);
    Matcher languageFilter = sectionPattern.matcher(pageContent);
    while (languageFilter.find() && !languageFilter.group(1).equals("English")) {
      // NOP
    }
    // Either the filter is at end of sequence or on English language header.
    if (languageFilter.hitEnd()) {
      // There is no English data in this page.
      return;
    }
    int englishSectionStartOffset = languageFilter.end();
    // Advance till end of sequence or new language section
    while (languageFilter.find() && languageFilter.group().charAt(2) == '=') {
      // NOP
    }
    // languageFilter.find();
    int englishSectionEndOffset =
        languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractEnglishData(englishSectionStartOffset, englishSectionEndOffset);
    wdh.finalizePageExtraction();
  }

  @Override
  public boolean filterOutPage(String pagename) {
    // Extract Wikisaurus and Reconstructed pages...
    return !isWikisaurus(pagename) && !isReconstructed(pagename) && super.filterOutPage(pagename);
  }

  private boolean isReconstructed(String pagename) {
    return pagename.startsWith("Reconstruction:");
  }

  private boolean isWikisaurus(String pagename) {
    return pagename.startsWith("Wikisaurus:") || pagename.startsWith("Thesaurus:");
  }

  private String cutNamespace(String pagename) {
    int p = pagename.indexOf(":");
    return pagename.substring(p + 1);
  }

  protected void extractEnglishData(int startOffset, int endOffset) {
    if (isWikisaurus(wiktionaryPageName)) {
      wiktionaryPageName = cutNamespace(wiktionaryPageName);
      wdh.initializeEntryExtraction(wiktionaryPageName);
      wikisaurusExtractor.extractWikisaurusSection(wiktionaryPageName,
          pageContent.substring(startOffset, endOffset));
      return;
    }
    wdh.initializeEntryExtraction(wiktionaryPageName);
    Matcher m = sectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    wikiExpander.setPageName(wiktionaryPageName);
    currentBlock = Block.NOBLOCK;

    HashMap<String, Object> previousContext = new HashMap<>();
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
        leaveCurrentBlock(m, previousContext);
        gotoNextBlock(nextBlock, context);
        previousContext = context;
      }
    }
    // Finalize the entry parsing
    leaveCurrentBlock(m, previousContext);
    wdh.finalizeEntryExtraction();
  }

  private Block computeNextBlock(Matcher m, Map<String, Object> context) {
    String title = m.group(1).trim();
    String nym;
    context.put("start", m.end());

    if (title.equals("Pronunciation")) {
      return Block.PRONBLOCK;
    } else if (WiktionaryDataHandler.isValidPOS(title)) {
      context.put("pos", title);
      return Block.DEFBLOCK;
    } else if (title.equals("Translations")) { // TODO: some sections are using Translation in the
      // singular form...
      return Block.TRADBLOCK;
    } else if (title.equals("Alternative spellings")) {
      return Block.ORTHOALTBLOCK;
    } else if (title.equals("Conjugation")) {
      return Block.CONJUGATIONBLOCK;
    } else if (title.startsWith("Etymology")) {
      return Block.ETYMOLOGYBLOCK;
    } else if (title.equals("Derived terms")) {
      return Block.DERIVEDBLOCK;
    } else if (title.equals("Descendants")) {
      return Block.DESCENDANTSBLOCK;
    } else if (null != (nym = EnglishGlobals.nymMarkerToNymName.get(title))) {
      context.put("nym", nym);
      return Block.NYMBLOCK;
    } else {
      log.debug("Ignoring content of section {} in {}", title, this.wiktionaryPageName);
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
      case CONJUGATIONBLOCK:
        break;
      case DERIVEDBLOCK:
        break;
      case DESCENDANTSBLOCK:
        break;
      case ETYMOLOGYBLOCK:
        break;
      default:
        assert false : "Unexpected block while parsing: " + wiktionaryPageName;
    }

  }

  private void leaveCurrentBlock(Matcher m, HashMap<String, Object> context) {
    if (blockStart == -1) {
      return;
    }

    int end = computeRegionEnd(blockStart, m);

    switch (currentBlock) {
      case NOBLOCK:
      case IGNOREPOS:
        break;
      case DEFBLOCK:
        String pos = (String) context.get("pos");
        wdh.addPartOfSpeech(pos);
        ewdh.registerEtymologyPos(wiktionaryPageName);
        extractMorphology(blockStart, end);
        extractHeadInformation(blockStart, end);
        extractDefinitions(blockStart, end);
        break;
      case TRADBLOCK:
        extractTranslations(blockStart, end);
        break;
      case ORTHOALTBLOCK:
        extractOrthoAlt(blockStart, end);
        break;
      case NYMBLOCK:
        extractNyms((String) context.get("nym"), blockStart, end);
        currentNym = null;
        break;
      case PRONBLOCK:
        extractPron(blockStart, end);
        break;
      case CONJUGATIONBLOCK:
        extractConjugation(blockStart, end);
        break;
      case ETYMOLOGYBLOCK:
        extractEtymology(blockStart, end);
        break;
      case DERIVEDBLOCK:
        extractDerived(blockStart, end);
        break;
      case DESCENDANTSBLOCK:
        extractDescendants(blockStart, end);
        break;
      default:
        assert false : "Unexpected block while parsing: " + wiktionaryPageName;
    }

    blockStart = -1;
  }

  private static final String[] heads = {"head", "head1", "head2"};
  private static ClassBasedSequenceFilter linkResolver = new ClassBasedSequenceFilter();

  private static ArrayList<WikiText.Token> getMyTemplateContent(WikiText.Token t) {
    if (t instanceof WikiText.Template) {
      WikiText.Template tt = (WikiText.Template) t;
      if (tt.getName().equals("vern") || tt.getName().equals("w") || tt.getName().equals("pedlink")
          || tt.getName().equals("what someone said")) {
        return tt.getArgs().get("1").tokens();
      } else if (tt.getName().equals("l")) {
        WikiText.WikiContent a3 = tt.getArgs().get("3");
        if (null == a3) {
          return tt.getArgs().get("2").tokens();
        } else {
          return a3.tokens();
        }
      } else {
        return new ArrayList<>();
      }
    } else {
      throw new RuntimeException("Cannot collect parameter contents on a non Template token");
    }
  }

  static {
    linkResolver.clearAction().keepContentOfInternalLink().sourceText()
        .keepContentOfTemplates(WiktionaryExtractor::getMyTemplateContent);
  }

  private void extractHeadInformation(int start, int end) {
    WikiText text = new WikiText(wiktionaryPageName, pageContent, start, end);
    for (WikiText.Token t : text.templatesOnUpperLevel()) {
      WikiText.Template tmpl = (WikiText.Template) t;
      if (tmpl.getName().equals("head") || tmpl.getName().startsWith("en-")) {
        Map<String, WikiText.WikiContent> args = tmpl.getArgs();
        if (tmpl.getName().equals("head")) {
          String pos = args.get("2").toString();
          if (pos != null && pos.endsWith(" form")) {
            continue;
          }
        }
        for (String h : heads) {
          WikiText.WikiContent head = args.get(h);
          if (null != head && head.toString().trim().length() != 0) {
            WikiCharSequence s = new WikiCharSequence(head, linkResolver);
            String headword = s.toString();
            if (isSignificantlyDifferent(headword, wiktionaryPageName)) {
              log.debug("Found {} template with head {} // '{}' in '{}'", tmpl.getName(), head,
                  headword, wiktionaryPageName);
              wdh.registerAlternateSpelling(headword);
            }
          }
        }
      }
    }
  }

  private boolean isSignificantlyDifferent(String headword, String pageName) {
    return !(headword.equals(pageName)
        || (headword.startsWith("the ") && pageName.equals(headword.substring(4)))
        || (headword.startsWith("The ") && pageName.equals(headword.substring(4)))
        || (headword.startsWith("(the) ") && pageName.equals(headword.substring(6)))
        || (headword.startsWith("(The) ") && pageName.equals(headword.substring(6)))
        || (headword.startsWith("to ") && pageName.equals(headword.substring(3)))
        || (headword.startsWith("To ") && pageName.equals(headword.substring(3)))
        || (headword.endsWith("!") && pageName.equals(headword.substring(0, headword.length() - 1)))
        || (headword.endsWith(".") && pageName.equals(headword.substring(0, headword.length() - 1)))
        || (headword.endsWith("?")
            && pageName.equals(headword.substring(0, headword.length() - 1))));
  }


  // TODO: check correct parsing of From ''[[semel#Latin|semel]]'' + ''[[pro#Latin|pro]]'' +
  // ''[[semper#Latin|semper]]''
  protected void extractEtymology(int blockStart, int end) {
    if (wdh.isDisabled(Feature.ETYMOLOGY)) {
      return;
    }
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      log.trace("Ignoring etymology for: {}", wiktionaryPageName);
      return;
    }

    log.trace("ETYM {}: {}", ewdh.getCurrentEntryLanguage(),
        pageContent.substring(blockStart, end));
    Etymology etymology =
        new Etymology(pageContent.substring(blockStart, end), ewdh.getCurrentEntryLanguage());

    etymology.fromDefinitionToSymbols();
    try {
      ewdh.registerEtymology(etymology);
    } catch (RuntimeException e) {
      log.error("Caught {} while registering etymology in {}", e, wiktionaryPageName);
      if (log.isDebugEnabled()) {
        e.printStackTrace(System.err);
      }
    }
  }

  // TODO: process * {{l|pt|mundinho}}, {{l|pt|mundozinho}} {{gloss|diminutives}}
  // * {{l|pt|mundão}} {{gloss|augmentative}}
  // DONE: process {{der4|title=Terms derived from ''free'' | [[freeball]], [[free-ball]] |
  // [[freebooter]] }}
  protected void extractDerived(int blockStart, int end) {
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      return;
    }

    String lang = ewdh.getCurrentEntryLanguage();
    lang = EnglishLangToCode.threeLettersCode(lang);
    extractBulletList(pageContent.substring(blockStart, end), lang);

    extractTable(pageContent.substring(blockStart, end), lang);
  }

  protected void extractDescendants(int blockStart, int end) {
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      return;
    }

    String lang = ewdh.getCurrentEntryLanguage();
    lang = EnglishLangToCode.threeLettersCode(lang);
    boolean isMatch = extractMultipleBulletList(pageContent.substring(blockStart, end), lang, true);

    // if there is no match to multiple bullet list
    if (!isMatch) {
      extractEtymtree(pageContent.substring(blockStart, end), lang);
    }
  }

  private void extractTable(String s, String lang) {
    for (Pair l : WikiTool.locateEnclosedString(s, "{{", "}}")) {
      String t = s.substring(l.start + 2, l.start + 6);
      int start = l.start;
      if (t.equals("der2") || t.equals("der3") || t.equals("der4")) {
        start += 7;
      } else if (t.equals("der-zh")) {
        start += 9;
      } else {
        return;
      }
      Map<String, String> args = WikiTool.parseArgs(s.substring(start, l.end - 2));

      for (String key : args.keySet()) {
        if (key.matches("\\d+$")) {// if key is an integer
          Etymology etymology = new Etymology(args.get(key), lang);

          etymology.fromTableToSymbols();

          if (etymology.symbols.size() == 0) {
            if (WikiTool.locateEnclosedString(etymology.string, "{{", "}}").size()
                + WikiTool.locateEnclosedString(etymology.string, "[[", "]]").size() == 0) {
              for (String lemma : split(etymology.string)) {
                etymology.symbols.add(new Symbols("_m|" + lang + "|" + lemma, lang, "TEMPLATE"));
              }
            }
          }
          ewdh.registerDerived(etymology);
        }
      }
    }
  }

  static private ArrayList<String> split(String s) {
    ArrayList<String> toreturn = new ArrayList<String>();

    String[] tmp = s.split(",");
    for (String t : tmp) {
      String[] tmp2 = t.split("/");
      for (String t2 : tmp2) {
        if (t2 != null && !t2.equals("")) {
          toreturn.add(t2.trim());
        }
      }
    }
    return toreturn;
  }

  private boolean extractMultipleBulletList(String s, String lang, boolean setRoot) {
    int offset = 0;
    ewdh.initializeAncestors();
    if (setRoot) {
      offset = 1;
      ewdh.registerCurrentEtymologyEntry(lang);
      ewdh.ancestors.add(ewdh.currentEtymologyEntry);
    }

    Matcher multipleBulletListMatcher = WikiPatterns.multipleBulletListPattern.matcher(s);
    int nStars = 0;
    while (multipleBulletListMatcher.find()) {
      nStars = multipleBulletListMatcher.group(1).length();
      if (nStars + offset - 1 < ewdh.ancestors.size()) {
        ewdh.ancestors.subList(nStars + offset - 1, ewdh.ancestors.size()).clear();
      }

      Etymology etymology = new Etymology(multipleBulletListMatcher.group(2), lang);

      etymology.fromBulletToSymbols();

      ewdh.addAncestorsAndRegisterDescendants(etymology);
    }

    ewdh.finalizeAncestors();

    return nStars > 0;
  }

  private void extractBulletList(String s, String lang) {
    ewdh.registerCurrentEtymologyEntry(lang);

    Matcher bulletListMatcher = WikiPatterns.bulletListPattern.matcher(s);
    while (bulletListMatcher.find()) {
      Etymology etymology = new Etymology(bulletListMatcher.group(1), lang);

      etymology.fromBulletToSymbols();

      // check that all lemmas share the same language
      for (Symbols b : etymology.symbols) {
        if (!b.args.get("lang").equals(lang)) {
          log.debug("Ignoring derived words {}", bulletListMatcher.group());
          return;
        }
      }

      ewdh.registerDerived(etymology);
    }
  }

  private void extractEtymtree(String s, String lang) {
    for (Pair template : WikiTool.locateEnclosedString(s, "{{", "}}")) {
      Symbols b = new Symbols(s.substring(template.start + 2, template.end - 2), lang, "TEMPLATE");
      if (b.values != null && b.values.get(0).equals("ETYMTREE") && b.args.get("lang") != null) {
        String page = b.args.get("page");
        if (b.args.get("word1") == null) {
          page = page + this.wiktionaryPageName;
        }
        if (ewdh.etymtreeHashSet.add(page)) {// if etymtree hasn't been saved already
          String etymtreePageContent = wi.getTextOfPage(page);
          if (etymtreePageContent != null) {
            log.debug("Extracting etymtree page {}", page);
            extractMultipleBulletList(etymtreePageContent, lang, false);
          } else {
            log.debug("Warning: cannot extract etymtree page {}", page);
          }
        }
        return;
      }
    }
  }

  private void extractConjugation(int startOffset, int endOffset) {
    log.debug("Conjugation extraction not yet implemented in:\t{}", wiktionaryPageName);
  }

  private EnglishInflectionData plural = new EnglishInflectionData().plural();
  private EnglishInflectionData singular = new EnglishInflectionData().singular();
  private EnglishInflectionData comparative = new EnglishInflectionData().comparative();
  private EnglishInflectionData superlative = new EnglishInflectionData().superlative();
  private EnglishInflectionData pres3Sg =
      new EnglishInflectionData().presentTense().thirdPerson().singular();
  private EnglishInflectionData presPtc = new EnglishInflectionData().presentTense().participle();
  private EnglishInflectionData past = new EnglishInflectionData().pastTense();
  private EnglishInflectionData pastPtc = new EnglishInflectionData().pastTense().participle();

  private void extractMorphology(int startOffset, int endOffset) {
    // TODO: For some entries, there are several morphology information covering different word
    // senses
    // TODO: Handle such cases (by creating another lexical entry ?) // Similar to reflexiveness in
    // French wiktionary
    if (ewdh.isDisabled(IWiktionaryDataHandler.Feature.MORPHOLOGY)) {
      return;
    }

    WikiText text = new WikiText(wiktionaryPageName, pageContent, startOffset, endOffset);

    WikiEventsSequence wikiTemplates = text.templatesOnUpperLevel();

    // Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    // macroMatcher.region(startOffset, endOffset);

    // while (macroMatcher.find()) {
    // TODO: current code goes through all templates of the defintion block while it should only
    // process morphology templates.

    int nbTempl = 0;
    for (WikiText.Token wikiTemplate : wikiTemplates) {
      nbTempl++;
      WikiText.Template tmpl = (WikiText.Template) wikiTemplate;
      // String g1 = macroMatcher.group(1);
      String g1 = tmpl.getName();
      if (g1.equals("en-noun")) {
        // log.debug("MORPHOLOGY EXTRACTION FROM : {}\tin\t{}", tmpl.toString(),
        // wiktionaryPageName);

        Map<String, String> args = tmpl.getParsedArgs();
        extractNounMorphology(args, false);
      } else if (g1.equals("en-proper noun") || g1.equals("en-proper-noun")
          || g1.equals("en-prop")) {
        // log.debug("MORPHOLOGY EXTRACTION FROM : {}\tin\t{}", tmpl.toString(),
        // wiktionaryPageName);

        Map<String, String> args = tmpl.getParsedArgs();
        extractNounMorphology(args, true);
      } else if (g1.equals("en-plural noun")) {
        ewdh.addInflectionOnCanonicalForm(new EnglishInflectionData().plural());
        Map<String, String> args = tmpl.getParsedArgs();
        if (args.containsKey("sg")) {
          String singularForm = args.get("sg");
          addForm(singular.toPropertyObjectMap(), singularForm);
          args.remove("sg");
        }
        if (!args.isEmpty()) {
          log.debug("en-plural noun macro: Non handled parameters : \t{}\tin\t{}", args,
              this.wiktionaryPageName);
        }

      } else if (g1.equals("en-adj") || g1.equals("en-adv") || g1.equals("en-adjective")
          || g1.equals("en-adj-more") || g1.equals("en-adverb") || g1.equals("en-adv-more")) {
        // log.debug("MORPHOLOGY EXTRACTION FROM : {}\tin\t{}", tmpl.toString(),
        // wiktionaryPageName);

        // TODO: mark canonicalForm as ????
        Map<String, String> args = tmpl.getParsedArgs();
        extractAdjectiveMorphology(args);


      } else if (g1.equals("en-verb")) {
        // TODO: extract verb morphology
        Map<String, String> args = tmpl.getParsedArgs();

        extractVerbMorphology(args);

      } else if (g1.equals("en-abbr")) {
        // TODO: extract abbreviation morphology
        log.debug("Use of deprecated en-abbr template in\t{}", wiktionaryPageName);
      } else if (g1.equals("en-acronym")) {
        // TODO: extract acronym morphology
        log.debug("Use of deprecated en-acronym template in\t{}", wiktionaryPageName);
      } else if (g1.equals("en-con")) {
        // nothing to extract...
      } else if (g1.equals("en-cont")) {
        // contraction
        log.debug("Use of deprecated en-cont (contraction) template in\t{}", wiktionaryPageName);
      } else if (g1.equals("en-det")) {
        // nothing to extract
      } else if (g1.equals("en-initialism")) {
        // TODO: extract initialism morphology
        log.debug("Use of deprecated en-initialism template in\t{}", wiktionaryPageName);
      } else if (g1.equals("en-interj")) {
        // nothing to extract
      } else if (g1.equals("en-part") || g1.equals("en-particle")) {
        // nothing to extract
      } else if (g1.equals("en-phrase")) {
        // nothing to extract
      } else if (g1.equals("en-prefix")) {
        // nothing to extract ??
        Map<String, String> args = tmpl.getParsedArgs();
        args.remove("sort");
        if (!args.isEmpty()) {
          log.debug("other args in en-suffix template\t{}\tin\t{}", args, wiktionaryPageName);
        }
      } else if (g1.equals("en-prep")) {
        // nothing to extract
      } else if (g1.equals("en-prep phrase")) {
        // TODO: the first argument is the head if different from pagename...
      } else if (g1.equals("en-pron")) {
        // TODO: extract part morphology
        Map<String, String> args = tmpl.getParsedArgs();
        if (null != args.get("desc")) {
          log.debug("desc argument in en-pron template in\t{}", wiktionaryPageName);
          args.remove("desc");
        }
        if (!args.isEmpty()) {
          log.debug("other args in en-pron template\t{}\tin\t{}", args, wiktionaryPageName);
        }
      } else if (g1.equals("en-proverb")) {
        // TODO: the first argument (or head argument) is the head if different from pagename...
      } else if (g1.equals("en-punctuation mark")) {
        // TODO: the first argument (or head argument) is the head if different from pagename...
      } else if (g1.equals("en-suffix")) {
        // TODO: cat2 and cat3 sontains some additional categories...
        Map<String, String> args = tmpl.getParsedArgs();
        args.remove("sort");
        if (!args.isEmpty()) {
          log.debug("other args in en-suffix template\t{}\tin\t{}", args, wiktionaryPageName);
        }
      } else if (g1.equals("en-symbol")) {
        // TODO: the first argument is the head if different from pagename...
      } else if (g1.equals("en-number")) {
        // TODO:
      } else if (g1.equals("head")) {
        Map<String, String> args = tmpl.getParsedArgs();
        String pos = args.get("2");
        if (null != pos && pos.endsWith("form")) {
          // This is a inflected form
          // TODO: Check if the inflected form is available in the base word morphology.
        } else {
          log.debug("MORPH: direct call to head\t{}\tin\t{}", tmpl.toString(),
              this.wiktionaryPageName);
        }
      } else {
        // log.debug("NOMORPH PATTERN:\t {}\t in:\t{}", g1, wiktionaryPageName);
        nbTempl--;
      }
    }
    if (nbTempl > 1) {
      log.debug("MORPHTEMPLATE: more than 1 morph template in\t{}", this.wiktionaryPageName);
    }
  }

  private void extractNounMorphology(Map<String, String> args, boolean properNoun) {
    // DONE: mark canonicalForm as singular
    ewdh.addInflectionOnCanonicalForm(new EnglishInflectionData().singular());

    // TODO: head is used to point to constituants of MWE, extract them and make parts explicit.
    String head = args.get("head");
    String headword;
    if (head != null && !head.trim().equals("")) {
      // TODO: evaluate the runtime impact of systematic expansion (Should I expand only if a
      // template is present in the argument value ?)
      headword = wikiExpander.expandAll(head, null); // Expand everything in the head value and
      // provide a text only version.
    } else {
      headword = wiktionaryPageName;
    }
    args.remove("head");
    int argnum = 1;
    String arg = args.get(Integer.toString(argnum));
    args.remove(Integer.toString(argnum));
    boolean uncountable = false;
    if (arg == null && !properNoun) {
      // There are no positional arg, meaning that the plural uses a s suffix
      arg = "s";
    }
    if (args.containsKey("plqual")) {
      args.put("pl1qual", args.get("plqual"));
      args.remove("plqual");
    }
    while (arg != null) {
      String note = args.get("pl" + argnum + "qual");
      if (arg.equals("s")) {
        if (!uncountable) {
          ewdh.countable();
        }
        addForm(plural.toPropertyObjectMap(), headword + "s", note);
      } else if (arg.equals("es")) {
        if (!uncountable) {
          ewdh.countable();
        }
        addForm(plural.toPropertyObjectMap(), headword + "es", note);
      } else if (arg.equals("-")) {
        // uncountable or usually uncountable
        uncountable = true;
        ewdh.uncountable();
      } else if (arg.equals("~")) {
        // countable and uncountable
        ewdh.countable();
        ewdh.uncountable();
      } else if (arg.equals("!")) {
        // plural not attested (exit loop)
        break;
      } else if (arg.equals("?")) {
        // unknown or uncertain plural (exit loop)
        break;
      } else {
        // pluralForm
        // TODO: if plural form = singular form, note entry as invariant
        if (!uncountable) {
          ewdh.countable();
        }
        addForm(plural.toPropertyObjectMap(), arg, note);
      }

      arg = args.get(Integer.toString(++argnum));
      args.remove(Integer.toString(argnum));
    }

    if (!args.isEmpty()) {
      log.debug("en-noun macro: Non handled parameters : \t{}\tin\t{}", args,
          this.wiktionaryPageName);
    }
  }

  private void extractAdjectiveMorphology(Map<String, String> args) {
    // TODO: head is used to point to constituants of MWE, extract them and make parts explicit.
    String h = args.get("head");
    String headword;
    if (h != null && !h.trim().equals("")) {
      // TODO: evaluate the runtime impact of systematic expansion (Should I expand only if a
      // template is present in the argument value ?)
      headword = wikiExpander.expandAll(h, null); // Expand everything in the head value and provide
      // a text only version.
    } else {
      headword = wiktionaryPageName;
    }
    args.remove("head");
    if (args.containsKey("sup")) {
      args.put("sup1", args.get("sup"));
      args.remove("sup");
    }
    int argnum = 1;
    String arg = args.get(Integer.toString(argnum));
    args.remove(Integer.toString(argnum));
    boolean notComparable = false;
    boolean comparativeOnly = false;
    if (arg == null) {
      // There are no positional arg, meaning that the adjective uses more and most as comparative
      // markers
      arg = "more";
    }
    while (arg != null) {
      if (arg.equals("more") && !"many".equals(wiktionaryPageName)
          && !"much".equals(wiktionaryPageName)) {
        if (!notComparable) {
          ewdh.comparable();
        }
        addForm(comparative.toPropertyObjectMap(), "more " + headword);
        if (null != args.get("sup" + argnum)) {
          log.debug("Irregular superlative with comparative with more in\t{}", wiktionaryPageName);
        }
        addForm(superlative.toPropertyObjectMap(), "most " + headword);
      } else if (arg.equals("further") && !"far".equals(wiktionaryPageName)) {
        if (!notComparable) {
          ewdh.comparable();
        }
        addForm(comparative.toPropertyObjectMap(), "further " + headword);
        if (null != args.get("sup" + argnum)) {
          log.debug("Irregular superlative with comparative with further in\t{}",
              wiktionaryPageName);
        }
        addForm(superlative.toPropertyObjectMap(), "furthest " + headword);
      } else if (arg.equals("er")) {
        if (!notComparable) {
          ewdh.comparable();
        }
        String stem = headword.replaceAll("([^aeiou])e?y$", "$1").replaceAll("e$", "");
        String comp = stem + "er";
        addForm(comparative.toPropertyObjectMap(), comp);
        String sup = getSuperlative(args, argnum, comp);
        if (null != sup) {
          addForm(superlative.toPropertyObjectMap(), sup);
        }
      } else if (arg.equals("-")) {
        // not comparable or not usually comparable
        notComparable = true;
        ewdh.notComparable();
        String sup = getSuperlative(args, argnum, "-");
        if (null != sup) {
          addForm(superlative.toPropertyObjectMap(), sup);
        }
      } else if (arg.equals("+")) {
        // comparative only
        ewdh.addInflectionOnCanonicalForm(comparative);
        break;
      } else if (arg.equals("?")) {
        // unknown or uncertain plural (exit loop)
        break;
      } else {
        // comparativeForm given
        if (!notComparable) {
          ewdh.comparable();
        }
        addForm(comparative.toPropertyObjectMap(), arg);
        String sup = getSuperlative(args, argnum, arg);
        if (null != sup) {
          addForm(superlative.toPropertyObjectMap(), sup);
        }
      }
      arg = args.get(Integer.toString(++argnum));
      args.remove(Integer.toString(argnum));
      args.remove("sup" + argnum);
    }

    if (!args.isEmpty()) {
      log.debug("en-adj macro: Non handled parameters : \t{}\tin\t{}", args,
          this.wiktionaryPageName);
    }
  }

  private void extractVerbMorphology(Map<String, String> args) {
    // Code based on wiktionary en-headword Lua Module code (date: 01/02/2016)

    String par1 = args.get("1");
    String par2 = args.get("2");
    String par3 = args.get("3");
    String par4 = args.get("4");

    String pres3sgForm = (null != par1) ? par1 : this.wiktionaryPageName + "s";
    String presPtcForm = (null != par2) ? par2 : this.wiktionaryPageName + "ing";
    String pastForm = (null != par3) ? par3 : this.wiktionaryPageName + "ed";
    String pastPtcForm = null;

    if (null != par1 && null == par2 && null == par3) {
      // This is the "new" format, which uses only the first parameter.
      if ("es".equals(par1)) {
        pres3sgForm = this.wiktionaryPageName + "es";
      } else if ("ies".equals(par1)) {
        if (!this.wiktionaryPageName.endsWith("y")) {
          log.debug("VERBMORPH : Incorrect en-verb parameter \"ies\" on non y ending verb\t{}",
              this.wiktionaryPageName);
        }
        String stem = this.wiktionaryPageName.substring(0, this.wiktionaryPageName.length() - 1);
        pres3sgForm = stem + "ies";
        presPtcForm = stem + "ying";
        pastForm = stem + "ied";
      } else if ("d".equals(par1)) {
        pres3sgForm = this.wiktionaryPageName + "s";
        presPtcForm = this.wiktionaryPageName + "ing";
        pastForm = this.wiktionaryPageName + "d";
      } else {
        pres3sgForm = this.wiktionaryPageName + "s";
        presPtcForm = par1 + "ing";
        pastForm = par1 + "ed";
      }
    } else {
      // This is the "legacy" format, using the second and third parameters as well.
      // It is included here for backwards compatibility and to ease the transition.
      if (null != par3) {
        if ("es".equals(par3)) {
          pres3sgForm = par1 + par2 + "es";
          presPtcForm = par1 + par2 + "ing";
          pastForm = par1 + par2 + "ed";
        } else if ("ing".equals(par3)) {
          pres3sgForm = this.wiktionaryPageName + "s";
          presPtcForm = par1 + par2 + "ing";

          if ("y".equals(par2)) {
            pastForm = this.wiktionaryPageName + "d";
          } else {
            pastForm = par1 + par2 + "ed";
          }
        } else if ("ed".equals(par3)) {
          if ("i".equals(par2)) {
            pres3sgForm = par1 + par2 + "es";
            presPtcForm = this.wiktionaryPageName + "ing";
          } else {
            pres3sgForm = this.wiktionaryPageName + "s";
            presPtcForm = par1 + par2 + "ing";
          }
          pastForm = par1 + par2 + "ed";
        } else if ("d".equals(par3)) {
          pres3sgForm = this.wiktionaryPageName + "s";
          presPtcForm = par1 + par2 + "ing";
          pastForm = par1 + par2 + "d";
        } else {
          // log.debug("VERBMORPH : Incorrect en-verb 3rd parameter on
          // verb\t{}",this.wiktionaryPageName);
        }
      } else {
        if (null != par2) {
          if ("es".equals(par2)) {
            pres3sgForm = par1 + "es";
            presPtcForm = par1 + "ing";
            pastForm = par1 + "ed";
          } else if ("ies".equals(par2)) {
            if (!(par1 + "y").equals(this.wiktionaryPageName)) {
              log.debug(
                  "VERBMORPH : Incorrect en-verb 2rd parameter ies  with stem different to pagename on verb\t{}",
                  this.wiktionaryPageName);
            }
            pres3sgForm = par1 + "ies";
            presPtcForm = par1 + "ying";
            pastForm = par1 + "ied";
          } else if ("ing".equals(par2)) {
            pres3sgForm = this.wiktionaryPageName + "s";
            presPtcForm = par1 + "ing";
            pastForm = par1 + "ed";
          } else if ("ed".equals(par2)) {
            pres3sgForm = this.wiktionaryPageName + "s";
            presPtcForm = par1 + "ing";
            pastForm = par1 + "ed";
          } else if ("d".equals(par2)) {
            if (!this.wiktionaryPageName.equals(par1)) {
              log.debug(
                  "VERBMORPH : Incorrect en-verb 2rd parameter d with par1 different to pagename on verb\t{}",
                  this.wiktionaryPageName);
            }
            pres3sgForm = this.wiktionaryPageName + "s";
            presPtcForm = par1 + "ing";
            pastForm = par1 + "d";
          } else {
            log.debug("VERBMORPH : unexpected 2rd parameter \"{}\" on verb\t{}", par2,
                this.wiktionaryPageName);
          }
        }
      }
    }

    if (null != par4) {
      pastPtcForm = par4;
    }
    String pres3SgQual = args.get("pres_3sg_qual");
    String presPtcQual = args.get("pres_ptc_qual");
    String pastQual = args.get("past_qual");
    String pastPtcQual = args.get("past_ptc_qual");

    Map<String, String> pastForms = new HashMap<>();
    Map<String, String> pastPtcForms = new HashMap<>();

    addForm(pres3Sg.toPropertyObjectMap(), pres3sgForm, pres3SgQual);
    addForm(presPtc.toPropertyObjectMap(), presPtcForm, presPtcQual);
    addForm(past.toPropertyObjectMap(), pastForm, pastQual);
    if (null != pastForm) {
      pastForms.put("1", pastForm);
    }
    addForm(pastPtc.toPropertyObjectMap(), pastPtcForm, pastPtcQual);
    if (null != pastPtcForm) {
      pastPtcForms.put("1", pastPtcForm);
    }

    args.remove("1");
    args.remove("2");
    args.remove("3");
    args.remove("4");
    args.remove("pres_3sg_qual");
    args.remove("pres_ptc_qual");
    args.remove("past_qual");
    args.remove("past_ptc_qual");

    Matcher p3sgM = Pattern.compile("^pres_3sg(\\d+)$").matcher("");
    Matcher presPtcM = Pattern.compile("^pres_ptc(\\d+)$").matcher("");
    Matcher pastM = Pattern.compile("^past(\\d+)$").matcher("");
    Matcher pastPtcM = Pattern.compile("^past_ptc(\\d+)$").matcher("");

    for (String k : args.keySet()) {
      if (p3sgM.reset(k).matches()) {
        String i = p3sgM.group(1);
        String qual = args.get("pres_3sg" + i + "_qual");
        addForm(pres3Sg.toPropertyObjectMap(), args.get(k), qual);
      } else if (presPtcM.reset(k).matches()) {
        String i = presPtcM.group(1);
        String qual = args.get("pres_ptc" + i + "_qual");
        addForm(presPtc.toPropertyObjectMap(), args.get(k), qual);
      } else if (pastM.reset(k).matches()) {
        String i = pastM.group(1);
        String qual = args.get("past" + i + "_qual");
        pastForms.put(i, args.get(k));
        addForm(past.toPropertyObjectMap(), args.get(k), qual);
      } else if (pastPtcM.reset(k).matches()) {
        String i = pastPtcM.group(1);
        String qual = args.get("past_ptc" + i + "_qual");
        pastPtcForms.put(i, args.get(k));
        addForm(pastPtc.toPropertyObjectMap(), args.get(k), qual);
      } else {
        // ignore this argument
      }
    }

    if (pastPtcForms.size() == 0) {
      // duplicate all past forms as pastPtc
      for (String v : pastForms.values()) {
        addForm(pastPtc.toPropertyObjectMap(), v);
      }
    }
  }

  private String getSuperlative(Map<String, String> args, int argnum, String comp) {
    String k = "sup" + argnum;
    String sup = null;
    if (args.containsKey(k)) {
      sup = args.get(k);
    } else {
      if (comp.endsWith("er")) {
        sup = comp.replaceAll("er$", "est");
      } else if (!"-".equals(sup)) {
        log.debug("Missing superlative for irregular comparative in\t{}", wiktionaryPageName);
      }
    }
    if ("-".equals(sup)) {
      return null;
    } else {
      return sup;
    }
  }

  private void addForm(HashSet<PropertyObjectPair> infl, String s) {
    addForm(infl, s, null);
  }

  private void addForm(HashSet<PropertyObjectPair> infl, String s, String note) {
    if (null == s || s.length() == 0 || s.equals("—") || s.equals("-")) {
      return;
    }

    ewdh.registerInflection(s, note, infl);
  }

  private void extractTranslations(String wikiSource) {
    WikiText txt = new WikiText(wikiSource);
    Resource currentGloss = null;
    int rank = 1;
    // TODO: there are templates called "qualifier" used to further qualify the translation check
    // and evaluate if extracting its data is useful.
    for (Token token : txt.templates()) {
      Template t = (Template) token;
      String tName = t.getName();
      if (tName.equals("t+") || tName.equals("t-") || tName.equals("tø") || tName.equals("t")) {
        WikiContent l = t.getArgs().get("1");
        WikiContent word = t.getArgs().get("2");
        WikiContent usageContent = t.getArgs().get("3");
        String usage = (usageContent == null) ? "" : usageContent.toString();
        if (word == null) {
          log.debug("No (required) translation in {} : {}", t, wiktionaryPageName);
          continue;
        }
        Map<String, WikiContent> args = new HashMap<>(t.getArgs()); // clone the args map so that
                                                                    // we can destroy it
        args.remove("1");
        args.remove("2");
        args.remove("3");
        String remainingArgs = "";
        if (args.size() > 0) {
          for (Entry<String, WikiContent> s : args.entrySet()) {
            remainingArgs = remainingArgs + "|" + s.getKey() + "=" + s.getValue();
          }
          if (usage.length() > 0) {
            usage = usage + remainingArgs;
          } else {
            usage = remainingArgs.substring(1);
          }
        }
        String lang = null;
        if (l != null) {
          lang = LangTools.normalize(l.toString());
        } else {
          log.debug("null language (first positional arg) in {} > {}", t.toString(),
              wiktionaryPageName);
        }
        lang = EnglishLangToCode.threeLettersCode(lang);
        if (lang != null) {
          // TODO: handle translations that are the result of template expansions (e.g. "anecdotal
          // evidence").
          wdh.registerTranslation(lang, currentGloss, usage, word.toString());
        }
      } else if (tName.equals("trans-top") || tName.equals("trans-top-also")) {
        // Get the gloss that should help disambiguate the source acception
        String g2 = t.getParsedArgs().get("1");
        // Ignore gloss if it is a macro
        if (g2 != null && !g2.startsWith("{{")) {
          currentGloss = wdh.createGlossResource(glossFilter.extractGlossStructure(g2), rank++);
        } else {
          currentGloss = null;
        }
      } else if (tName.equals("checktrans-top")) {
        // forget glose.
        currentGloss = null;
      } else if (tName.equals("trans-mid")) {
        // just ignore it
      } else if (tName.equals("trans-bottom")) {
        // Forget the current glose
        currentGloss = null;
      } else if (tName.equals("section link")) {
        WikiContent g2 = t.getArgs().get("1");
        log.debug("Section link: {} for entry {}", g2, wiktionaryPageName);
        if (g2 != null) {
          String translationContent = getTranslationContentForLink(g2.toString());
          if (null != translationContent)
            extractTranslations(translationContent);
        }
      } else if (log.isDebugEnabled()) {
        log.debug("Ignored template: {} in translation section for entry {}", t.toString(),
            wiktionaryPageName);
      }
    }
  }

  private void extractTranslationsOld(String wikiSource) {
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(wikiSource);
    Resource currentGloss = null;
    int rank = 1;
    // TODO: there are templates called "qualifier" used to further qualify the translation check
    // and evaluate if extracting its data is useful.
    while (macroMatcher.find()) {
      String g1 = macroMatcher.group(1);

      if (g1.equals("t+") || g1.equals("t-") || g1.equals("tø") || g1.equals("t")) {
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
          lang = EnglishLangToCode.threeLettersCode(lang);
          if (lang != null) {
            wdh.registerTranslation(lang, currentGloss, usage, word);
          }

        }
      } else if (g1.equals("trans-top") || g1.equals("trans-top-also")) {
        // Get the gloss that should help disambiguate the source acception
        String g2 = macroMatcher.group(2);
        // Ignore gloss if it is a macro
        if (g2 != null && !g2.startsWith("{{")) {
          currentGloss = wdh.createGlossResource(glossFilter.extractGlossStructure(g2), rank++);
        } else {
          currentGloss = null;
        }
      } else if (g1.equals("checktrans-top")) {
        // forget glose.
        currentGloss = null;
      } else if (g1.equals("trans-mid")) {
        // just ignore it
      } else if (g1.equals("trans-bottom")) {
        // Forget the current glose
        currentGloss = null;
      } else if (g1.equals("section link")) {
        String g2 = macroMatcher.group(2);
        log.debug("Section link: {} for entry {}", g2, wiktionaryPageName);
        String translationContent = getTranslationContentForLink(g2);
        if (null != translationContent)
          extractTranslations(translationContent);
      }
    }
  }

  /**
   * Returns the content of the specified translation page's section. returns null if the page does
   * not exists or has already been extracted.
   * 
   * @param link
   * @return
   */
  private String getTranslationContentForLink(String link) {
    if (!processedLinks.add(link))
      return null;
    String[] linkAndSection = link.split("#");
    String translationPage = linkAndSection[0];
    String translationSection = linkAndSection[1];

    String translationPageContent = wi.getTextOfPageWithRedirects(translationPage);

    // TODO : extract the correct section from the full page.
    // Assume there is only on language and the anchor corresponds to level 3 Header (POS)
    WikiText text = new WikiText(translationPageContent);
    for (WikiSection s : text.sections(3)) {
      // return the first matching section
      if (s.getHeader().getContent().toString().equals(translationSection))
        return s.getContent().toString();
    }
    log.debug("Could not find appropriate section {} in translation section link target for {}",
        translationSection, wiktionaryPageName);
    return translationPageContent;
  }

  private Set<String> processedLinks = new HashSet<>();

  private void extractTranslations(int startOffset, int endOffset) {
    processedLinks.clear();
    processedLinks.add(wiktionaryPageName);
    extractTranslations(pageContent.substring(startOffset, endOffset));
  }

  @Override
  protected void extractNyms(String synRelation, int startOffset, int endOffset) {
    WikiText text = new WikiText(wiktionaryPageName, pageContent, startOffset, endOffset);
    ClassBasedFilter filter = new ClassBasedFilter();
    filter.allowListItem();

    WikiEventsSequence wikiEvents = text.filteredTokens(filter);

    for (WikiText.Token tok : wikiEvents) {
      if (tok instanceof WikiText.ListItem) {
        // It's a link, only keep the alternate string if present.
        WikiText.ListItem li = (WikiText.ListItem) tok;
        extractNyms(synRelation, li.getContent());
      }
    }
  }

  private void extractNyms(String synRelation, WikiText.WikiContent content) {
    ClassBasedFilter filter = new ClassBasedFilter();
    filter.allowInternalLink().allowTemplates();

    WikiEventsSequence wikiEvents = content.filteredTokens(filter);

    Resource currentGloss = null;
    int rank = 1;

    // TODO: extract glosses as present in wiktionary pages
    for (WikiText.Token tok : wikiEvents) {
      if (tok instanceof WikiText.InternalLink) {
        // It's a link, only keep the alternate string if present.
        WikiText.InternalLink link = (WikiText.InternalLink) tok;
        String linkText = link.getLinkText();
        if (linkText != null && !linkText.equals("") && !linkText.startsWith("Catégorie:")
            && !linkText.startsWith("#")) {
          if (isWikisaurus(linkText)) {
            // NOP: Wikisaurus pages are extracted independently
            // TODO : should we note that the current lexical entry points
            // to this particular wikisaurus page
          } else {
            wdh.registerNymRelation(linkText, synRelation, currentGloss);
          }
        }
      } else if (tok instanceof WikiText.Template) {
        WikiText.Template tmpl = (WikiText.Template) tok;
        if ("l".equals(tmpl.getName()) || "link".equals(tmpl.getName())) {
          Map<String, String> args = tmpl.getParsedArgs();
          if ("en".equals(args.get("1"))) {
            String target = args.get("2");
            args.remove("2");
            args.remove("1");
            if (!args.isEmpty()) {
              log.debug("Unhandled remaining args {} in {}", args.entrySet().toString(),
                  this.wiktionaryPageName);
            }
            wdh.registerNymRelation(target, synRelation, currentGloss);
          }
        } else if ("sense".equals(tmpl.getName())) {
          String g = tmpl.getParsedArgs().get("1");
          for (int i = 2; i < 9; i++) {
            String p = tmpl.getParsedArgs().get(Integer.toString(i));
            if (null != p) {
              g += ", " + p;
            }
          }
          currentGloss = wdh.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
        }
      }
    }
  }

  @Override
  public void extractExample(String example) {
    // TODO: current example extractor cannot handle English data where different lines are used to
    // define the example.

  }

  protected void extractPron(int startOffset, int endOffset) {
    Matcher pronMatcher = pronPattern.matcher(pageContent);
    pronMatcher.region(startOffset, endOffset);
    while (pronMatcher.find()) {
      String pron = pronMatcher.group(1);

      if (null == pron || pron.equals("")) {
        return;
      }

      if (!pron.equals("")) {
        wdh.registerPronunciation(pron, "en-fonipa");
      }
    }
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    // TODO: properly handle macros in definitions.
    definitionExpander.setPageName(this.wiktionaryPageName);
    definitionExpander.parseDefinition(definition, defLevel);
  }
}
