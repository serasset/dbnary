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
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.fra.morphology.FrenchInflectionDecoder;
import org.getalp.dbnary.fra.morphology.InflectionExtractorWikiModel;
import org.getalp.dbnary.fra.morphology.VerbalInflexionExtractorWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPattern;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.PhoneticRepresentation;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String pronunciationPatternString = "\\{\\{pron\\|([^|}]*)\\|([^}]*)}}";

  protected final static String otherFormPatternString = "\\{\\{fr-[^}]*}}";

  private static final HashMap<String, String> posMarkers;
  private static final HashSet<String> ignoredSectionTitles;
  private static final HashSet<String> ignoredPosMarkers;

  private final static HashMap<String, String> nymMarkerToNymName;

  private static void addPos(String pos) {
    posMarkers.put(pos, pos);
  }

  private static void addPos(String p, String n) {
    posMarkers.put(p, n);
  }

  static {

    posMarkers = new HashMap<>(130);

    addPos("-déf-");
    addPos("-déf-/2");
    addPos("-déf2-");
    addPos("--");
    addPos("-adj-");
    addPos("-adj-/2");
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

    ignoredSectionTitles = new HashSet<>();
    ignoredPosMarkers = new HashSet<>();

    // IGNORED SECTIONS
    ignoredPosMarkers.add("lettre");
    ignoredPosMarkers.add("symbole");
    ignoredPosMarkers.add("symb");

    ignoredSectionTitles.add("références");
    ignoredSectionTitles.add("anagrammes");
    ignoredSectionTitles.add("paronymes");
    ignoredSectionTitles.add("prononciation");
    ignoredSectionTitles.add("références");
    ignoredSectionTitles.add("voir aussi");
    ignoredSectionTitles.add("voir");



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

    // TODO trouver tous les modèles de section...

    // affixesToDiscardFromLinks = new HashSet<String>();
    // affixesToDiscardFromLinks.add("s");
  }

  protected final static Pattern pronunciationPattern;
  protected final static Pattern otherFormPattern;

  static {
    pronunciationPattern = Pattern.compile(pronunciationPatternString);
    otherFormPattern = Pattern.compile(otherFormPatternString);
  }

  WiktionaryDataHandler frwdh;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    frwdh = (WiktionaryDataHandler) wdh;
  }

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

  @Override
  public void extractData() {
    frwdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    WikiDocument doc = page.asStructuredDocument();
    doc.getContent().wikiTokens().stream().filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection).forEach(wikiSection -> extractSection(wikiSection));
    frwdh.finalizePageExtraction();
  }

  private void extractSection(WikiSection section) {
    Optional<String> language = sectionLanguage(section);
    language.ifPresent(l -> extractLanguageSection(section, l));
  }

  private void extractLanguageSection(WikiSection languageSection, String language) {
    if (null == language)
      return;
    if (wdh.isDisabled(ExtractionFeature.FOREIGN_LANGUAGES) && !"fr".equals(language))
      return;

    // The language is always defined when arriving here
    wdh.initializeLanguageSection(language);

    for (Token t : languageSection.getContent().sections()) {
      WikiSection section = t.asWikiSection();
      Pair<Template, String> titleTemplateAndSection = sectionTitle(section);
      Template title = titleTemplateAndSection.getLeft();
      String sectionName = titleTemplateAndSection.getRight();
      String pos;
      if ("étymologie".equals(sectionName)) {
        // NOTHING YET
      } else if ((pos = posMarkers.get(sectionName)) != null) {
        if (title != null && "flexion".equals(title.getParsedArg("3"))) {
          extractInflections(section);
          // There are several entries with misplaced sub section (e.g. translations given after
          // an inflexions section). Try to extract data from these misplaced subsections.
          handleLexicalEntrySubSections(section);
        } else {
          extractLexicalEntry(section, pos);
        }
      } else if (ignoredPosMarkers.contains(sectionName)) {
        log.trace("Ignoring part of speech {} in {}",
            title == null ? sectionName : title.getText());
        // IGNORE
      } else if (ignoredSectionTitles.contains(sectionName)) {
        log.trace("Ignoring section {} in {}", title == null ? sectionName : title.getText());
        // There are several entries with misplaced sub section (e.g. translations given after
        // reference section). Try to extract data from these misplaced subsections.
        handleLexicalEntrySubSections(section);
        // IGNORE
      } else {
        log.debug("Unexpected title {} in {}", title == null ? sectionName : title.getText(),
            getWiktionaryPageName());
        // There are several entries with misplaced sub section (e.g. translations given after
        // reference section). Try to extract data from these misplaced subsections.
        handleLexicalEntrySubSections(section);
      }
    }
    wdh.finalizeLanguageSection();
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
   * @param section the wiki text section corresponding to an inflected forms definitions.
   */
  private void extractInflections(WikiSection section) {
    WikiContent content = section.getPrologue();
    WikiContent heading = section.getHeading().getContent();

    Pair<String, String> langAndPoS = heading.templatesOnUpperLevel().stream()
        .map(Token::asTemplate).filter(t -> t.getName().equals("S")).findFirst()
        .map(t -> new ImmutablePair<>(t.getParsedArgs().get("2").trim(),
            posMarkers.get(t.getParsedArgs().get("1").trim().toLowerCase())))
        .orElse(null);

    // Only get inflections for verbs so that we capture the missing inflected past participles.
    if (!langAndPoS.getRight().equals("-verb-"))
      return;

    String pronunciation = content.templatesOnUpperLevel().stream().map(Token::asTemplate)
        .filter(t -> t.getName().equals("pron")).findFirst()
        .map(t -> t.getParsedArgs().get("1").trim()).orElse(null);

    ClassBasedFilter filter = new ClassBasedFilter();
    filter.denyAll().allowIndentedItem(); // Only parse indented item
    List<Pair<InternalLink, LexicalForm>> forms =
        content.filteredTokens(filter).stream().map(Token::asIndentedItem)
            .flatMap(ident -> FrenchInflectionDecoder.getOtherForms(ident, pronunciation))
            .collect(Collectors.toList());
    forms.forEach(pair -> {
      pair.getRight().addValue(
          new WrittenRepresentation(wdh.currentLexEntry(), wdh.getCurrentEntryLanguage()));
      if (null != pronunciation && pronunciation.length() > 0)
        pair.getRight()
            .addValue(new PhoneticRepresentation(pronunciation, wdh.getCurrentEntryLanguage()));
    });

    forms.forEach(pair -> frwdh.registerInflection(pair.getRight(), pair.getLeft().getTargetText(),
        langAndPoS.getLeft(), langAndPoS.getRight()));
  }

  /**
   * Extract the lexical entry that is described in the section.
   * 
   * @param section
   * @param pos the (standardised) part of speech described by the section
   */
  private void extractLexicalEntry(WikiSection section, String pos) {
    log.debug("Extracting LexicalEntry {} in {}", pos, getWiktionaryPageName());
    wdh.initializeLexicalEntry(pos);

    if ("-verb-".equals(pos)) {
      wdh.registerPropertyOnCanonicalForm(LexinfoOnt.verbFormMood, LexinfoOnt.infinitive);
    }
    // First we extract morphological features, conjugations, definitions, etc. that are described
    // BEFORE other subsections.
    int blockStart = section.getHeading().getBeginIndex();
    int end = section.getPrologue().getEndIndex();
    List<String> morphologicalFeatures = extractMorphologicalData(blockStart, end);
    extractConjugationPage(morphologicalFeatures);
    extractDefinitions(blockStart, end);
    extractPronunciation(section.getPrologue());
    extractOtherForms(section.getPrologue(), morphologicalFeatures);

    // Then, we extract sub sections
    handleLexicalEntrySubSections(section);
  }

  private void handleLexicalEntrySubSections(WikiSection section) {
    for (Token t : section.getContent().sections()) {
      WikiSection subsection = t.asWikiSection();
      Pair<Template, String> titleTemplateAndSection = sectionTitle(subsection);
      Template title = titleTemplateAndSection.getLeft();
      String sectionName = titleTemplateAndSection.getRight();
      String nym;
      if (sectionName.startsWith("trad")) {
        extractTranslations(subsection.getPrologue());
      } else if (variantSections.contains(sectionName)) {
        extractOrthoAlt(subsection.getContent().getBeginIndex(),
            subsection.getPrologue().getEndIndex());
      } else if (null != (nym = nymMarkerToNymName.get(sectionName))) {
        if (log.isDebugEnabled()
            && section.getContent().sections().stream().findFirst().isPresent())
          log.debug("Subsection under nym section in {}", getWiktionaryPageName());
        extractNyms(nym, subsection.getContent().getBeginIndex(),
            subsection.getPrologue().getEndIndex());
      } else {
        log.debug("Unexpected sub section title {} in {}",
            title == null ? sectionName : title.getText(), getWiktionaryPageName());
      }
      // There may be a level 5 subsubsection named "traductions à trier" that contain more
      // translations. This subsubsection is usually present at the end of the translations section,
      // but it may appear alone, inside any other subsection. We need to handle this specific
      // translations section separately.
      subsection.getContent().sections().stream().map(Token::asWikiSection)
          .filter(s -> sectionTitle(s).getRight().startsWith("trad"))
          .forEach(s -> extractTranslations(s.getPrologue()));
    }
  }

  private Pair<Template, String> sectionTitle(WikiSection section) {
    List<Token> titleTemplate = section.getHeading().getContent().tokens().stream()
        .filter(t -> !(t instanceof Text
            && t.asText().getText().replaceAll("\u00A0", "").trim().equals("")))
        .collect(Collectors.toList());
    if (titleTemplate.size() == 0) {
      log.debug("Unexpected empty title in {}", getWiktionaryPageName());
      return new ImmutablePair<>(null, "");
    }
    if (titleTemplate.size() > 1) {
      log.debug("Unexpected multi title {} in {}", section.getHeading().getText(),
          getWiktionaryPageName());
    }
    if (!(titleTemplate.get(0) instanceof Template)) {
      log.debug("Unexpected non template title {} in {}", section.getHeading().getText(),
          getWiktionaryPageName());
      return new ImmutablePair<>(null,
          section.getHeading().getContent().getText().toLowerCase().trim());
    }
    String tname = titleTemplate.get(0).asTemplate().getName().trim();
    if (!"S".equals(tname)) {
      log.debug("Template title is not an S: {} in {}", section.getHeading().getText(),
          getWiktionaryPageName());
      return new ImmutablePair<>(titleTemplate.get(0).asTemplate(), tname);
    }

    return new ImmutablePair<>(titleTemplate.get(0).asTemplate(),
        titleTemplate.get(0).asTemplate().getParsedArg("1").toLowerCase().trim());
  }

  private Optional<String> sectionLanguage(WikiSection section) {
    if (section.getHeading().getLevel() == 2) {
      return section.getHeading().getContent().templatesOnUpperLevel().stream()
          .map(Token::asTemplate).filter(t -> "langue".equals(t.getName()))
          .map(t -> t.getParsedArg("1")).findFirst();
    }
    return Optional.empty();
  }

  private void extractConjugationPage(List<String> context) {
    Resource pos = wdh.currentLexinfoPos();
    if (null != pos && pos.equals(LexinfoOnt.verb)) {
      verbalInflectionExtractor.extractConjugations(context);
    }
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

  private static String translationTokenizer = "(?<ITALICS>'{2,3}.*?'{2,3})|"
      + "(?<PARENS>\\(\\P{Reserved}*?\\))|" + "(?<SPECIALPARENS>\\(.*?\\))|"
      + "(?<TMPL>\\p{Template})|" + "(?<LINK>\\p{InternalLink})";

  private static final Pattern tokenizer = WikiPattern.compile(translationTokenizer);

  private void extractTranslations(WikiContent content) {
    WikiCharSequence line = new WikiCharSequence(content);

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

  protected void extractPronunciation(WikiContent content) {
    content.templatesOnUpperLevel().stream().map(Token::asTemplate)
        .filter(t -> t.getName().equals("pron")).forEach(p -> {
          String pron = p.getParsedArgs().get("1");
          String lang = p.getParsedArgs().get("2");
          if (null == lang) {
            lang = p.getParsedArgs().get("lang");
          }
          if (null != lang) {
            lang = LangTools.getPart1OrId(lang.trim());
          }
          if (null == lang || lang.equals("")) {
            lang = wdh.getCurrentEntryLanguage();
          }
          if (null != pron && !(pron = pron.trim()).equals("")) {
            wdh.registerPronunciation(pron, lang + "-fonipa");
          }
        });
  }

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

  // TODO: all canonical forms are also available as otherForm, process other forms to remove
  // the one corresponding to canonical forms.
  private void extractOtherForms(WikiContent content, List<String> context) {
    if (wdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
      return;
    }
    // In French Wiktionary, many adjective "main entry" (the lemma) is marked as masculine
    // so remove any genre from adjective otherForm extraction.
    if ("-adj-".equals(wdh.currentWiktionaryPos())) {
      context.removeIf("Masculin"::equals);
      // I consider that an adjective which is marked as feminine is in fact defective.
      // context.removeIf("Féminin"::equals);
    }

    content.templatesOnUpperLevel().stream().map(Token::asTemplate)
        .filter(t -> t.getName().startsWith("fr-")).filter(t -> !t.getName().startsWith("fr-verbe"))
        .forEach(t -> morphologyExtractor.parseOtherForm(t.getText(), context));
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    // TODO: properly handle macros in definitions.
    // definitionExpander.setPageName(this.getWiktionaryPageName());
    definitionExpander.parseDefinition(definition, defLevel);
  }

}
