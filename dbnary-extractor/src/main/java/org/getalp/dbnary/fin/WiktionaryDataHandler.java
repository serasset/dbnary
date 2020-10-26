package org.getalp.dbnary.fin;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {

    // Finnish
    posAndTypeValueMap.put("Substantiivi",
        new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Adjektiivi",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Verbi", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Adverbi", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Erisnimi",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("subs", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("verbi", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Interjektio",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Postpositio",
        new PosAndType(LexinfoOnt.postposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Prepositio",
        new PosAndType(LexinfoOnt.preposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Partikkeli",
        new PosAndType(LexinfoOnt.particle, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Suffiksi", new PosAndType(LexinfoOnt.suffix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Prefiksi", new PosAndType(LexinfoOnt.prefix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Konjunktio",
        new PosAndType(LexinfoOnt.conjunction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Aakkonen", new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Kirjoitusmerkki",
        new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Numeraali",
        new PosAndType(LexinfoOnt.numeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Pronomini",
        new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Lyhenne",
        new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));

  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

}
