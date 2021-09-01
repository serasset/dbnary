package org.getalp.dbnary.tur;

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

    // Turkish
    posAndTypeValueMap.put("Ad", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Adıl", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Atasözü",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Bağlaç",
        new PosAndType(LexinfoOnt.conjunction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Belirteç", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Deyim", new PosAndType(LexinfoOnt.idiom, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Emir", new PosAndType(LexinfoOnt.imperative, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Erkek adı",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry)); // Male name ?
    posAndTypeValueMap.put("Erkek ismi",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry)); // Male name ?
    posAndTypeValueMap.put("Eylem", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("İlgeç", new PosAndType(LexinfoOnt.adposition, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Kısaltma",
        new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Kız adı",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry)); // Female name
    posAndTypeValueMap.put("Ön ad", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Ön ek", new PosAndType(LexinfoOnt.prefix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Özel Ad",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Özel ad",
        new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Sayı", new PosAndType(LexinfoOnt.numeral, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Son ek", new PosAndType(LexinfoOnt.suffix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Sözce",
        new PosAndType(LexinfoOnt.expression, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Ünlem",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));

    posAndTypeValueMap.put("Soyadı", posAndTypeValueMap.get("Özel ad"));

    // Excerpt taken from template "Şablon:İsim soylu kelime" in Turkish
    posAndTypeValueMap.put("Aile ismi", posAndTypeValueMap.get("Soyadı"));
    posAndTypeValueMap.put("Edat", posAndTypeValueMap.get("İlgeç"));
    posAndTypeValueMap.put("Erkek ismi", posAndTypeValueMap.get("Erkek adı"));
    posAndTypeValueMap.put("Fiil", posAndTypeValueMap.get("Eylem"));
    posAndTypeValueMap.put("İbare", posAndTypeValueMap.get("Sözce"));
    posAndTypeValueMap.put("İsim", posAndTypeValueMap.get("Ad"));
    posAndTypeValueMap.put("Isim", posAndTypeValueMap.get("Ad"));
    posAndTypeValueMap.put("isim", posAndTypeValueMap.get("Ad"));
    posAndTypeValueMap.put("Kız ismi", posAndTypeValueMap.get("Kız adı"));
    posAndTypeValueMap.put("Özel isim", posAndTypeValueMap.get("Özel ad"));
    posAndTypeValueMap.put("Özel İsim", posAndTypeValueMap.get("Özel ad"));
    posAndTypeValueMap.put("Ozel ad", posAndTypeValueMap.get("Özel ad"));
    posAndTypeValueMap.put("Sıfat", posAndTypeValueMap.get("Ön ad"));
    posAndTypeValueMap.put("Soy ismi", posAndTypeValueMap.get("Soyadı"));
    posAndTypeValueMap.put("Zamir", posAndTypeValueMap.get("Adıl"));
    posAndTypeValueMap.put("Zarf", posAndTypeValueMap.get("Belirteç"));

    // Other POS found in wiktionary data
    posAndTypeValueMap.put("Ek", new PosAndType(LexinfoOnt.affix, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Eylem (basit)",
        new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Harf", new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("İfade", new PosAndType(LexinfoOnt.expression, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("Önek", posAndTypeValueMap.get("Ön ek"));
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
