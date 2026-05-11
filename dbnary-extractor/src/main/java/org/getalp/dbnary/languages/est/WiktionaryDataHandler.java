package org.getalp.dbnary.languages.est;

import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {
  static {
    posAndTypeValueMap.put("nimisõna", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("s.()", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("s", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("loendussõna", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    // posAndTypeValueMap.put("nimisõnad", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("nimisõnafraas", new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("nimisõnaühend", new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("liiginimi", new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("omastav omadussõna", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("umbmäärane omadussõna", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("näitav omadussõna", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("omadussõna", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("omadussõnafraas", new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("sihiline ja sihitu tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    // Intentional verb
    posAndTypeValueMap.put("sihiline tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    // Aimless verb
    posAndTypeValueMap.put("sihitu tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    // Proper verb
    posAndTypeValueMap.put("enesekohane tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    // Impersonal verb
    posAndTypeValueMap.put("impersonaalne tegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("abisõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // auxiliary verb
    posAndTypeValueMap.put("abiverb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // auxiliary verb
    posAndTypeValueMap.put("partitsiip", new PosAndType(LexinfoOnt.participle, OntolexOnt.Word)); // auxiliary verb
    posAndTypeValueMap.put("tegusõnafraas", new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("ühendtegusõna", new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("määrsõna", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("kaassõna", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("kaassõnafraas", new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("määrsõnafraas", new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("tagasõnafraas", new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("kohanimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("pärisnimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("eesnimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("isikunimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("perekonnanimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("rahvanimi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("arvsõna", new PosAndType(LexinfoOnt.numeral, OntolexOnt.Word));
    posAndTypeValueMap.put("number", new PosAndType(LexinfoOnt.numeral, OntolexOnt.Word));
    posAndTypeValueMap.put("järgarv", new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word));
    posAndTypeValueMap.put("arvsõnafraas", new PosAndType(LexinfoOnt.numeral, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("lühend", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
    posAndTypeValueMap.put("asesõna", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("asesõnafraas", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("isikuline asesõna", new PosAndType(LexinfoOnt.personalPronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("omastav asesõna", new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("possessiivne asesõna", new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("küsiv asesõna", new PosAndType(LexinfoOnt.interrogativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("näitav asesõna", new PosAndType(LexinfoOnt.demonstrativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("umbmäärane asesõna", new PosAndType(LexinfoOnt.indefinitePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("siduv asesõna", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("hüüdsõna", new PosAndType(LexinfoOnt.exclamativePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("hüüdsõnafraas", new PosAndType(LexinfoOnt.exclamativePronoun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("v.", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // Transitive
    posAndTypeValueMap.put("v.t.", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // Transitive
    posAndTypeValueMap.put("v.t", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // Transitive
    posAndTypeValueMap.put("v.i.", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // intransitive
    posAndTypeValueMap.put("v.r.", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word)); // ?
    posAndTypeValueMap.put("eessõna", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("eessõnaline ühend", new PosAndType(LexinfoOnt.compoundPreposition, OntolexOnt.Word));
    posAndTypeValueMap.put("eesliide", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Word));
    posAndTypeValueMap.put("järelliide", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Word));
    posAndTypeValueMap.put("järelsõna", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Word));
    posAndTypeValueMap.put("tagasõna", new PosAndType(LexinfoOnt.affix, OntolexOnt.Word));
    posAndTypeValueMap.put("sidesõna", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("ühendsidesõna", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("sidesõnaline ühend", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("sidesõnafraas", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("akronüüm", new PosAndType(LexinfoOnt.acronym, OntolexOnt.Word));
    posAndTypeValueMap.put("kontraktsioon", new PosAndType(LexinfoOnt.contraction, OntolexOnt.Word));
    posAndTypeValueMap.put("kildsõna", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("sidend", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("pron. pers.", new PosAndType(LexinfoOnt.personalPronoun, OntolexOnt.Word));
    // cosmonym is a kind of toponym
    posAndTypeValueMap.put("kosmonüüm", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("astronüüm", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("artikkel", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
    posAndTypeValueMap.put("määrav artikkel", new PosAndType(LexinfoOnt.definiteArticle, OntolexOnt.Word));
    posAndTypeValueMap.put("umbmäärane artikkel", new PosAndType(LexinfoOnt.definiteArticle, OntolexOnt.Word));
    posAndTypeValueMap.put("partikkel", new PosAndType(LexinfoOnt.particle, OntolexOnt.Word));
    posAndTypeValueMap.put("sümbol", new PosAndType(LexinfoOnt.symbol, OntolexOnt.Word));
    posAndTypeValueMap.put("tähis", new PosAndType(LexinfoOnt.symbol, OntolexOnt.Word));
    posAndTypeValueMap.put("täht", new PosAndType(LexinfoOnt.letter, OntolexOnt.Word));
    posAndTypeValueMap.put("kirjamärk", new PosAndType(LexinfoOnt.letter, OntolexOnt.Word));
    posAndTypeValueMap.put("eessõnafraas", new PosAndType(LexinfoOnt.preposition, OntolexOnt.MultiWordExpression));

    // "Part Of Speech"
    posAndTypeValueMap.put("sõnaosa", new PosAndType(null, OntolexOnt.Word));
    // Word
    posAndTypeValueMap.put("sõna", new PosAndType(null, OntolexOnt.Word));

    posAndTypeValueMap.put("vanasõna", new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("väljend", new PosAndType(LexinfoOnt.expression, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("kõnekäänd", new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("fraas", new PosAndType(LexinfoOnt.setPhrase, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("lause", new PosAndType(LexinfoOnt.setPhrase, OntolexOnt.MultiWordExpression));
  }

  public boolean isPartOfSpeech(String s) {
    return posAndTypeValueMap.containsKey(s);
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public void voidPartOfSpeech() {
    currentLexicalEntry = null;
    currentLexEntry = null;
    currentEncodedLexicalEntryName = null;
    currentSense = null;
  }

  public void registerTranslation(boolean senseLocal, String lang, Resource currentGloss, String usage, String word) {
    Resource target = senseLocal ? currentSense : currentLexEntry;
    super.registerTranslationToEntity(target, lang, currentGloss, usage, word);
  }
}
