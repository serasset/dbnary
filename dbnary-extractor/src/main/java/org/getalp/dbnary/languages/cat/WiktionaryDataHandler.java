package org.getalp.dbnary.languages.cat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  private static final HashMap<String, String> nymRelation = new HashMap<>();

  static {
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

}
