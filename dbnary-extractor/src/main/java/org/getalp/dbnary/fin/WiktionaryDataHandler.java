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
    posAndTypeValueMap
        .put("Substantiivi", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap
        .put("Adjektiivi", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Verbi", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Adverbi", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap
        .put("Erisnimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("subs", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("verbi", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));

  }

  public WiktionaryDataHandler(String lang) {
    super(lang);
  }

}
