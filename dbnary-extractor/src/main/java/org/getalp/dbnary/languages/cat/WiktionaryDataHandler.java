package org.getalp.dbnary.languages.cat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.*;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  private static final HashMap<String, String> nymRelation = new HashMap<>();
  private static final HashMap<String, String> pronCodeRelation = new HashMap<>();

  static {
    pronCodeRelation.put("balear", "ca-u-sd-esib");
    pronCodeRelation.put("valencià", "ca-valencia");
    pronCodeRelation.put("septentrional", "ca-FR");
    pronCodeRelation.put("alguerès", "ca-IT");
    pronCodeRelation.put("central", "ca");
    pronCodeRelation.put("nord-occidental", "ca");
    pronCodeRelation.put("local", "ca");
    pronCodeRelation.put("barceloní", "ca");
    pronCodeRelation.put("ribagorçà", "ca");

    pronCodeRelation.put("centr.", pronCodeRelation.get("central"));
    pronCodeRelation.put("nord-occ.", pronCodeRelation.get("nord-occidental"));
    pronCodeRelation.put("mallorquí", pronCodeRelation.get("balear"));
    pronCodeRelation.put("Oriental", pronCodeRelation.get("central"));
    pronCodeRelation.put("oriental", pronCodeRelation.get("central"));
    pronCodeRelation.put("occidental", pronCodeRelation.get("nord-occidental"));
    pronCodeRelation.put("Occidental", pronCodeRelation.get("nord-occidental"));

    nymRelation.put("sin", "syn");
    nymRelation.put("ant", "ant");
    nymRelation.put("hipo", "hypo");
    nymRelation.put("hiper", "hyper");
    nymRelation.put("mero", "mero");
    nymRelation.put("holo", "holo");
    nymRelation.put("paro", "qsyn");

    nymRelation.put("Antònims", "ant");
    nymRelation.put("Sinònims", "syn");
    nymRelation.put("Hipònims", "hypo");
    nymRelation.put("Hiperònims", "hyper");
    nymRelation.put("Parònims", "qsyn");

    posAndTypeValueMap.put("Nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom-1", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom-2", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom1", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom2", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Verb 1", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Verb 2", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Verb-1", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Verb-2", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Verbs", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjectiu", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjectiu-1", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjectiu-2", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("Adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjetiu", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("Sigles", new PosAndType(LexinfoOnt.acronym, OntolexOnt.Word));
    posAndTypeValueMap.put("Adverbi", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjective", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom propi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom Propi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Nom prompi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.number, OntolexOnt.Word));
    posAndTypeValueMap.put("Conjunció", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("Abreviatura", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("Lletra", new PosAndType(LexinfoOnt.letter, OntolexOnt.Word));
    posAndTypeValueMap.put("Preposició", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("Interjecció", new PosAndType(LexinfoOnt.Interjection, OntolexOnt.Word));
    posAndTypeValueMap.put("Pronom", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Article", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
    posAndTypeValueMap.put("Contracció", new PosAndType(LexinfoOnt.contraction, OntolexOnt.Word));
    posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Sufix", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.infix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Símbol", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));
    posAndTypeValueMap.put("Forma verbal", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("Frase feta",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Desinència", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Acrònim", new PosAndType(LexinfoOnt.acronym, OntolexOnt.Word));
    posAndTypeValueMap.put("Proverbi",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Transliteració",
        new PosAndType(LexinfoOnt.transliteration, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Lletres", new PosAndType(LexinfoOnt.letter, OntolexOnt.Word));
    posAndTypeValueMap.put("Determinant", new PosAndType(LexinfoOnt.determiner, OntolexOnt.Word));
    posAndTypeValueMap.put("Caràcter", new PosAndType(LexinfoOnt.symbol, OntolexOnt.Word));
    posAndTypeValueMap.put("Posposició", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("Adjectiu numeral",
        new PosAndType(LexinfoOnt.genericNumeral, OntolexOnt.Word));
    posAndTypeValueMap.put("Abreviacions",
        new PosAndType(LexinfoOnt.AbbreviatedForm, OntolexOnt.Word));
    posAndTypeValueMap.put("Pronom relatiu",
        new PosAndType(LexinfoOnt.relativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Pronom indefinit",
        new PosAndType(LexinfoOnt.indefinitePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Postposició", new PosAndType(LexinfoOnt.postposition, OntolexOnt.Word));
    posAndTypeValueMap.put("Prenom", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("Forma Nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));


  }


  public WiktionaryDataHandler(String longEditionLanguageCode, String tdbDir) {
    super(longEditionLanguageCode, tdbDir);
  }

  @Override
  public void registerNymRelation(String target, String synRelation) {
    super.registerNymRelation(target, nymRelation.get(synRelation));
  }

  @Override
  public void initializeLexicalEntry(String title) {

    if (posAndTypeValueMap.get(title) == null)
      log.warn("UNHANDLED LEXICAL TYPE : " + title + " name -> " + this.currentPage.getName());

    PosAndType pat = posAndTypeValueMap.get(title);
    Resource typeR = typeResource(pat);
    initializeLexicalEntry(title, posResource(pat), typeR);
  }

  public void addLexicalForm(LexicalForm form) {
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox)
      return;

    form.attachTo(currentLexEntry.inModel(morphoBox));
  }

  public void registerVerbForm(final String value) {
    if (value == null)
      return;

    if (value.contains("t"))
      aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasValency, OliaOnt.Transitive));
    if (value.contains("i"))
      aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasValency, OliaOnt.Intransitive));

    /*
     * TODO if "a" is in the value, the currentLexEntry is an Auxilliar. if (value.contains("a"))
     * aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasValency, OliaOnt.AuxiliaryVerb));
     */
  }

  final static String SENSE_NUM_PREFIX = "^\\[(.+)\\]\\s*:\\s*(.*)$";
  final static Pattern senseNumPattern = Pattern.compile(SENSE_NUM_PREFIX);

  public void registerDerivedForms(String value) {
    if (currentLexEntry == null)
      return;
    Matcher m = senseNumPattern.matcher(value);
    if (m.matches()) {
      log.warn("Unhandled sense num in {} : {}", currentPagename(), m.group(1));
      value = m.group(2);
    }

    registerDerivedForm(value.split(","));
  }

  public void registerDerivedForm(final String... values) {
    if (currentLexEntry == null)
      return;

    for (String val : values) {
      Resource target = getPageResource(val.trim());
      aBox.add(target, DBnaryOnt.derivedFrom, currentLexEntry);
      aBox.add(aBox.createStatement(target, DBnaryOnt.derivedFrom, currentLexEntry));
    }
  }

  public void registerPron(final ArrayList<WiktionaryExtractor.PronBuilder> prons) {
    String code;
    for (WiktionaryExtractor.PronBuilder pron : prons) {

      code = pronCodeRelation.get(pron.loc);

      if (pron.loc.equals("root"))
        code = this.getCurrentEntryLanguage();

      if (code == null) {
        log.trace("{} => Unhandled loc (don't worry) -> {}", currentPage.getName(), pron.loc);
        code = this.getCurrentEntryLanguage();
      }

      this.registerPronunciation(pron.pron, code + "-fonipa");

    }
  }

}
