package org.getalp.dbnary.rus;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OliaOnt;
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

    // Greek
    posAndTypeValueMap.put("прил", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Фам", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("сущ", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("гл", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("мест", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("числ", new PosAndType(LexinfoOnt.numeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("прич",
        new PosAndType(LexinfoOnt.participleAdjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("деепр", new PosAndType(LexinfoOnt.participle, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("interj",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("suffix", new PosAndType(LexinfoOnt.suffix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("part", new PosAndType(LexinfoOnt.particle, OntolexOnt.LexicalEntry));
    // TODO: validate this by asking to Nexus Slack ?
    // posAndTypeValueMap.put("predic", new PosAndType(LexinfoOnt.predicativeNominative,
    // OntolexOnt.LexicalEntry));
    // posAndTypeValueMap.put("intro", new PosAndType(LexinfoOnt.?, OntolexOnt.LexicalEntry));

  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

}
