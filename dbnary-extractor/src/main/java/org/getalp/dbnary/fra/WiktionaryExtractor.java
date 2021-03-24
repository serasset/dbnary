package org.getalp.dbnary.fra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.fra.morphology.InflectionExtractorWikiModel;
import org.getalp.dbnary.fra.morphology.VerbalInflexionExtractorWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPattern;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.dbnary.wiki.WikiTool;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.PhoneticRepresentation;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  // NOTE: to subclass the extractor, you need to define how a language section is recognized.
  // then, how are sections recognized and what is their semantics.
  // then, how to extract specific elements from the particular sections
  protected final static String languageSectionPatternString;

  protected final static String languageSectionPatternString1 = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
  protected final static String languageSectionPatternString2 =
      "==\\s*\\{\\{langue\\|([^\\}]*)\\}\\}\\s*==";
  // TODO: handle morphological informations e.g. fr-rég template ?
  protected final static String pronunciationPatternString =
      "\\{\\{pron\\|([^\\|\\}]*)\\|([^\\}]*)\\}\\}";

  protected final static String otherFormPatternString = "\\{\\{fr-[^\\}]*\\}\\}";

  private String lastExtractedPronunciationLang = null;

  private static Pattern inflectionMacroNamePattern = Pattern.compile("^fr-");
  protected final static String inflectionDefPatternString =
      "^\\# ''([^\n]+) (?:de'' |d\\’''||(?:du verbe|du nom|de l’adjectif)'' )\\[\\[([^\n]+)\\]\\]\\.$";
  protected final static Pattern inflectionDefPattern =
      Pattern.compile(inflectionDefPatternString, Pattern.MULTILINE);

  private static HashMap<String, String> posMarkers;
  private static HashSet<String> ignorablePosMarkers;
  private static HashSet<String> sectionMarkers;

  private final static HashMap<String, String> nymMarkerToNymName;

  private static HashSet<String> unsupportedMarkers = new HashSet<>();

  public static final Locale frLocale = new Locale("fr");

  // private static Set<String> affixesToDiscardFromLinks = null;
  private static void addPos(String pos) {
    posMarkers.put(pos, pos);
  }

  private static void addPos(String p, String n) {
    posMarkers.put(p, n);
  }

  static {
    languageSectionPatternString =
        new StringBuilder().append("(?:").append(languageSectionPatternString1).append(")|(?:")
            .append(languageSectionPatternString2).append(")").toString();

    posMarkers = new HashMap<>(130);
    ignorablePosMarkers = new HashSet<>(130);

    addPos("-déf-");
    addPos("-déf-/2");
    addPos("-déf2-");
    addPos("--");
    addPos("-adj-");
    addPos("-adj-/2");
    ignorablePosMarkers.add("-flex-adj-indéf-");
    addPos("-adj-dém-");
    addPos("-adj-excl-");
    addPos("-adj-indéf-");
    addPos("-adj-int-");
    addPos("-adj-num-");
    addPos("-adj-pos-");
    addPos("-adv-");
    addPos("-adv-int-");
    addPos("-adv-pron-");
    addPos("-adv-rel-");
    addPos("-aff-");
    addPos("-art-");
    ignorablePosMarkers.add("-flex-art-déf-");
    ignorablePosMarkers.add("-flex-art-indéf-");
    ignorablePosMarkers.add("-flex-art-part-");
    addPos("-art-déf-");
    addPos("-art-indéf-");
    addPos("-art-part-");
    addPos("-aux-");
    addPos("-circonf-");
    addPos("-class-");
    addPos("-cpt-");
    addPos("-conj-");
    addPos("-conj-coord-");
    addPos("-cont-");
    addPos("-copule-");
    addPos("-corrélatif-");
    addPos("-erreur-");
    addPos("-faux-prov-");
    ignorablePosMarkers.add("-flex-adj-");
    ignorablePosMarkers.add("-flex-adj-num-");
    ignorablePosMarkers.add("-flex-adj-pos-");
    ignorablePosMarkers.add("-flex-adv-");
    ignorablePosMarkers.add("-flex-art-");
    ignorablePosMarkers.add("-flex-aux-");
    ignorablePosMarkers.add("-flex-conj-");
    ignorablePosMarkers.add("-flex-interj-");
    ignorablePosMarkers.add("-flex-lettre-");
    ignorablePosMarkers.add("-flex-loc-adj-");
    ignorablePosMarkers.add("-flex-loc-conj-");
    ignorablePosMarkers.add("-flex-loc-nom-");
    ignorablePosMarkers.add("-flex-loc-verb-");
    ignorablePosMarkers.add("-flex-nom-");
    ignorablePosMarkers.add("-flex-nom-fam-");
    ignorablePosMarkers.add("-flex-nom-pr-");
    ignorablePosMarkers.add("-flex-mots-diff-");
    ignorablePosMarkers.add("-flex-prénom-");
    ignorablePosMarkers.add("-flex-prép-");
    ignorablePosMarkers.add("-flex-pronom-");
    ignorablePosMarkers.add("-flex-pronom-indéf-");
    ignorablePosMarkers.add("-flex-pronom-int-");
    ignorablePosMarkers.add("-flex-pronom-pers-");
    ignorablePosMarkers.add("-flex-pronom-rel-");
    ignorablePosMarkers.add("-flex-verb-");
    ignorablePosMarkers.add("-inf-");
    addPos("-interf-");
    addPos("-interj-");
    addPos("-lettre-");
    addPos("-loc-");
    addPos("-loc-adj-");
    addPos("-loc-adv-");
    addPos("-loc-conj-");
    addPos("-loc-dét-");
    addPos("-loc-interj-");
    addPos("-loc-nom-");
    addPos("-loc-phr-");
    addPos("-loc-post-");
    addPos("-loc-prép-");
    addPos("-loc-pronom-");
    addPos("-loc-verb-");
    addPos("-nom-");
    addPos("-nom-fam-");
    addPos("-nom-ni-");
    addPos("-nom-nu-");
    addPos("-nom-nn-");
    addPos("-nom-npl-");
    addPos("-nom-pr-");
    addPos("-nom-sciences-");
    addPos("-numér-");
    addPos("-onoma-");
    addPos("-part-");
    addPos("-post-");
    addPos("-préf-");
    addPos("-prénom-");
    addPos("-prép-");
    addPos("-pronom-");
    addPos("-pronom-adj-");
    addPos("-pronom-dém-");
    addPos("-pronom-indéf-");
    addPos("-pronom-int-");
    addPos("-pronom-pers-");
    addPos("-pronom-pos-");
    addPos("-pronom-rel-");
    addPos("-prov-");
    addPos("-racine-");
    addPos("-radical-");
    addPos("-rimes-");
    addPos("-signe-");
    addPos("-sin-");
    addPos("-subst-pron-pers-");
    ignorablePosMarkers.add("-suf-");
    ignorablePosMarkers.add("-flex-suf-");
    ignorablePosMarkers.add("-symb-");
    addPos("type");
    addPos("-var-typo-");
    addPos("-verb-");
    addPos("-verb-pr-");

    // S section titles
    // TODO: get alternate from https://fr.wiktionary.org/wiki/Module:types_de_mots/data and
    // normalize the part of speech
    // ADJECTIFS
    addPos("adjectif", "-adj-");
    addPos("adj", "-adj-");
    addPos("adjectif qualificatif", "-adj-");

    // ADVERBES
    addPos("adverbe", "-adv-");
    addPos("adv", "-adv-");
    addPos("adverbe interrogatif");
    addPos("adv-int");
    addPos("adverbe int");
    addPos("adverbe pronominal");
    addPos("adv-pr");
    addPos("adverbe pro");
    addPos("adverbe relatif");
    addPos("adv-rel");
    addPos("adverbe rel");

    // CONJONCTIONS
    addPos("conjonction");
    // addPos("conj");
    addPos("conjonction de coordination");
    addPos("conj-coord");
    addPos("conjonction coo");

    addPos("copule");

    // DÉTERMINANTS
    addPos("adjectif démonstratif");
    addPos("adj-dém");
    addPos("adjectif dém");
    addPos("déterminant");
    addPos("dét");
    addPos("adjectif exclamatif");
    addPos("adj-excl");
    addPos("adjectif exc");
    addPos("adjectif indéfini");
    addPos("adj-indéf");
    addPos("adjectif ind");
    addPos("adjectif interrogatif");
    addPos("adj-int");
    addPos("adjectif int");
    addPos("adjectif numéral");
    addPos("adj-num");
    addPos("adjectif num");
    addPos("adjectif possessif");
    addPos("adj-pos");
    addPos("adjectif pos");

    addPos("article");
    addPos("art");
    addPos("article défini");
    addPos("art-déf");
    addPos("article déf");
    addPos("article indéfini");
    addPos("art-indéf");
    addPos("article ind");
    addPos("article partitif");
    addPos("art-part");
    addPos("article par");

    // NOMS
    addPos("nom", "-nom-");
    addPos("substantif", "-nom-");
    addPos("nom commun", "-nom-");
    addPos("nom de famille");
    addPos("nom-fam");
    addPos("patronyme");
    addPos("nom propre", "-nom-pr-");
    addPos("nom-pr", "-nom-pr-");
    addPos("nom scientifique");
    addPos("nom-sciences");
    addPos("nom science");
    addPos("nom scient");
    addPos("prénom");

    // PRÉPOSITION
    addPos("préposition");
    addPos("prép");

    // PRONOMS
    addPos("pronom");
    addPos("pronom-adjectif");
    addPos("pronom démonstratif");
    addPos("pronom-dém");
    addPos("pronom dém");
    addPos("pronom indéfini");
    addPos("pronom-indéf");
    addPos("pronom ind");
    addPos("pronom interrogatif");
    addPos("pronom-int");
    addPos("pronom int");
    addPos("pronom personnel");
    addPos("pronom-pers");
    addPos("pronom-per");
    addPos("pronom réf");
    addPos("pronom-réfl");
    addPos("pronom réfléchi");
    addPos("pronom possessif");
    addPos("pronom-pos");
    addPos("pronom pos");
    addPos("pronom relatif");
    addPos("pronom-rel");
    addPos("pronom rel");

    // VERBES
    addPos("verbe", "-verb-");
    addPos("verb", "-verb-");
    addPos("verbe pronominal");
    addPos("verb-pr");
    addPos("verbe pr");

    // EXCLAMATIONS
    addPos("interjection");
    addPos("interj");
    addPos("onomatopée");
    addPos("onoma");
    addPos("onom");

    // PARTIES TODO: Extract affixes in French
    // addPos("affixe");
    // addPos("aff");
    // addPos("circonfixe");
    // addPos("circonf");
    // addPos("circon");
    // addPos("infixe");
    // addPos("inf");
    // addPos("interfixe");
    // addPos("interf");
    // addPos("particule");
    // addPos("part");
    // addPos("particule numérale");
    // addPos("part-num");
    // addPos("particule num");
    // addPos("postposition");
    // addPos("post");
    // addPos("postpos");
    // addPos("préfixe");
    // addPos("préf");
    // addPos("radical");
    // addPos("rad");
    // addPos("suffixe");
    // addPos("suff");
    // addPos("suf");
    //
    // addPos("pré-verbe");
    // addPos("pré-nom");

    // PHRASES
    addPos("locution");
    addPos("loc");
    addPos("locution-phrase");
    addPos("loc-phr");
    addPos("locution-phrase");
    addPos("locution phrase");
    addPos("proverbe");
    addPos("prov");

    // DIVERS
    addPos("quantificateur");
    addPos("quantif");
    addPos("variante typographique");
    addPos("var-typo");
    addPos("variante typo");
    addPos("variante par contrainte typographique");

    // CARACTÈRES
    ignorablePosMarkers.add("lettre");

    ignorablePosMarkers.add("symbole");
    ignorablePosMarkers.add("symb");
    addPos("classificateur");
    addPos("class");
    addPos("classif");
    addPos("numéral");
    addPos("numér");
    addPos("num");
    addPos("sinogramme");
    addPos("sinog");
    addPos("sino");

    addPos("erreur");
    addPos("faute");
    addPos("faute d'orthographe");
    addPos("faute d’orthographe");

    // Spéciaux
    addPos("gismu");
    addPos("rafsi");

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
    nymMarkerToNymName.put("-méton-", "meto");
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
    sectionMarkers = new HashSet<>(200);
    sectionMarkers.addAll(posMarkers.keySet());
    sectionMarkers.addAll(nymMarkerToNymName.keySet());
    sectionMarkers.add("-étym-");
    sectionMarkers.add("-voc-");
    sectionMarkers.add("-trad-");
    sectionMarkers.add("-note-");
    sectionMarkers.add("-réf-");
    sectionMarkers.add("clé de tri");
    sectionMarkers.add("-anagr-");
    sectionMarkers.add("-drv-");
    sectionMarkers.add("-voir-");
    sectionMarkers.add("-pron-");
    sectionMarkers.add("-gent-");
    sectionMarkers.add("-apr-");
    sectionMarkers.add("-paro-");
    sectionMarkers.add("-homo-");
    sectionMarkers.add("-exp-");
    sectionMarkers.add("-compos-");
    // DONE: prendre en compte la variante orthographique (différences avec -ortho-alt- ?)
    sectionMarkers.add("-var-ortho-");

    // TODO trouver tous les modèles de section...

    // affixesToDiscardFromLinks = new HashSet<String>();
    // affixesToDiscardFromLinks.add("s");
  }

  WiktionaryDataHandler frwdh;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    frwdh = (WiktionaryDataHandler) wdh;
  }

  protected final static Pattern languageSectionPattern;
  protected final static Pattern pronunciationPattern;
  protected final static Pattern otherFormPattern;

  static {
    languageSectionPattern = Pattern.compile(languageSectionPatternString);
    pronunciationPattern = Pattern.compile(pronunciationPatternString);
    otherFormPattern = Pattern.compile(otherFormPatternString);
  }

  private enum Block {
    NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK
  }

  private Block currentBlock = Block.NOBLOCK;
  private int blockStart = -1;
  private int blocktitleStart = -1;

  private String currentNym = null;

  protected ExampleExpanderWikiModel exampleExpander;
  protected FrenchDefinitionExtractorWikiModel definitionExpander;
  protected InflectionExtractorWikiModel morphologyExtractor;
  protected VerbalInflexionExtractorWikiModel verbalInflectionExtractor;
  protected ExpandAllWikiModel glossExtractor;

  @Override
  public void setWiktionaryIndex(WiktionaryIndex wi) {
    super.setWiktionaryIndex(wi);
    exampleExpander = new ExampleExpanderWikiModel(wi, new Locale("fr"),
        "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    definitionExpander = new FrenchDefinitionExtractorWikiModel(this.wdh, this.wi, new Locale("fr"),
        "/${image}", "/${title}");
    verbalInflectionExtractor = new VerbalInflexionExtractorWikiModel(this.wdh, this.wi,
        new Locale("fr"), "/${image}", "/${title}");
    morphologyExtractor = new InflectionExtractorWikiModel(this.wdh, this.wi, new Locale("fr"),
        "/${image}", "/${title}");
    glossExtractor = new ExpandAllWikiModel(this.wi, new Locale("fr"), "/${image}", "/${title}");
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    exampleExpander.setPageName(this.getWiktionaryPageName());
    definitionExpander.setPageName(this.getWiktionaryPageName());
    morphologyExtractor.setPageName(this.getWiktionaryPageName());
    verbalInflectionExtractor.setPageName(this.getWiktionaryPageName());
    glossExtractor.setPageName(this.getWiktionaryPageName());
  }

  private Set<String> defTemplates = null;

  protected boolean isFrenchLanguageHeader(Matcher m) {
    return (null != m.group(1) && m.group(1).equals("fr"))
        || (null != m.group(2) && m.group(2).equals("fr"));
  }

  public String getLanguageInHeader(Matcher m) {
    if (null != m.group(1)) {
      return m.group(1);
    }

    if (null != m.group(2)) {
      return m.group(2);
    }

    return null;
  }

  @Override
  public void extractData() {
    extractData(false);
  }

  protected void extractData(boolean extractForeignData) {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(pageContent);
    WikiDocument doc = page.asStructuredDocument();
    doc.getContent().wikiTokens().stream().filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection)
        .forEach(wikiSection -> extractSection(wikiSection, extractForeignData));
    wdh.finalizePageExtraction();
  }

  private void extractSection(WikiSection section, boolean extractForeignData) {
    Optional<String> language = sectionLanguage(section);
    if (language.filter(l -> extractForeignData || "fr".equals(l)).isPresent()) {
      extractLanguageSection(section, language.get());
    } else {
      log.trace("TODO: extracting information from section {} in {}",
          section.getHeading().getText().trim(), getWiktionaryPageName());
    }
  }

  private void extractLanguageSection(WikiSection languageSection, String language) {
    // The language is always defined when arriving here
    wdh.initializeLanguageSection(getWiktionaryPageName(), language);

    for (Token t : languageSection.getContent().sections()) {
      WikiSection section = t.asWikiSection();
      Template title = sectionTitle(section);
      if (title != null && "S".equals(title.getName())) {
        String arg1 = title.getParsedArg("1");
        String pos;
        if ("étymologie".equals(arg1)) {
          // NOTHING YET
        } else if ((pos = posMarkers.get(arg1)) != null) {
          log.debug("Extracting LexicalEntry {} in {}", pos, getWiktionaryPageName());
          // } else if (ignorablePosMarkers.contains(arg1)) {
          // IGNORE
        } else {
          log.debug("Unexpected title {} in {}", title.getText(), getWiktionaryPageName());
        }
      }
    }
    wdh.finalizeLanguageSection();
  }

  private Template sectionTitle(WikiSection section) {
    List<Token> titleTemplate = section.getHeading().getContent().tokens().stream()
        .filter(t -> !(t instanceof Text && t.asText().getText().trim().equals("")))
        .collect(Collectors.toList());
    if (titleTemplate.size() == 0) {
      log.debug("Unexpected empty title in {}", getWiktionaryPageName());
      return null;
    }
    if (titleTemplate.size() > 1) {
      log.debug("Unexpected multi title {} in {}", section.getHeading().getText(),
          getWiktionaryPageName());
    }
    if (!(titleTemplate.get(0) instanceof Template)) {
      log.debug("Unexpected non template title {} in {}", section.getHeading().getText(),
          getWiktionaryPageName());
      return null;
    }
    return titleTemplate.get(0).asTemplate();
  }

  private Optional<String> sectionLanguage(WikiSection section) {
    if (section.getHeading().getLevel() == 2) {
      return section.getHeading().getContent().templatesOnUpperLevel().stream()
          .map(Token::asTemplate).filter(t -> "langue".equals(t.getName()))
          .map(t -> t.getParsedArg("1")).findFirst();
    }
    return Optional.empty();
  }

  private boolean isCharacterDescription(WikiSection section) {
    if (section.getHeading().getLevel() == 2) {
      return section.getHeading().getContent().templatesOnUpperLevel().stream()
          .map(Token::asTemplate).anyMatch(t -> "caractère".equals(t.getName()));
    }
    return false;
  }

  protected void extractDataOld(boolean extractForeignData) {
    wdh.initializePageExtraction(getWiktionaryPageName());
    Matcher languageFilter = languageSectionPattern.matcher(pageContent);
    int startSection = -1;

    // exampleExpander = new ExampleExpanderWikiModel(wi, frLocale, this.wiktionaryPageName, "");
    exampleExpander.setPageName(this.getWiktionaryPageName());

    String nextLang = null, lang = null;

    while (languageFilter.find()) {
      nextLang = getLanguageInHeader(languageFilter);
      extractData(startSection, languageFilter.start(), lang, extractForeignData);
      lang = nextLang;
      startSection = languageFilter.end();
    }

    // Either the filter is at end of sequence or on French language header.
    if (languageFilter.hitEnd()) {
      extractData(startSection, pageContent.length(), lang, extractForeignData);
    }
    wdh.finalizePageExtraction();
  }

  protected void extractData(int startOffset, int endOffset, String lang,
      boolean extractForeignData) {
    if (lang == null) {
      return;
    }

    if (extractForeignData) {
      if ("fr".equals(lang)) {
        return;
      }

      wdh.initializeLanguageSection(getWiktionaryPageName(), lang);
    } else {
      if (!"fr".equals(lang)) {
        return;
      }

      wdh.initializeLanguageSection(getWiktionaryPageName());
    }
    Matcher m = WikiPatterns.macroPattern.matcher(pageContent);
    m.region(startOffset, endOffset);

    log.trace("Extracting page \t{}", this.getWiktionaryPageName());

    // WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are
    // not macro based ?
    // DONE: (priority: top) link the definition node with the current Part of Speech
    // DONE: handle alternative spelling

    currentBlock = Block.NOBLOCK;

    while (m.find()) {
      // Iterate until we find a new section
      if (m.group(1).equals("S")) {
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
    }

    // Finalize the entry parsing
    leaveCurrentBlock(m);

    wdh.finalizeLanguageSection();
  }

  private Block computeNextBlock(Matcher m, Map<String, Object> context) {
    Map<String, String> sectionArgs = WikiTool.parseArgs(m.group(2));
    String sectionTitle = sectionArgs.get("1");
    String pos, nym;
    context.put("start", m.end());
    context.put("sectionTitleStart", m.start());

    if (sectionTitle != null) {
      if (ignorablePosMarkers.contains(sectionTitle)) {
        return Block.IGNOREPOS;
      } else if ((pos = posMarkers.get(sectionTitle)) != null) {
        context.put("pos", pos);
        if ("flexion".equals(sectionArgs.get("3"))) {
          context.put("lang", LangTools.normalize(sectionArgs.get("2")));
          return Block.INFLECTIONBLOCK;
        } else {
          return Block.DEFBLOCK;
        }
      } else if (isTranslation(m, sectionTitle)) {
        return Block.TRADBLOCK;
      } else if (isAlternate(m, sectionTitle)) {
        return Block.ORTHOALTBLOCK;
      } else if (null != (nym = getNymHeader(m, sectionTitle))) {
        context.put("nym", nym);
        return Block.NYMBLOCK;
      } else if (isValidSection(m, sectionTitle)) {
        return Block.NOBLOCK;
      } else {
        log.debug("Invalid section title {} in {}", sectionTitle, this.getWiktionaryPageName());
        return Block.NOBLOCK;
      }
    } else {
      log.debug("Null section title in {}", sectionTitle, this.getWiktionaryPageName());
      return Block.NOBLOCK;
    }
  }


  private void gotoNextBlock(Block nextBlock, HashMap<String, Object> context) {
    currentBlock = nextBlock;
    Object start = context.get("start");
    blockStart = (null == start) ? -1 : (int) start;
    Object titleStart = context.get("sectionTitleStart");
    blocktitleStart = (null == titleStart) ? -1 : (int) titleStart;

    switch (nextBlock) {
      case NOBLOCK:
      case IGNOREPOS:
        break;
      case INFLECTIONBLOCK:
        break;
      case DEFBLOCK:

        String pos = (String) context.get("pos");
        wdh.initializeLexicalEntry(pos);
        if ("-verb-".equals(pos)) {
          wdh.registerPropertyOnCanonicalForm(LexinfoOnt.verbFormMood, LexinfoOnt.infinitive);
        }
        break;
      case TRADBLOCK:
        break;
      case ORTHOALTBLOCK:
        break;
      case NYMBLOCK:
        currentNym = (String) context.get("nym");
        break;
      default:
        assert false : "Unexpected block while ending extraction of entry: "
            + getWiktionaryPageName();
    }

  }

  private void leaveCurrentBlock(Matcher m) {
    if (blockStart == -1) {
      return;
    }

    int end = computeRegionEnd(blockStart, m);

    log.trace("Leaving block {} while parsing entry {}", currentBlock.name(),
        this.getWiktionaryPageName());
    switch (currentBlock) {
      case NOBLOCK:
      case IGNOREPOS:
        break;
      case INFLECTIONBLOCK:
        // TODO : check if extracting inflections from form pages gives additional information.
        // TODO: currently extracting inflections is the only way to get the inflected past
        // participle forms
        // We keep the section title for the inflection extraction.
        extractInflections(blocktitleStart, end);
        break;
      case DEFBLOCK:
        List<String> morphologicalFeatures = extractMorphologicalData(blockStart, end);
        extractConjugationPage(morphologicalFeatures);
        extractDefinitions(blockStart, end);
        extractPronunciation(blockStart, end);
        extractOtherForms(blockStart, end, morphologicalFeatures);
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
        assert false : "Unexpected block while ending extraction of entry: "
            + getWiktionaryPageName();
    }

    blockStart = -1;
    blocktitleStart = -1;
  }

  private void extractConjugationPage(List<String> context) {
    Resource pos = wdh.currentLexinfoPos();
    if (null != pos && pos.equals(LexinfoOnt.verb)) {
      verbalInflectionExtractor.extractConjugations(context);
    }
  }

  /**
   * Extract inflections from "pseudo" PoS sections, like "Forme de Verbe", etc.
   *
   * This information is redundant with the original description of inflected forms that is supposed
   * to be present in the original lexical entry.
   *
   * SHOULD WE KEEP EXTRACTING THESE ???
   *
   * This is the only place where we find inflected forms of past participle ?
   *
   * @param blockStart
   * @param end
   */
  private void extractInflections(int blockStart, int end) {
    String source = pageContent.substring(blockStart, end);
    WikiText inflectionSection = new WikiText(source);

    String pronunciation = inflectionSection.templatesOnUpperLevel().stream().map(Token::asTemplate)
        .filter(t -> t.getName().equals("pron")).findFirst()
        .map(t -> t.getParsedArgs().get("1").trim()).orElse(null);

    Pair<String, String> langAndPoS = inflectionSection.templatesOnUpperLevel().stream()
        .map(Token::asTemplate).filter(t -> t.getName().equals("S")).findFirst()
        .map(t -> new ImmutablePair<>(t.getParsedArgs().get("2").trim(),
            posMarkers.get(t.getParsedArgs().get("1").trim())))
        .orElse(null);

    ClassBasedFilter filter = new ClassBasedFilter();
    filter.denyAll().allowIndentedItem(); // Only parse indented item
    List<Pair<InternalLink, LexicalForm>> forms =
        inflectionSection.filteredTokens(filter).stream().map(Token::asIndentedItem)
            .flatMap(ident -> FrenchExtractorWikiModel.getOtherForms(ident, pronunciation))
            .collect(Collectors.toList());
    forms.forEach(pair -> {
      pair.getRight().addValue(
          new WrittenRepresentation(wdh.currentLexEntry(), wdh.getCurrentEntryLanguage()));
      if (null != pronunciation && pronunciation.length() > 0)
        pair.getRight().addValue(
            new PhoneticRepresentation(pronunciation, wdh.getCurrentEntryLanguage() + "-fonipa"));
    });

    forms.forEach(pair -> frwdh.registerInflection(pair.getRight(), pair.getLeft().getTargetText(),
        langAndPoS.getLeft(), langAndPoS.getRight()));
  }

  private boolean isValidSection(Matcher m, String sectionTitle) {
    return sectionTitle != null || sectionMarkers.contains(m.group(1));
  }

  protected boolean isTranslation(Matcher m, String sectionTitle) {
    if (sectionTitle != null) {
      return sectionTitle.startsWith("trad");
    }
    return m.group(1).equals("-trad-");
  }

  private static Set<String> variantSections = new HashSet<>();

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

  private boolean isAlternate(Matcher m, String sectionTitle) {
    if (sectionTitle != null) {
      return variantSections.contains(sectionTitle);
    }

    return m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-");
  }


  private String getNymHeader(Matcher m, String sectionTitle) {
    if (sectionTitle != null) {
      return nymMarkerToNymName.get(sectionTitle);
    }

    return nymMarkerToNymName.get(m.group(1));
  }


  private static String translationTokenizer = "(?<ITALICS>'{2,3}.*?'{2,3})|"
      + "(?<PARENS>\\(\\P{Reserved}*?\\))|" + "(?<SPECIALPARENS>\\(.*?\\))|"
      + "(?<TMPL>\\p{Template})|" + "(?<LINK>\\p{InternalLink})";

  private static final Pattern tokenizer = WikiPattern.compile(translationTokenizer); // match all
                                                                                      // templates

  private void extractTranslations(int startOffset, int endOffset) {
    // log.debug("Translation section: " + pageContent.substring(startOffset, endOffset));

    WikiText text = new WikiText(getWiktionaryPageName(), pageContent, startOffset, endOffset);
    WikiCharSequence line = new WikiCharSequence(text);

    Matcher lexer = tokenizer.matcher(line);
    Resource currentGloss = null;
    int rank = 1;

    while (lexer.find()) {

      String g;
      if (null != (g = lexer.group("ITALICS"))) {
        // TODO: keep as usage and add current translation object when finding a comma
        log.debug("Found italics | {} | in translation for {}", g, getWiktionaryPageName());
      } else if (null != (g = lexer.group("PARENS"))) {
        // TODO: keep as usage and add current translation object when finding a comma
        log.debug("Found parenthesis | {} | in translation for {}", g, getWiktionaryPageName());
      } else if (null != (g = lexer.group("SPECIALPARENS"))) {
        log.debug("Template or link inside parens: | {} | for [ {} ]",
            line.getSourceContent(lexer.group("SPECIALPARENS")), getWiktionaryPageName());
        // TODO: some are only additional usage notes, other are alternate translation, decide
        // between them and handle the translation cases.
      } else if (null != (g = lexer.group("LINK"))) {
        log.debug("Translation as link : {}", line.getToken(lexer.group("LINK")));
      } else if (null != (g = lexer.group("TMPL"))) {
        WikiText.Template t = (WikiText.Template) line.getToken(g);
        String tname = t.getName();
        Map<String, String> args = t.getParsedArgs();

        switch (tname) {
          case "trad+":
          case "trad-":
          case "trad":
          case "t+":
          case "t-":
          case "trad--":
            String lang = LangTools.normalize(args.remove("1"));
            String word = args.remove("2");
            args.remove("nocat");
            String usage = null;
            if (args.size() > 0) {
              usage = args.toString(); // get all remaining arguments as usages
              usage = usage.substring(1, usage.length() - 1);
            }
            lang = FrenchLangtoCode.threeLettersCode(lang);

            if (lang != null && word != null) {
              wdh.registerTranslation(lang, currentGloss, usage, word);
            }

            break;
          case "boîte début":
          case "trad-début":
          case "(":
            // Get the glose that should help disambiguate the source acception
            String g1 = args.get("1");
            String g2 = args.get("2");
            args.remove("1");
            args.remove("2");
            if (args.size() > 0) {
              log.debug("unused args in translation gloss : {}", args);
            }
            String gloss = null;
            if (g1 != null || g2 != null) {
              gloss = (g1 == null || g1.equals("") ? "" : g1)
                  + (g2 == null || g2.equals("") ? "" : "|" + g2);
            }
            glossExtractor.setPageName(getWiktionaryPageName());
            if (null != gloss) {
              gloss = glossExtractor.expandAll(gloss, null);
            }
            currentGloss =
                wdh.createGlossResource(glossFilter.extractGlossStructure(gloss), rank++);

            break;
          case "trad-fin":
          case ")":
            currentGloss = null;
            break;

          case "-":
          case "T":
          default:
            break;
        }
      }
    }
  }

  protected void extractPronunciation(int startOffset, int endOffset) {
    extractPronunciation(startOffset, endOffset, true);
  }

  private HashSet<PronunciationPair> extractPronunciation(int startOffset, int endOffset,
      boolean registerPronunciation) {
    Matcher pronMatcher = pronunciationPattern.matcher(pageContent);
    pronMatcher.region(startOffset, endOffset);

    lastExtractedPronunciationLang = null;

    // TODO [URGENT]: what is this registerPronounciation boolean ?
    HashSet<PronunciationPair> res = registerPronunciation ? null : new HashSet<>();

    while (pronMatcher.find()) {
      String pron = pronMatcher.group(1);
      String lang = pronMatcher.group(2);

      if (pron == null || pron.equals("")) {
        return null;
      }
      // TODO [URGENT]: check when language is not present and display log debug information
      if (lang == null || lang.equals("")) {
        return null;
      }

      if (pron.startsWith("1=")) {
        pron = pron.substring(2);
      }

      if (lang.startsWith("|2=")) {
        lang = lang.substring(3);
      } else if (lang.startsWith("|lang=")) {
        lang = lang.substring(6);
      } else if (lang.startsWith("lang=")) {
        lang = lang.substring(5);
      }

      lang = LangTools.getPart1OrId(lang.trim());

      lastExtractedPronunciationLang = lang;

      if (lang != null && !lang.equals("") && !pron.equals("")) {
        if (registerPronunciation) {
          wdh.registerPronunciation(pron, lang + "-fonipa");
        } else {
          res.add(new PronunciationPair(pron, lang + "-fonipa"));
        }
      }
    }

    return res;
  }

  // static Pattern conjugationGroup =
  // Pattern.compile("\\{\\{conjugaison\\|fr\\|groupe=(\\d)\\}\\}");

  /**
   * Extracts morphological information on the lexical entry itself.
   * 
   * @return
   */
  private List<String> extractMorphologicalData(int blockStart, int end) {
    List<String> context = new ArrayList<>();
    String block = pageContent.substring(blockStart, end);

    if (block.matches("[\\s\\S]*\\{\\{m\\}\\}(?! *:)[\\s\\S]*")) {
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.masculine);
      context.add("Masculin");
    }

    if (block.matches("[\\s\\S]*\\{\\{f\\}\\}(?! *:)[\\s\\S]*")) {
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.feminine);
      context.add("Féminin");
    }

    if (block.matches("[\\s\\S]*\\{\\{mf\\}\\}(?! *:)[\\s\\S]*")) {
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.masculine);
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.feminine);
      context.add("Masculin et féminin identiques");
    }

    if (block.matches("[\\s\\S]*\\{\\{plurale tantum|fr\\}\\}(?! *:)[\\s\\S]*")) {
      // plural-only word
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.number, LexinfoOnt.plural);
      context.add("Pluriel");
    }

    // Pour les verbes :
    // Transitivité
    // {{t}}
    // {{i}}
    // Autres
    // {{impersonnel}}
    // {{prnl}} /!\ Ne pas confondre avec {{pronl}} (contexte) /!\

    return context;
  }

  public void extractExample(String example) {
    Map<Property, String> context = new HashMap<>();

    String ex = exampleExpander.expandExample(example, defTemplates, context);
    Resource exampleNode = null;
    if (ex != null && !ex.equals("")) {
      exampleNode = wdh.registerExample(ex, context);
    }
  }

  private void extractOtherForms(int start, int end, List<String> context) {
    if (wdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
      return;
    }
    // In French Wiktionary, many adjective "main entry" (the lemma) is marked as masculine
    // so remove any genre from adjective otherForm extraction.
    if ("-adj-".equals(wdh.currentWiktionaryPos())) {
      context.removeIf("Masculin"::equals);
      context.removeIf("Féminin"::equals);
    }

    Matcher otherFormMatcher = otherFormPattern.matcher(pageContent);
    otherFormMatcher.region(start, end);

    while (otherFormMatcher.find()) {
      String templateCall = otherFormMatcher.group();
      if (templateCall.startsWith("{{fr-verbe"))
        continue;
      morphologyExtractor.parseOtherForm(otherFormMatcher.group(), context);
    }
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    // TODO: properly handle macros in definitions.
    // definitionExpander.setPageName(this.getWiktionaryPageName());
    definitionExpander.parseDefinition(definition, defLevel);
  }

}
