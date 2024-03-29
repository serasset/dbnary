package org.getalp.dbnary.languages.bul;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {

    posAndTypeValueMap.put("Съществително нарицателно име",
        new PosAndType(LexinfoOnt.noun, LexinfoOnt.Noun));
    posAndTypeValueMap.put("Съществително собствено име",
        new PosAndType(LexinfoOnt.properNoun, LexinfoOnt.ProperNoun));

    posAndTypeValueMap.put("Прилагателно име",
        new PosAndType(LexinfoOnt.adjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("Глагол", new PosAndType(LexinfoOnt.verb, LexinfoOnt.Verb));
    posAndTypeValueMap.put("Наречие", new PosAndType(LexinfoOnt.adverb, LexinfoOnt.Adverb));
    posAndTypeValueMap.put("Числително име",
        new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));

    posAndTypeValueMap.put("Частица", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));

    posAndTypeValueMap.put("Предлог",
        new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition));
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

}
