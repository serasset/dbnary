package org.getalp.dbnary.languages.gle;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;
import org.getalp.dbnary.*;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.iso639.ISO639_3;
import org.getalp.model.ontolex.LexicalForm;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {
  static {
    posAndTypeValueMap.put("-adjf-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-faid-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-int-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-adj-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-aid-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-nounf-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("-fainm-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("-propn-", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-ainmd-", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-pronoun-", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-for-", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-rfh-", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("-prep-", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("-noun-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("-ainm-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("-verb-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("-briath-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("-fbriath-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("-verbf-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("-dobh-", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("-adv-", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("-réim-", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Word));
    posAndTypeValueMap.put("-pfx-", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Word));
    posAndTypeValueMap.put("-forc-",
        new PosAndType(LexinfoOnt.interrogativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-intpr-",
        new PosAndType(LexinfoOnt.interrogativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-giorr-", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("-abbr-", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("-contr-", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("-uimh-", new PosAndType(LexinfoOnt.number, OntolexOnt.Word));
    posAndTypeValueMap.put("-num-", new PosAndType(LexinfoOnt.number, OntolexOnt.Word));
    posAndTypeValueMap.put("-inis-", new PosAndType(LexinfoOnt.initialism, OntolexOnt.Word));
    posAndTypeValueMap.put("-sym-", new PosAndType(LexinfoOnt.symbol, OntolexOnt.Word));
    posAndTypeValueMap.put("-aidsheal-",
        new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-possadj-",
        new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-alt-", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
    posAndTypeValueMap.put("-art-", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
    posAndTypeValueMap.put("-seanfh-",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("-conj-", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("-cón-", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("-mgm-", new PosAndType(LexinfoOnt.formalRegister, OntolexOnt.Word));
    posAndTypeValueMap.put("-mír-", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
  }

  public WiktionaryDataHandler(String longEditionLanguageCode, String tdbDir) {
    super(longEditionLanguageCode, tdbDir);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    PosAndType pat = posAndTypeValueMap.get(pos);
    Resource typeR = typeResource(pat);
    initializeLexicalEntry(pos, posResource(pat), typeR);
  }

  public void addLexicalForm(LexicalForm form) {
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox)
      return;

    form.attachTo(currentLexEntry.inModel(morphoBox));
  }
}
