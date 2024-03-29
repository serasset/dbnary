package org.getalp.dbnary.languages.swe;

import java.util.HashSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.PropertyObjectPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author malick
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {
    /*
     * Les lignes avec comme commentaire "fait" indique ce part of speech à été trouver dans certain
     * mots du swedois
     */
    // swedois
    posAndTypeValueMap.put("Substantiv", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry)); // fait
    posAndTypeValueMap.put("Proper noun",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Proper Noun",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));

    posAndTypeValueMap.put("Adjektiv",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry)); // fait
    posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry)); // fait
    posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry)); // fait
    posAndTypeValueMap.put("Artikel", new PosAndType(LexinfoOnt.article, LexinfoOnt.Article)); // fait
    posAndTypeValueMap.put("Konjunktion",
        new PosAndType(LexinfoOnt.conjunction, LexinfoOnt.Conjunction)); // fait
    posAndTypeValueMap.put("Determiner",
        new PosAndType(LexinfoOnt.determiner, LexinfoOnt.Determiner));

    posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("Cardinal numeral",
        new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("Cardinal number",
        new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));

    posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("Particle", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));
    posAndTypeValueMap.put("Preposition",
        new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition)); // fait
    posAndTypeValueMap.put("Postposition",
        new PosAndType(LexinfoOnt.postposition, LexinfoOnt.Postposition));

    posAndTypeValueMap.put("Prepositional phrase",
        new PosAndType(null, OntolexOnt.MultiWordExpression));

    posAndTypeValueMap.put("Pronomen", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun)); // fait
    posAndTypeValueMap.put("Symbol", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));

    posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.prefix, LexinfoOnt.Prefix));
    posAndTypeValueMap.put("Suffix", new PosAndType(LexinfoOnt.suffix, LexinfoOnt.Suffix));
    posAndTypeValueMap.put("Affix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
    posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.infix, LexinfoOnt.Infix));
    posAndTypeValueMap.put("Interfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
    posAndTypeValueMap.put("Circumfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));

    posAndTypeValueMap.put("Ordspråk",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression)); // fait
    posAndTypeValueMap.put("Interjektion",
        new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection)); // fait
    posAndTypeValueMap.put("Phrase",
        new PosAndType(LexinfoOnt.phraseologicalUnit, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Idiom",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));

    // ajouter
    posAndTypeValueMap.put("pronom-pers",
        new PosAndType(LexinfoOnt.personalPronoun, LexinfoOnt.Pronoun));

    // Initialism ?
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public static boolean isValidPOS(String pos) {
    return posAndTypeValueMap.containsKey(pos);
  }

  @Override
  protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    // Do not try to merge new form with an existing compatible one in English.
    // This would lead to a Past becoming a PastParticiple when registering the past participle
    // form.
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox) {
      return;
    }

    lexEntry = lexEntry.inModel(morphoBox);

    String otherFormNodeName = computeOtherFormResourceName(lexEntry, properties);
    Resource otherForm = morphoBox.createResource(getPrefix() + otherFormNodeName, OntolexOnt.Form);
    morphoBox.add(lexEntry, OntolexOnt.otherForm, otherForm);
    mergePropertiesIntoResource(properties, otherForm);

  }

}
