package org.getalp.dbnary.languages.ita;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.commons.PostTranslationDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryDataHandler extends PostTranslationDataHandler {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {

    posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("sost", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("loc noun",
        new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc nom",
        new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("nome", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("name", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("adjc", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("agg", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("loc adjc",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc agg",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("avv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("loc avv",
        new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("loc verb",
        new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression));

    posAndTypeValueMap.put("agg num", new PosAndType(LexinfoOnt.numeral, OntolexOnt.Word));
    posAndTypeValueMap.put("agg poss",
        new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word));
    // card/ord is not a Part of speech, but an information added to aggetivi numerali
    // posAndTypeValueMap.put("card", new PosAndType(LexinfoOnt.cardinalNumeral, OntolexOnt.Word));
    // posAndTypeValueMap.put("ord", new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word));
    posAndTypeValueMap.put("agg nom", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));

    posAndTypeValueMap.put("art", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
    posAndTypeValueMap.put("cong", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
    posAndTypeValueMap.put("inter", new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word));
    posAndTypeValueMap.put("interj", new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word));
    posAndTypeValueMap.put("loc cong",
        new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc conj",
        new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc inter",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc interj",
        new PosAndType(LexinfoOnt.interjection, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("loc prep",
        new PosAndType(LexinfoOnt.preposition, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("posp", new PosAndType(LexinfoOnt.postposition, OntolexOnt.Word));
    posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    posAndTypeValueMap.put("pron poss",
        new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("pronome poss",
        new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("pronome", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
    posAndTypeValueMap.put("pron dim",
        new PosAndType(LexinfoOnt.demonstrativePronoun, OntolexOnt.Word));

    // TODO: -acron-, -acronim-, -acronym-, -espr-, -espress- mark locution as phrases

    // Template:-abbr-

    // Template redirects
    // Template:-abbr-
    // Template:-acronim-
    // Template:-acronym-
    // Template:-esclam-
    // Template:-espress-
    // Template:-let-
    // Template:-loc noun form-
    // Template:-name form-
    // Template:-noun form-
    // Template:-prefix-
    // Template:-pronome form-
    // Template:-pronoun form-
    posAndTypeValueMap.put("pref", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
    posAndTypeValueMap.put("prefix", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
    posAndTypeValueMap.put("suff", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));
    posAndTypeValueMap.put("suffix", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));

    posAndTypeValueMap.put("acron", new PosAndType(null, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("acronim", new PosAndType(null, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("acronym", new PosAndType(null, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("espr", new PosAndType(null, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("espress", new PosAndType(null, OntolexOnt.MultiWordExpression));

    // For translation glosses

  }

  public static boolean isValidPOS(String pos) {
    return posAndTypeValueMap.containsKey(pos);
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  protected List<Resource> getLexicalEntryUsingGloss(Resource structuredGloss) {
    ArrayList<Resource> res = new ArrayList<>();
    Statement s = structuredGloss.getProperty(RDF.value);
    if (null == s) {
      return res;
    }
    String gloss = s.getString();
    if (null == gloss) {
      return res;
    }
    gloss = gloss.toLowerCase().trim();
    if (gloss.startsWith("aggettivo numerale")) {
      addAllResourceOfPoS(res, LexinfoOnt.numeral);
    } else if (gloss.startsWith("aggettivo")) {
      addAllResourceOfPoS(res, LexinfoOnt.adjective);
    } else if (gloss.startsWith("avverbio")) {
      addAllResourceOfPoS(res, LexinfoOnt.adverb);
    } else if (gloss.startsWith("pronome")) {
      addAllResourceOfPoS(res, LexinfoOnt.pronoun);
    } else if (gloss.startsWith("sostantivo")) {
      addAllResourceOfPoS(res, LexinfoOnt.noun);
    } else if (gloss.startsWith("verbo")) {
      addAllResourceOfPoS(res, LexinfoOnt.verb);
    } else if (gloss.startsWith("agg. e sost.")) {
      addAllResourceOfPoS(res, LexinfoOnt.adjective);
      addAllResourceOfPoS(res, LexinfoOnt.noun);
    } else {
      log.debug("Could not decode gloss '{}' to chose lexical entry in '{}'", gloss,
          currentPagename());
    }
    return res;
  }
}
