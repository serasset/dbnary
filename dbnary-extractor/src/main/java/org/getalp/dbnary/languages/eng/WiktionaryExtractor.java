/**
 *
 */
package org.getalp.dbnary.languages.eng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.Span;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.ClassBasedSequenceFilter;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.dbnary.wiki.WikiTool;
import org.getalp.iso639.ISO639_3;
import org.getalp.iso639.ISO639_3.Lang;
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

  private WiktionaryDataHandler ewdh; // English specific version of the data handler.

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    if (wdh instanceof WiktionaryDataHandler) {
      ewdh = (WiktionaryDataHandler) wdh;
    } else {
      log.error("English Wiktionary Extractor instanciated with a non english data handler!");
    }
  }

  private ExpandAllWikiModel wikiExpander;
  protected EnglishDefinitionExtractorWikiModel definitionExpander;
  protected EnglishExampleExpanderWikiModel exampleExpander;
  protected EnglishMorphologyExtractorWikiModel morphologyExtractor;
  protected EnglishPronunciationExtractorWikiModel pronunciationExpander;
  private WikisaurusExtractor wikisaurusExtractor;


  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    wikiExpander =
        new ExpandAllWikiModel(wi, Locale.ENGLISH, "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    definitionExpander = new EnglishDefinitionExtractorWikiModel(this.wdh, this.wi,
        new Locale("en"), "/${image}", "/${title}");
    exampleExpander = new EnglishExampleExpanderWikiModel(this.wi, new Locale("en"), "/${image}",
        "/${title}", wdh);
    morphologyExtractor = new EnglishMorphologyExtractorWikiModel(this.ewdh, this.wi,
        new Locale("en"), "/${image}", "/${title}");
    pronunciationExpander = new EnglishPronunciationExtractorWikiModel(this.wdh, this.wi,
        new Locale("en"), "/${image}", "/${title}");
    wikisaurusExtractor = new WikisaurusExtractor(this.ewdh);
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    wikiExpander.setPageName(wiktionaryPageName);
    definitionExpander.setPageName(wiktionaryPageName);
    exampleExpander.setPageName(wiktionaryPageName);
    morphologyExtractor.setPageName(wiktionaryPageName);
    pronunciationExpander.setPageName(wiktionaryPageName);
  }


  @Override
  public boolean filterOutPage(String pagename) {
    // Extract Wikisaurus and Reconstructed pages...
    return isTranslationPage(pagename)
        || (!isWikisaurus(pagename) && !isReconstructed(pagename) && super.filterOutPage(pagename));
  }

  private boolean isTranslationPage(String pagename) {
    return pagename.endsWith("/translations");
  }


  private boolean isReconstructed(String pagename) {
    return pagename.startsWith("Reconstruction:");
  }

  private boolean isWikisaurus(String pagename) {
    return pagename.startsWith("Wikisaurus:") || pagename.startsWith("Thesaurus:");
  }

  @Override
  public void extractData() {
    Consumer<Heading> sectionConsumer = this::extractLanguageSection;

    boolean isWikisaurus = isWikisaurus(getWiktionaryPageName());
    if (isWikisaurus) {
      setWiktionaryPageName(cutNamespace(getWiktionaryPageName()));
      sectionConsumer = this::extractWikisaurusLanguageSection;
    }
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText text = new WikiText(pageContent);

    // Iterate over all level 2 headers
    text.headers(2).stream().map(Token::asHeading).forEach(sectionConsumer);
    wdh.finalizePageExtraction();
  }

  private void extractLanguageSection(Heading heading) {
    String languageName = heading.getContent().getText().trim();
    Lang lg = ISO639_3.sharedInstance.getLangFromName(languageName);
    if (null != lg) {
      WikiContent sectionContent = heading.getSection().getContent();
      extractLanguageData(lg, sectionContent);
    } else {
      log.debug("Ignoring language section {} || {}", languageName, getWiktionaryPageName());

    }
  }

  private void extractWikisaurusLanguageSection(Heading heading) {
    String languageName = heading.getContent().getText().trim();
    if ("English".equals(languageName)) {
      String sectionContent = heading.getSection().getContent().getText();
      wikisaurusExtractor.extractWikisaurusSection(getWiktionaryPageName(), sectionContent);
    } else {
      log.debug("Wikisaurus: Ignoring language section {} || {}", languageName,
          getWiktionaryPageName());
    }
  }

  private String cutNamespace(String pagename) {
    int p = pagename.indexOf(":");
    return pagename.substring(p + 1);
  }

  protected void extractLanguageData(Lang lg, WikiContent content) {
    String l2 = lg.getPart1();
    if (null == l2 || l2.trim().isEmpty()) {
      l2 = lg.getId();
    }
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(l2)) {
      return;
    }
    wdh.initializeLanguageSection(l2);

    boolean ignorePOS = false;

    for (Token secionHeadingToken : content.headers()) {
      Heading sectionHeading = secionHeadingToken.asHeading();
      ignorePOS = extractBlock(sectionHeading, ignorePOS);
    }

    wdh.finalizeLanguageSection();
  }

  /**
   * returns true if the block is a POS that should be ignored
   *
   * @param h the heading of the section to be extracted
   * @param ignorePOS true if latest PoS was invalid and we should not extract this section
   * @return true iff the section is the beginning of a PoS section that should be ignored
   */
  private boolean extractBlock(Heading h, boolean ignorePOS) {
    String title = h.getContent().getText().trim();
    String nym;
    WikiContent blockContent = h.getSection().getPrologue();
    if (title.equals("Pronunciation")) {
      ewdh.initializeNewEtymology();
      extractPron(blockContent);
    } else if (WiktionaryDataHandler.isValidPOS(title)) {
      wdh.initializeLexicalEntry(title);
      ewdh.registerEtymologyPos(getWiktionaryPageName());
      extractMorphology(blockContent);
      extractHeadInformation(blockContent);
      // DONE: English definition comes along examples and nyms, extract these also.
      extractDefinitions(blockContent);
    } else if (title.equals("Translations")) { // TODO: some sections are using Translation in the
      // singular form...
      if (!ignorePOS) {
        extractTranslations(blockContent);
      }
    } else if (title.equals("Alternative spellings") || title.equals("Alternative forms")) {
      extractOrthoAlt(blockContent);
    } else if (title.equals("Conjugation")) {
      extractConjugation(blockContent);
    } else if (title.startsWith("Etymology")) {
      extractEtymology(blockContent.getBeginIndex(), blockContent.getEndIndex());
    } else if (title.equals("Derived terms")) {
      extractDerived(blockContent.getBeginIndex(), blockContent.getEndIndex());
      extractDerivedSection(blockContent);
    } else if (title.equals("Descendants")) {
      extractDescendants(blockContent.getBeginIndex(), blockContent.getEndIndex());
    } else if (null != (nym = EnglishGlobals.nymMarkerToNymName.get(title))) {
      extractNyms(nym, blockContent);
    } else {
      log.debug("Ignoring content of section {} in {}", title, this.getWiktionaryPageName());
    }
    return false;
  }

  private static final String[] heads = {"head", "head1", "head2"};
  private static final ClassBasedSequenceFilter linkResolver = new ClassBasedSequenceFilter();

  private static List<Token> getMyTemplateContent(WikiText.Token t) {
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

  private void extractHeadInformation(WikiContent text) {
    for (Token t : text.templatesOnUpperLevel()) {
      Template tmpl = (Template) t;
      if (tmpl.getName().equals("head") || tmpl.getName().startsWith("en-")) {
        Map<String, WikiContent> args = tmpl.getArgs();
        if (tmpl.getName().equals("head")) {
          String pos = args.get("2").toString();
          if (pos != null && pos.endsWith(" form")) {
            continue;
          }
        }
        for (String h : heads) {
          WikiContent head = args.get(h);
          if (null != head && !head.toString().trim().isEmpty()) {
            WikiCharSequence s = new WikiCharSequence(head, linkResolver);
            String headword = s.toString();
            if (isSignificantlyDifferent(headword, getWiktionaryPageName())) {
              log.debug("Found {} template with head {} // '{}' in '{}'", tmpl.getName(), head,
                  headword, getWiktionaryPageName());
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

  private void extractDefinitions(WikiContent text) {
    ClassBasedFilter indentedItems = new ClassBasedFilter();
    indentedItems.allowIndentedItem();
    Iterator<Token> definitionsListItems = (new WikiEventsSequence(text, indentedItems)).iterator();
    Set<Pair<Property, RDFNode>> referenceContext = null;

    while (definitionsListItems.hasNext()) {
      IndentedItem listItem = definitionsListItems.next().asIndentedItem();
      String liContent = listItem.getContent().getText();
      if (listItem instanceof NumberedListItem) {
        if (liContent.startsWith("*::")) {
          log.debug("Ignoring quotation meta [{}] — {}", liContent, getWiktionaryPageName());
        } else if (liContent.startsWith("*:")) {
          // It's a quotation content
          Set<Pair<Property, RDFNode>> citation = expandExample(liContent.substring(2).trim());
          Optional<Pair<Property, RDFNode>> value =
              citation.stream().filter(p -> p.getLeft().equals(RDF.value)).findFirst();
          if (null == referenceContext) {
            log.debug("A citation is given without reference [{}] — {}", liContent,
                getWiktionaryPageName());
            registerExampleIfNotNull(citation);
          } else if (value.isEmpty()) {
            log.debug("Unexpected empty example in [{}] — {}", liContent, getWiktionaryPageName());
          } else {
            citation.addAll(referenceContext);
            registerExampleIfNotNull(citation);
          }
        } else if (liContent.startsWith("*")) {
          // It's a quotation reference (that starts a new quotation)
          Set<Pair<Property, RDFNode>> citation = expandCitation(liContent.substring(1).trim());
          Optional<Pair<Property, RDFNode>> value =
              citation.stream().filter(p -> p.getLeft().equals(RDF.value)).findFirst();
          if (value.isEmpty()) {
            // It's only a reference and the citation itself is to be found later
            referenceContext = citation;
          } else {
            registerExampleIfNotNull(citation);
          }
        } else if (liContent.startsWith(":")) {
          // This is a simple example or a nym
          extractExample(liContent.substring(1).trim());
        } else {
          // This is a definition that starts a new word sense
          extractDefinition(liContent.trim(), listItem.asNumberedListItem().getLevel());
          referenceContext = null;
        }
      } else {
        log.trace("Unexpected IndentedItem in definition block [{}] : {}", getWiktionaryPageName(),
            listItem.getText());
      }
    }
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    definitionExpander.parseDefinition(definition, defLevel);
  }

  public void extractExample(String example) {
    Set<Pair<Property, RDFNode>> citation = expandExample(example);
    registerExampleIfNotNull(citation);
  }

  private void registerExampleIfNotNull(Set<Pair<Property, RDFNode>> context) {
    Optional<Pair<Property, RDFNode>> value =
        context.stream().filter(p -> p.getLeft().equals(RDF.value)).findFirst();
    if (value.isEmpty() && !context.isEmpty()) {
      // There is no example, it is a note that should be attached to the definition
      wdh.addToCurrentWordSense(context);
    }
    if (value.isPresent()) {
      ewdh.registerExample(context);
    }
  }

  public Set<Pair<Property, RDFNode>> expandCitation(String example) {
    Set<Pair<Property, RDFNode>> context = new HashSet<>();

    exampleExpander.expandCitation(example, context, wdh.getExtractedLanguage(),
        wdh.getCurrentEntryLanguage());
    return context;
  }

  public Set<Pair<Property, RDFNode>> expandExample(String example) {
    Set<Pair<Property, RDFNode>> context = new HashSet<>();
    exampleExpander.expandExample(example, context, wdh.getExtractedLanguage(),
        wdh.getCurrentEntryLanguage());
    return context;
  }

  // TODO: check correct parsing of From ''[[semel#Latin|semel]]'' + ''[[pro#Latin|pro]]'' +
  // ''[[semper#Latin|semper]]''
  protected void extractEtymology(int blockStart, int end) {
    if (wdh.isDisabled(ExtractionFeature.ETYMOLOGY)) {
      return;
    }
    if (getWiktionaryPageName().trim().split("\\s+").length >= 3) {
      log.trace("Ignoring etymology for: {}", getWiktionaryPageName());
      return;
    }

    if (log.isTraceEnabled()) {
      log.trace("ETYM {}: {}", ewdh.getCurrentEntryLanguage(),
          pageContent.substring(blockStart, end));
    }
    try {
      Etymology etymology =
          new Etymology(pageContent.substring(blockStart, end), ewdh.getCurrentEntryLanguage());

      etymology.fromDefinitionToSymbols();

      ewdh.registerEtymology(etymology);
    } catch (RuntimeException e) {
      log.error("Caught {} while registering etymology in {}", e, getWiktionaryPageName());
      if (log.isDebugEnabled()) {
        e.printStackTrace(System.err);
      }
    }
  }

  private void extractDerivedSection(WikiContent blockContent) {
    // blockContent.templatesOnUpperLevel().forEach(this::extractDerivationList);
    WikiCharSequence section = new WikiCharSequence(blockContent);
    DerivationsParser dp = new DerivationsParser(this.getWiktionaryPageName());
    dp.extractDerivations(section, ewdh);
  }

  // TODO: process * {{l|pt|mundinho}}, {{l|pt|mundozinho}} {{gloss|diminutives}}
  // * {{l|pt|mundão}} {{gloss|augmentative}}
  // DONE: process {{der4|title=Terms derived from ''free'' | [[freeball]], [[free-ball]] |
  // [[freebooter]] }}
  protected void extractDerived(int blockStart, int end) {
    if (getWiktionaryPageName().trim().split("\\s+").length >= 3) {
      return;
    }

    String lang = ewdh.getCurrentEntryLanguage();
    lang = EnglishLangToCode.threeLettersCode(lang);
    extractBulletList(pageContent.substring(blockStart, end), lang);

    extractTable(pageContent.substring(blockStart, end), lang);
  }

  protected void extractDescendants(int blockStart, int end) {
    if (getWiktionaryPageName().trim().split("\\s+").length >= 3) {
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
    for (Span l : WikiTool.locateEnclosedString(s, "{{", "}}")) {
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
    ArrayList<String> toreturn = new ArrayList<>();

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
    for (Span template : WikiTool.locateEnclosedString(s, "{{", "}}")) {
      Symbols b = new Symbols(s.substring(template.start + 2, template.end - 2), lang, "TEMPLATE");
      if (b.values != null && b.values.get(0).equals("ETYMTREE") && b.args.get("lang") != null) {
        String page = b.args.get("page");
        if (b.args.get("word1") == null) {
          page = page + this.getWiktionaryPageName();
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

  private void extractConjugation(WikiContent blockContent) {
    log.debug("Conjugation extraction not yet implemented in:\t{}", getWiktionaryPageName());
  }

  private final EnglishInflectionData plural = new EnglishInflectionData().plural();
  private final EnglishInflectionData singular = new EnglishInflectionData().singular();
  private final EnglishInflectionData comparative = new EnglishInflectionData().comparative();
  private final EnglishInflectionData superlative = new EnglishInflectionData().superlative();
  private final EnglishInflectionData pres3Sg =
      new EnglishInflectionData().presentTense().thirdPerson().singular();
  private final EnglishInflectionData presPtc =
      new EnglishInflectionData().presentTense().participle();
  private final EnglishInflectionData past = new EnglishInflectionData().pastTense();
  private final EnglishInflectionData pastPtc =
      new EnglishInflectionData().pastTense().participle();

  private void extractMorphology(WikiContent text) {
    if (ewdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
      return;
    }

    WikiEventsSequence wikiTemplates = text.templatesOnUpperLevel();

    int nbTempl = 0;
    for (Token wikiTemplate : wikiTemplates) {
      nbTempl++;
      Template tmpl = (Template) wikiTemplate;
      String tmplName = tmpl.getName();
      switch (tmplName) {
        case "en-noun":
        case "en-proper noun":
        case "en-proper-noun":
        case "en-prop":
        case "en-propn":
        case "en-plural noun":
        case "en-adj":
        case "en-adv":
        case "en-adjective":
        case "en-adj-more":
        case "en-adverb":
        case "en-adv-more":
        case "en-verb":
        case "en-abbr":
        case "en-acronym":
        case "en-con":
        case "en-cont":
        case "en-det":
        case "en-interj":
        case "en-interjection":
        case "en-intj":
        case "en-exclamation":
        case "en-part":
        case "en-particle":
        case "en-phrase":
        case "en-prep":
        case "en-initialism":
        case "en-prefix":
        case "en-preposition":
        case "en-prep phrase":
        case "en-PP":
        case "en-pp":
        case "en-pron":
        case "en-pronoun":
        case "en-proverb":
        case "en-punctuation mark":
        case "en-suffix":
        case "en-symbol":
        case "en-number":
          morphologyExtractor.parseMorphology(tmpl.getText());
          break;
        case "head": {
          Map<String, String> args = tmpl.getParsedArgs();
          String pos = args.get("2");
          if (null != pos && pos.endsWith("form")) {
            // This is a inflected form
            // TODO: Check if the inflected form is available in the base word morphology.
          } else {
            morphologyExtractor.parseMorphology(tmpl.getText());
          }
          break;
        }
        default:
          if (tmplName.startsWith("en-")) {
            log.debug("MORPH: unknown template {} ||| {}", tmpl, getWiktionaryPageName());
          }
          nbTempl--;
          break;
      }
    }
    if (nbTempl > 1) {
      log.debug("MORPH: more than 1 morph template in\t{}", this.getWiktionaryPageName());
    }

  }

  private void extractMorphologyOld(WikiContent text) {
    // TODO: For some entries, there are several morphology information covering different word
    // senses
    // TODO: Handle such cases (by creating another lexical entry ?) // Similar to reflexiveness in
    // French wiktionary
    if (ewdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
      return;
    }

    WikiEventsSequence wikiTemplates = text.templatesOnUpperLevel();

    // Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    // macroMatcher.region(startOffset, endOffset);

    // while (macroMatcher.find()) {
    // TODO: current code goes through all templates of the defintion block while it should only
    // process morphology templates.

    int nbTempl = 0;
    for (Token wikiTemplate : wikiTemplates) {
      nbTempl++;
      Template tmpl = (Template) wikiTemplate;
      // String g1 = macroMatcher.group(1);
      String g1 = tmpl.getName();
      switch (g1) {
        case "en-noun": {
          Map<String, String> args = tmpl.cloneParsedArgs();
          extractNounMorphology(args, false);
          break;
        }
        case "en-proper noun":
        case "en-proper-noun":
        case "en-prop": {
          Map<String, String> args = tmpl.cloneParsedArgs();
          extractNounMorphology(args, true);
          break;
        }
        case "en-plural noun": {
          ewdh.addInflectionOnCanonicalForm(new EnglishInflectionData().plural());
          Map<String, String> args = tmpl.cloneParsedArgs();
          if (args.containsKey("sg")) {
            String singularForm = args.get("sg");
            addForm(singular.toPropertyObjectMap(), singularForm);
            args.remove("sg");
          }
          if (!args.isEmpty()) {
            log.debug("en-plural noun macro: Non handled parameters : \t{}\tin\t{}", args,
                this.getWiktionaryPageName());
          }

          break;
        }
        case "en-adj":
        case "en-adv":
        case "en-adjective":
        case "en-adj-more":
        case "en-adverb":
        case "en-adv-more": {
          // log.debug("MORPHOLOGY EXTRACTION FROM : {}\tin\t{}", tmpl.toString(),
          // wiktionaryPageName);

          // TODO: mark canonicalForm as ????
          Map<String, String> args = tmpl.cloneParsedArgs();
          extractAdjectiveMorphology(args);

          break;
        }
        case "en-verb": {
          // TODO: extract verb morphology
          Map<String, String> args = tmpl.cloneParsedArgs();

          extractVerbMorphology(args);

          break;
        }
        case "en-abbr":
          // TODO: extract abbreviation morphology
          log.debug("Use of deprecated en-abbr template in\t{}", getWiktionaryPageName());
          break;
        case "en-acronym":
          // TODO: extract acronym morphology
          log.debug("Use of deprecated en-acronym template in\t{}", getWiktionaryPageName());
          break;
        case "en-con":
          // nothing to extract...
          break;
        case "en-cont":
          // contraction
          log.debug("Use of deprecated en-cont (contraction) template in\t{}",
              getWiktionaryPageName());
          break;
        case "en-det":
        case "en-interj":
        case "en-part":
        case "en-particle":
        case "en-phrase":
        case "en-prep":
          // nothing to extract
          break;
        case "en-initialism":
          // TODO: extract initialism morphology
          log.debug("Use of deprecated en-initialism template in\t{}", getWiktionaryPageName());
          break;
        case "en-prefix": {
          // nothing to extract ??
          Map<String, String> args = tmpl.cloneParsedArgs();
          args.remove("sort");
          if (!args.isEmpty()) {
            log.debug("other args in en-prefix template\t{}\tin\t{}", args,
                getWiktionaryPageName());
          }
          break;
        }
        case "en-prep phrase":
          // TODO: the first argument is the head if different from pagename...
          break;
        case "en-pron": {
          // TODO: extract part morphology
          Map<String, String> args = tmpl.cloneParsedArgs();
          if (null != args.get("desc")) {
            log.debug("desc argument in en-pron template in\t{}", getWiktionaryPageName());
            args.remove("desc");
          }
          if (!args.isEmpty()) {
            log.debug("other args in en-pron template\t{}\tin\t{}", args, getWiktionaryPageName());
          }
          break;
        }
        case "en-proverb":
          // TODO: the first argument (or head argument) is the head if different from pagename...
          break;
        case "en-punctuation mark":
          // TODO: the first argument (or head argument) is the head if different from pagename...
          break;
        case "en-suffix": {
          // TODO: cat2 and cat3 contains some additional categories...
          Map<String, String> args = tmpl.cloneParsedArgs();
          args.remove("sort");
          if (!args.isEmpty()) {
            log.debug("other args in en-suffix template\t{}\tin\t{}", args,
                getWiktionaryPageName());
          }
          break;
        }
        case "en-symbol":
          // TODO: the first argument is the head if different from pagename...
          break;
        case "en-number":
          // TODO:
          break;
        case "head": {
          Map<String, String> args = tmpl.getParsedArgs();
          String pos = args.get("2");
          if (null != pos && pos.endsWith("form")) {
            // This is a inflected form
            // TODO: Check if the inflected form is available in the base word morphology.
          } else {
            log.debug("MORPH: direct call to head\t{}\tin\t{}", tmpl, this.getWiktionaryPageName());
          }
          break;
        }
        default:
          // log.debug("NOMORPH PATTERN:\t {}\t in:\t{}", g1, wiktionaryPageName);
          nbTempl--;
          break;
      }
    }
    if (nbTempl > 1) {
      log.debug("MORPHTEMPLATE: more than 1 morph template in\t{}", this.getWiktionaryPageName());
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
      headword = getWiktionaryPageName();
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
    label: while (arg != null) {
      String note = args.get("pl" + argnum + "qual");
      switch (arg) {
        case "s":
          if (!uncountable) {
            ewdh.countable();
          }
          addForm(plural.toPropertyObjectMap(), headword + "s", note);
          break;
        case "es":
          if (!uncountable) {
            ewdh.countable();
          }
          addForm(plural.toPropertyObjectMap(), headword + "es", note);
          break;
        case "-":
          // uncountable or usually uncountable
          uncountable = true;
          ewdh.uncountable();
          break;
        case "~":
          // countable and uncountable
          ewdh.countable();
          ewdh.uncountable();
          break;
        case "!":
          // plural not attested (exit loop)
          break label;
        case "?":
          // unknown or uncertain plural (exit loop)
          break label;
        default:
          // pluralForm
          // TODO: if plural form = singular form, note entry as invariant
          if (!uncountable) {
            ewdh.countable();
          }
          addForm(plural.toPropertyObjectMap(), arg, note);
          break;
      }

      arg = args.get(Integer.toString(++argnum));
      args.remove(Integer.toString(argnum));
    }

    if (!args.isEmpty()) {
      log.debug("en-noun macro: Non handled parameters : \t{}\tin\t{}", args,
          this.getWiktionaryPageName());
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
      headword = getWiktionaryPageName();
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
      if (arg.equals("more") && !"many".equals(getWiktionaryPageName())
          && !"much".equals(getWiktionaryPageName())) {
        if (!notComparable) {
          ewdh.comparable();
        }
        addForm(comparative.toPropertyObjectMap(), "more " + headword);
        if (null != args.get("sup" + argnum)) {
          log.debug("Irregular superlative with comparative with more in\t{}",
              getWiktionaryPageName());
        }
        addForm(superlative.toPropertyObjectMap(), "most " + headword);
      } else if (arg.equals("further") && !"far".equals(getWiktionaryPageName())) {
        if (!notComparable) {
          ewdh.comparable();
        }
        addForm(comparative.toPropertyObjectMap(), "further " + headword);
        if (null != args.get("sup" + argnum)) {
          log.debug("Irregular superlative with comparative with further in\t{}",
              getWiktionaryPageName());
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
          this.getWiktionaryPageName());
    }
  }

  private void extractVerbMorphology(Map<String, String> args) {
    // Code based on wiktionary en-headword Lua Module code (date: 01/02/2016)

    String par1 = args.get("1");
    String par2 = args.get("2");
    String par3 = args.get("3");
    String par4 = args.get("4");

    String pres3sgForm = (null != par1) ? par1 : this.getWiktionaryPageName() + "s";
    String presPtcForm = (null != par2) ? par2 : this.getWiktionaryPageName() + "ing";
    String pastForm = (null != par3) ? par3 : this.getWiktionaryPageName() + "ed";
    String pastPtcForm = null;

    if (null != par1 && null == par2 && null == par3) {
      // This is the "new" format, which uses only the first parameter.
      switch (par1) {
        case "es":
          pres3sgForm = this.getWiktionaryPageName() + "es";
          break;
        case "ies":
          if (!this.getWiktionaryPageName().endsWith("y")) {
            log.debug("VERBMORPH : Incorrect en-verb parameter \"ies\" on non y ending verb\t{}",
                this.getWiktionaryPageName());
          }
          String stem =
              this.getWiktionaryPageName().substring(0, this.getWiktionaryPageName().length() - 1);
          pres3sgForm = stem + "ies";
          presPtcForm = stem + "ying";
          pastForm = stem + "ied";
          break;
        case "d":
          pres3sgForm = this.getWiktionaryPageName() + "s";
          presPtcForm = this.getWiktionaryPageName() + "ing";
          pastForm = this.getWiktionaryPageName() + "d";
          break;
        default:
          pres3sgForm = this.getWiktionaryPageName() + "s";
          presPtcForm = par1 + "ing";
          pastForm = par1 + "ed";
          break;
      }
    } else {
      // This is the "legacy" format, using the second and third parameters as well.
      // It is included here for backwards compatibility and to ease the transition.
      if (null != par3) {
        switch (par3) {
          case "es":
            pres3sgForm = par1 + par2 + "es";
            presPtcForm = par1 + par2 + "ing";
            pastForm = par1 + par2 + "ed";
            break;
          case "ing":
            pres3sgForm = this.getWiktionaryPageName() + "s";
            presPtcForm = par1 + par2 + "ing";

            if ("y".equals(par2)) {
              pastForm = this.getWiktionaryPageName() + "d";
            } else {
              pastForm = par1 + par2 + "ed";
            }
            break;
          case "ed":
            if ("i".equals(par2)) {
              pres3sgForm = par1 + par2 + "es";
              presPtcForm = this.getWiktionaryPageName() + "ing";
            } else {
              pres3sgForm = this.getWiktionaryPageName() + "s";
              presPtcForm = par1 + par2 + "ing";
            }
            pastForm = par1 + par2 + "ed";
            break;
          case "d":
            pres3sgForm = this.getWiktionaryPageName() + "s";
            presPtcForm = par1 + par2 + "ing";
            pastForm = par1 + par2 + "d";
            break;
          default:
            // log.debug("VERBMORPH : Incorrect en-verb 3rd parameter on
            // verb\t{}",this.wiktionaryPageName);
            break;
        }
      } else {
        if (null != par2) {
          switch (par2) {
            case "es":
              pres3sgForm = par1 + "es";
              presPtcForm = par1 + "ing";
              pastForm = par1 + "ed";
              break;
            case "ies":
              if (!(par1 + "y").equals(this.getWiktionaryPageName())) {
                log.debug(
                    "VERBMORPH : Incorrect en-verb 2rd parameter ies  with stem different to pagename on verb\t{}",
                    this.getWiktionaryPageName());
              }
              pres3sgForm = par1 + "ies";
              presPtcForm = par1 + "ying";
              pastForm = par1 + "ied";
              break;
            case "ing":
              pres3sgForm = this.getWiktionaryPageName() + "s";
              presPtcForm = par1 + "ing";
              pastForm = par1 + "ed";
              break;
            case "ed":
              pres3sgForm = this.getWiktionaryPageName() + "s";
              presPtcForm = par1 + "ing";
              pastForm = par1 + "ed";
              break;
            case "d":
              if (!this.getWiktionaryPageName().equals(par1)) {
                log.debug(
                    "VERBMORPH : Incorrect en-verb 2rd parameter d with par1 different to pagename on verb\t{}",
                    this.getWiktionaryPageName());
              }
              pres3sgForm = this.getWiktionaryPageName() + "s";
              presPtcForm = par1 + "ing";
              pastForm = par1 + "d";
              break;
            default:
              log.debug("VERBMORPH : unexpected 2rd parameter \"{}\" on verb\t{}", par2,
                  this.getWiktionaryPageName());
              break;
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
        log.debug("Missing superlative for irregular comparative in\t{}", getWiktionaryPageName());
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
    WikiText txt = new WikiText(getWiktionaryPageName(), wikiSource);
    extractTranslations(txt.content());
  }

  // DONE: the multitrans template hides all its subtemplates. Either make a stream entering
  // multitrans or call the extraction recursively, BUT in this case the gloss should be external
  // to the recursive call (gloss is defined outside and may be redefined inside...
  private void extractTranslations(WikiContent txt) {
    processedLinks.clear();
    processedLinks.add(getWiktionaryPageName());

    AtomicReference<Resource> currentGloss = new AtomicReference<>();
    AtomicInteger rank = new AtomicInteger(1);
    // TODO: there are templates called "qualifier" used to further qualify the translation check
    // and evaluate if extracting its data is useful.
    // TODO: Handle trans-see links that point to the translation section of a synonym. Keep the
    // link in the extracted dataset.
    templates(txt).sequential().forEach(t -> {
      String tName = t.getName();
      if (tName.equals("t+") || tName.equals("t-") || tName.equals("tø") || tName.equals("t")
          || tName.equals("t-check") || tName.equals("t+check") || tName.equals("t-simple")
          || tName.equals("tt") || tName.equals("tt+")) {
        WikiContent l = t.getArgs().get("1");
        WikiContent word = t.getArgs().get("2");
        WikiContent usageContent = t.getArgs().get("3");
        String usage = (usageContent == null) ? "" : usageContent.toString();
        if (word == null) {
          log.debug("No (required) translation in {} : {}", t, getWiktionaryPageName());
          return;
        }
        Map<String, WikiContent> args = new HashMap<>(t.getArgs()); // clone the args map so that
        // we can destroy it
        args.remove("1");
        args.remove("2");
        args.remove("3");
        StringBuilder remainingArgs = new StringBuilder();
        if (args.size() > 0) {
          for (Entry<String, WikiContent> s : args.entrySet()) {
            remainingArgs.append("|").append(s.getKey()).append("=").append(s.getValue());
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
          log.debug("null language (first positional arg) in {} > {}", t, getWiktionaryPageName());
        }
        lang = EnglishLangToCode.threeLettersCode(lang);
        if (lang != null) {
          // TODO: handle translations that are the result of template expansions (e.g. "anecdotal
          // evidence").
          // TODO : handle translations that are links to other entries (maybe keep those links)
          wdh.registerTranslation(lang, currentGloss.get(), usage, word.toString());
        }
      } else if (tName.equals("trans-top") || tName.equals("trans-top-also")) {
        // Get the gloss that should help disambiguate the source acception
        String g2 = t.getParsedArgs().get("1");
        // Ignore gloss if it is a macro
        if (g2 != null && !g2.startsWith("{{")) {
          currentGloss.set(wdh.createGlossResource(g2, rank.getAndAdd(1)));
        } else {
          currentGloss.set(null);
        }
      } else if (tName.equals("checktrans-top")) {
        // forget glose.
        currentGloss.set(null);
      } else if (tName.equals("trans-mid")) {
        // just ignore it
      } else if (tName.equals("trans-bottom")) {
        // Forget the current glose
        currentGloss.set(null);
      } else if (tName.equals("section link") || tName.equals("see translation subpage")) {
        String link;
        if (tName.equals("section link")) {
          link = t.getParsedArgs().get("1");
        } else {
          String section = t.getParsedArgs().getOrDefault("1", "English");
          String page = t.getParsedArgs().getOrDefault("2", getWiktionaryPageName());
          link = page + "/translations#" + section;
        }
        log.debug("Section link: {} for entry {}", link, getWiktionaryPageName());
        if (link != null) {
          WikiContent translationContent = getTranslationContentForLink(link);
          if (null != translationContent) {
            extractTranslations(translationContent);
          }
        }
      } else if (log.isDebugEnabled()) {
        log.debug("Ignored template: {} in translation section for entry {}", t,
            getWiktionaryPageName());
      }
    });
  }

  private Stream<Template> templates(WikiContent txt) {
    return new EnglishTranslationTemplateStream().visit(txt);
  }

  /**
   * Returns the content of the specified translation page's section. returns null if the page does
   * not exists or has already been extracted.
   *
   * @param link
   * @return
   */
  private WikiContent getTranslationContentForLink(String link) {
    if (!processedLinks.add(link)) {
      return null;
    }
    String[] linkAndSection = link.split("#");
    String translationPage = linkAndSection[0];
    String translationSection = linkAndSection[1];

    String translationPageContent = wi.getTextOfPageWithRedirects(translationPage);
    if (null == translationPageContent) {
      log.debug("Translation link: Could not retrieve page {} in {}", translationPage,
          getWiktionaryPageName());
      return null;
    }
    // TODO : extract the correct section from the full page.
    // Assume there is only on language and the anchor corresponds to level 3 Header (POS)
    WikiText text = new WikiText(getWiktionaryPageName(), translationPageContent);
    for (WikiSection s : text.sections(3)) {
      // return the first matching section
      if (s.getHeading().getContent().toString().equals(translationSection)) {
        return s.getContent();
      }
    }
    log.debug("Could not find appropriate section {} in translation section link target for {}",
        translationSection, getWiktionaryPageName());
    return text.content();
  }

  private Set<String> processedLinks = new HashSet<>();

  private void extractTranslations(int startOffset, int endOffset) {
    processedLinks.clear();
    processedLinks.add(getWiktionaryPageName());
    extractTranslations(pageContent.substring(startOffset, endOffset));
  }

  protected void extractNyms(String synRelation, WikiText.WikiContent blockContent) {
    ClassBasedFilter filter = new ClassBasedFilter();
    filter.allowListItem().allowIndentation();
    // TODO ! For now I mimick the previous behaviour by
    // allowing ListItems and Indentations (consider adding NumberedListItem ?) !!!!!

    WikiEventsSequence wikiEvents = blockContent.filteredTokens(filter);

    for (WikiText.Token tok : wikiEvents) {
      if (tok instanceof WikiText.IndentedItem) {
        extractNymValues(synRelation, tok.asIndentedItem().getContent());
      }
    }
  }

  private void extractNymValues(String synRelation, WikiText.WikiContent content) {
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
        if (!linkText.isEmpty() && !linkText.startsWith("Category:") && !linkText.startsWith("#")) {
          if (isWikisaurus(linkText)) {
            // NOP: Wikisaurus pages are extracted independently
            // TODO : should we note that the current lexical entry points
            // to this particular wikisaurus page
          } else {
            wdh.registerNymRelation(linkText, synRelation, currentGloss, null);
          }
        }
      } else if (tok instanceof WikiText.Template) {
        WikiText.Template tmpl = (WikiText.Template) tok;
        if ("l".equals(tmpl.getName()) || "link".equals(tmpl.getName())) {
          Map<String, String> args = tmpl.cloneParsedArgs();
          if ("en".equals(args.get("1"))) {
            String target = args.get("2");
            args.remove("2");
            args.remove("1");
            if (!args.isEmpty()) {
              log.debug("Unhandled remaining args {} in {}", args.entrySet(),
                  this.getWiktionaryPageName());
            }
            wdh.registerNymRelation(target, synRelation, currentGloss, null);
          } else {
            log.debug("NYM: Non English link in {} : {}", tmpl, this.getWiktionaryPageName());
          }
        } else if ("sense".equals(tmpl.getName())) {
          String g = tmpl.getParsedArgs().get("1");
          for (int i = 2; i < 9; i++) {
            String p = tmpl.getParsedArgs().get(Integer.toString(i));
            if (null != p) {
              g += ", " + p;
            }
          }
          currentGloss = wdh.createGlossResource(g, rank++);
        }
      }
    }
  }

  private static final Pattern languagePronPattern = Pattern.compile("(?:...?-IPA)");
  private final Matcher languagePronTemplate = languagePronPattern.matcher("");

  protected void extractPron(WikiContent pronContent) {
    pronContent.templates().stream().map(Token::asTemplate).forEach(template -> {
      String tname;
      if ((tname = template.getName()).equals("IPA") || tname.equals("IPA-lite")) {
        Map<String, String> args = template.cloneParsedArgs();
        String lg = args.get("1");
        args.remove("1");
        extractPronFromTemplateArgs(lg, args);
      } else if (tname.equals("IPAchar")) {
        Map<String, String> args = template.getParsedArgs();
        extractPronFromTemplateArgs(null, args);
      } else if (shoudlExpandPronTemplate(template)) {
        pronunciationExpander.parsePronunciation(template.toString());
      } else if (tname.equals("a") || tname.equals("accent")) {
        log.trace("Pronunciation Accent: {} ||| {}", template, wdh.currentPagename());
      } else {
        log.debug("Pronunciation {}: ignored template {} \\ {}", wdh.getCurrentEntryLanguage(),
            template, wdh.currentPagename());
      }
    });
  }

  private void extractPronFromTemplateArgs(String lg, Map<String, String> args) {
    if (null != lg && !wdh.getCurrentEntryLanguage().equals(lg)) {
      log.debug("Non Matching Languages (actual: {} / expected: {}) pronunciation in page {}.",
          args.get("1"), wdh.getCurrentEntryLanguage(), this.getWiktionaryPageName());
    }
    if (args.containsKey("10")) {
      log.warn("More than 10 pronunciations in {}", getWiktionaryPageName());
    }
    for (int i = 1; i < 9; i++) {
      String pronunciation = args.get(Integer.toString(i));
      if (null == pronunciation || pronunciation.equals("")) {
        continue;
      }
      wdh.registerPronunciation(pronunciation.trim(), "en-fonipa");
    }
  }

  private static final Set<String> pronTemplateToExpand = new HashSet<>();

  static {
    pronTemplateToExpand.add("eo-pron");
    pronTemplateToExpand.add("fi-pronunciation");
    pronTemplateToExpand.add("fi-p");
    pronTemplateToExpand.add("IPA letters");
    pronTemplateToExpand.add("it-pr");
    pronTemplateToExpand.add("ja-pron");
    pronTemplateToExpand.add("ko-hanja-pron");
    pronTemplateToExpand.add("pl-pronunciation");
    pronTemplateToExpand.add("pl-p");
    pronTemplateToExpand.add("ru-IPA-manual");
    pronTemplateToExpand.add("vi-ipa");
    pronTemplateToExpand.add("vi-pron");
    pronTemplateToExpand.add("za-pron");
  }

  private boolean shoudlExpandPronTemplate(Template template) {
    return languagePronTemplate.reset(template.getName()).matches()
        || pronTemplateToExpand.contains(template.getName());
  }

  private static boolean isPositiveInt(String str) {
    if (str == null) {
      return false;
    }
    int length = str.length();
    if (length == 0) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      char c = str.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  protected void extractOrthoAlt(WikiContent blockContent) {
    blockContent.wikiTokens().stream().filter(t -> t instanceof ListItem).map(Token::asListItem)
        .forEach(li -> {
          li.getContent().templates().stream().map(Token::asTemplate).forEach(t -> {
            if ("alter".equals(t.getName()) || "alt".equals(t.getName())) {
              for (String k : t.getParsedArgs().keySet()) {
                if (isPositiveInt(k)) {
                  if ("1".equals(k)) {
                    continue;
                  }
                  String alt = t.getParsedArgs().get(k);
                  if (alt != null && !alt.equals("")) {
                    wdh.registerAlternateSpelling(alt);
                  } else {
                    break;
                  }
                } else {
                  log.debug("Alternate Forms: unexpected named arg {} in {}", k,
                      getWiktionaryPageName());
                }
              }
            } else {
              log.debug("Alternate Forms: Unhandled template {} in {}", t.getName(),
                  getWiktionaryPageName());
            }
          });
        });
  }

  @Override
  public void postProcessData(String dumpFileVersion) {
    if (log.isTraceEnabled()) {
      ((DbnaryWikiModel) pronunciationExpander).displayGlobalTrace("Pronunciation Model");
      ((DbnaryWikiModel) definitionExpander).displayGlobalTrace("Definition Model");
    }
    ewdh.postProcessEtymology();
    super.postProcessData(dumpFileVersion);
  }
}
