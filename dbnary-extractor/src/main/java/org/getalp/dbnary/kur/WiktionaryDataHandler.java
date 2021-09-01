package org.getalp.dbnary.kur;

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

    // DONE: Kurdish
    posAndTypeValueMap.put("Navdêr", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Serenav",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Rengdêr",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Lêker", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Hoker", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Cînav", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Baneşan",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Daçek", new PosAndType(LexinfoOnt.adposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Pêşdaçek",
        new PosAndType(LexinfoOnt.preposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Paşdaçek",
        new PosAndType(LexinfoOnt.postposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Bazinedaçek",
        new PosAndType(LexinfoOnt.circumposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Girêdek",
        new PosAndType(LexinfoOnt.conjunction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Artîkel", new PosAndType(LexinfoOnt.article, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Navgir", new PosAndType(LexinfoOnt.infix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Paşgir", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));
    posAndTypeValueMap.put("Pêşgir", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
    // These two means a root form but seems to correspond to a inflected form.
    // posAndTypeValueMap.put("Reh", new PosAndType(LexinfoOnt.???, OntolexOnt.LexicalEntry));
    // posAndTypeValueMap.put("Rehekî lêkerê", new PosAndType(LexinfoOnt.???, OntolexOnt.Affix));
    posAndTypeValueMap.put("Biwêj",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Hevok",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Gotineke pêşiyan",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Hejmar", new PosAndType(LexinfoOnt.numeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Tîp", new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Sembol", new PosAndType(LexinfoOnt.symbol, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Kurtenav",
        new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    pos = pos.trim();
    if (pos.contains("çekim") || pos.contains("çekilmiş")) {
      return; // ignore inflected forms
    }

    PosAndType pat = posAndTypeValueMap.get(pos);
    if (null == pat) {
      log.debug("Unknown POS : {} in {}", pos, currentWiktionaryPageName);
    }
    initializeLexicalEntry__noModel(pos, posResource(pat), typeResource(pat));
  }

}
