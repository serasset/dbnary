package org.getalp.dbnary.nld;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author malick
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {

    // DONE: dutch POS
    posAndTypeValueMap.put("abbr",
        new PosAndType(LexinfoOnt.abbreviation, LexinfoOnt.AbbreviatedForm));
    posAndTypeValueMap.put("adjc", new PosAndType(LexinfoOnt.adjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("adverb", new PosAndType(LexinfoOnt.adverb, LexinfoOnt.Adverb));
    posAndTypeValueMap.put("adverb-num", new PosAndType(LexinfoOnt.adverb, LexinfoOnt.Adverb)); // ?
    posAndTypeValueMap.put("art", new PosAndType(LexinfoOnt.article, LexinfoOnt.Article));
    posAndTypeValueMap.put("cijfer", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("circumf",
        new PosAndType(LexinfoOnt.circumposition, LexinfoOnt.Adposition));
    posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, LexinfoOnt.Conjunction));
    posAndTypeValueMap.put("cont",
        new PosAndType(LexinfoOnt.contraction, LexinfoOnt.AbbreviatedForm));
    // not for Dutch...
    // posAndTypeValueMap.put("ideo", new PosAndType(LexinfoOnt., OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("interf", new PosAndType(LexinfoOnt.infix, LexinfoOnt.Infix));
    posAndTypeValueMap.put("interj",
        new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection));
    posAndTypeValueMap.put("leesteken", new PosAndType(LexinfoOnt.punctuation, LexinfoOnt.Symbol));
    posAndTypeValueMap.put("letter", new PosAndType(LexinfoOnt.letter, LexinfoOnt.Symbol));
    posAndTypeValueMap.put("name", new PosAndType(LexinfoOnt.properNoun, LexinfoOnt.ProperNoun));
    posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, LexinfoOnt.Noun));
    posAndTypeValueMap.put("num", new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));
    // not for Dutch...
    // posAndTypeValueMap.put("num-distr", new PosAndType(LexinfoOnt., OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("num-indef",
        new PosAndType(LexinfoOnt.indefiniteCardinalNumeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("num-int",
        new PosAndType(LexinfoOnt.interrogativeCardinalNumeral, LexinfoOnt.Numeral));
    // Not for Dutch
    // posAndTypeValueMap.put("num-srt", new PosAndType(LexinfoOnt., OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("ordn",
        new PosAndType(LexinfoOnt.ordinalAdjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("ordn-indef",
        new PosAndType(LexinfoOnt.indefiniteOrdinalNumeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("prcp",
        new PosAndType(LexinfoOnt.participleAdjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("phrase",
        new PosAndType(LexinfoOnt.phraseologicalUnit, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("post",
        new PosAndType(LexinfoOnt.postposition, LexinfoOnt.Postposition));
    posAndTypeValueMap.put("pref", new PosAndType(LexinfoOnt.prefix, LexinfoOnt.Prefix));
    posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition));
    // Not in Dutch
    // posAndTypeValueMap.put("prep-form", new PosAndType(LexinfoOnt., OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("pron-adv",
        new PosAndType(LexinfoOnt.adverbialPronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-dem",
        new PosAndType(LexinfoOnt.demonstrativePronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-excl",
        new PosAndType(LexinfoOnt.exclamativePronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-indef",
        new PosAndType(LexinfoOnt.indefinitePronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-int",
        new PosAndType(LexinfoOnt.interrogativePronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-pers",
        new PosAndType(LexinfoOnt.personalPronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-pos",
        new PosAndType(LexinfoOnt.possessivePronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-rec",
        new PosAndType(LexinfoOnt.reciprocalPronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-refl",
        new PosAndType(LexinfoOnt.reflexivePersonalPronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("pronom-rel",
        new PosAndType(LexinfoOnt.relativePronoun, LexinfoOnt.Pronoun));
    // posAndTypeValueMap.put("pronom-temp", new PosAndType(LexinfoOnt.noun,
    // LexinfoOnt.LexicalEntry));
    posAndTypeValueMap.put("pronoun", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("prtc", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));
    posAndTypeValueMap.put("suff", new PosAndType(LexinfoOnt.suffix, LexinfoOnt.Suffix));
    posAndTypeValueMap.put("symbool", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));
    posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, LexinfoOnt.Verb));
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public static boolean isValidPOS(String pos) {
    return posAndTypeValueMap.containsKey(pos);
  }

  @Override
  public void addPartOfSpeech(String pos) {
    // reset the sense number.
    currentSenseNumber = 0;
    currentSubSenseNumber = 0;
    super.addPartOfSpeech(pos);
  }

}
