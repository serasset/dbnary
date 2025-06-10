package org.getalp.dbnary.languages.ces;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {
  static {
    posAndTypeValueMap.put("podstatné jméno", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("přídavné jméno", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("příslovce", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("sloveso", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("spojka", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("zájemno", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("zkratka", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("částice", new PosAndType(LexinfoOnt.particle, OntolexOnt.Word));
    posAndTypeValueMap.put("citoslovce", new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word));

    posAndTypeValueMap.put("přísloví",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("idiom",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("fráze",
        new PosAndType(LexinfoOnt.setPhrase, OntolexOnt.MultiWordExpression));
  }

  public boolean isPartOfSpeech(String s) {
    return posAndTypeValueMap.containsKey(s);
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }
}
