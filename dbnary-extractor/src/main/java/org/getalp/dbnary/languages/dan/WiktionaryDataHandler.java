package org.getalp.dbnary.languages.dan;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Property;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  static {

    // Finnish
    posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("noun2", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Substantiv", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Adjektiv",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Verbum", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("prop", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("subs", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    // This is an onomatopoeia, classify it as a noun ?
    posAndTypeValueMap.put("lyd", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("interj",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("post",
        new PosAndType(LexinfoOnt.postposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.LexicalEntry));
    // Rather: Prepositional Conjunction ?
    posAndTypeValueMap.put("pp", new PosAndType(LexinfoOnt.preposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("art", new PosAndType(LexinfoOnt.article, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("prov", new PosAndType(LexinfoOnt.proverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("frase", new PosAndType(LexinfoOnt.expression, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("part", new PosAndType(LexinfoOnt.particle, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("suf", new PosAndType(LexinfoOnt.suffix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("end", new PosAndType(LexinfoOnt.suffix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("pref", new PosAndType(LexinfoOnt.prefix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Aakkonen", new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("num", new PosAndType(LexinfoOnt.numeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("phr",
        new PosAndType(LexinfoOnt.phraseologicalUnit, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Kirjoitusmerkki",
        new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("car-num",
        new PosAndType(LexinfoOnt.cardinalNumeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("seq-num",
        new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("pron", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("pers-pronom",
        new PosAndType(LexinfoOnt.personalPronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("ubest-pronon",
        new PosAndType(LexinfoOnt.indefinitePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("dem-pronom",
        new PosAndType(LexinfoOnt.demonstrativePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("int-pronom",
        new PosAndType(LexinfoOnt.interrogativePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("rel-pronom",
        new PosAndType(LexinfoOnt.relativePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Possessivt Pronomen (Ejestedord)",
        new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Possessivt Pronomen",
        new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("abbr",
        new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("abr", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("acro", new PosAndType(LexinfoOnt.acronym, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("contr",
        new PosAndType(LexinfoOnt.contraction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("symb", new PosAndType(LexinfoOnt.symbol, OntolexOnt.LexicalEntry));

  }

  private final static Map<String, Property> nymMap = new HashMap<>();
  static {
    nymMap.put("syn", LexinfoOnt.synonym);
    nymMap.put("ant", LexinfoOnt.antonym);
    nymMap.put("hyper", LexinfoOnt.hypernym);
    nymMap.put("hypo", LexinfoOnt.hyponym);
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public boolean isPartOfSpeech(String name) {
    return posAndTypeValueMap.containsKey(name);
  }

  public boolean isNym(String name) {
    return nymMap.containsKey(name);
  }
}
