package org.getalp.dbnary.languages.spa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.commons.PostTranslationDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends PostTranslationDataHandler {

  private final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    if (pos.startsWith("{{")) {
      pos = pos.substring(2).trim();
    }
    pos = pos.split("\\|")[0];

    PosAndType pat = null;
    String spos = pos;

    if (pos.startsWith("adjetivo cardinal")) {
      pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo ordinal")) {
      pat = new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo posesivo")) {
      pat = new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo")) {
      // Catches :
      //    adjetivo demostrativo
      //    adjetivo indefinido
      //    adjetivo indeterminado
      //    adjetivo interrogativo
      //    adjetivo numeral
      //    adjetivo relativo
      pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
    } else if (pos.startsWith("sustantivo propio")) {
      // Catches : sustantivo propio/pruebas
      pat = new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word);
    } else if (pos.startsWith("sustantivo femenino y masculino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo femenino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo masculino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
    } else if (pos.startsWith("adverbio")) {
      // Catches :
      //    adverbio comparativo
      //    adverbio de afirmación
      //    adverbio de cantidad
      //    adverbio de duda
      //    adverbio de lugar
      //    adverbio de modo
      //    adverbio de negación
      //    adverbio de orden
      //    adverbio de tiempo
      //    adverbio demostrativo
      //    adverbio interrogativo
      //    adverbio relativo
      pat = new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word);
    } else if (pos.startsWith("verbo transitivo")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactic frame
    } else if (pos.startsWith("verbo intransitivo")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactic frame
    } else if (pos.startsWith("verbo pronominal")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactic frame
    } else if (pos.startsWith("verbo impersonal")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactic frame
    } else if (pos.startsWith("verbo auxiliar")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactic frame
    } else if (pos.startsWith("verbo")) {
      // Catches :
      //    verbo bitransitivo
      //    verbo enclítico
      //    verbo modal
      //    verbo perfectivo
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
    } else if (pos.startsWith("afijo")) {
      pat = new PosAndType(LexinfoOnt.affix, OntolexOnt.Affix);
    } else if (pos.startsWith("prefijo")) {
      pat = new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix);
    } else if (pos.startsWith("circunfijo")) {
      pat = new PosAndType(LexinfoOnt.affix, OntolexOnt.Affix);
    } else if (pos.startsWith("sufijo")) {
      // Catches :    sufijo flexivo"
      pat = new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix);
    } else if (pos.startsWith("símbolo")) {
      pat = new PosAndType(LexinfoOnt.symbol, OntolexOnt.LexicalEntry);
    } else if (pos.startsWith("artículo")) {
      // Catches:
      // artículo determinado
      // artículo indeterminado
      pat = new PosAndType(LexinfoOnt.symbol, OntolexOnt.LexicalEntry);
    } else if (pos.startsWith("conjunción")) {
      // Catches:
      // conjunción adversativa
      // conjunción ilativa
      pat = new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word);
    } else if (pos.startsWith("determinante")) {
      pat = new PosAndType(LexinfoOnt.determiner, OntolexOnt.Word);
    } else if (pos.startsWith("dígrafo") || pos.startsWith("letra")) {
      pat = new PosAndType(LexinfoOnt.letter, OntolexOnt.LexicalEntry);
    } else if (pos.startsWith("interjección")) {
      pat = new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word);
    } else if (pos.startsWith("locución adjetiva")) {
      pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución adverbial")) {
      pat = new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución conjuntiva")) {
      pat = new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución interjectiva")) {
      pat = new PosAndType(LexinfoOnt.interjection, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución prepositiva")) {
      pat = new PosAndType(LexinfoOnt.preposition, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución pronominal")) {
      pat = new PosAndType(LexinfoOnt.pronoun, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución sustantiva")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución verbal")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("locución") || pos.startsWith("expresión")) {
      pat = new PosAndType(LexinfoOnt.expression, OntolexOnt.MultiWordExpression);
    } else if (pos.startsWith("onomatopeya")) {
      // NOT SURE I SHOULD USE THIS
      pat = new PosAndType(null, OntolexOnt.Word);
    } else if (pos.startsWith("partícula")) {
      pat = new PosAndType(LexinfoOnt.particle, OntolexOnt.Word);
    } else if (pos.startsWith("postposición")) {
      pat = new PosAndType(LexinfoOnt.postposition, OntolexOnt.Word);
    } else if (pos.startsWith("preposición")) {
      // Catches:
      // preposición de ablativo
      // preposición de acusativo
      // preposición de acusativo o ablativo
      // preposición de genitivo
      pat = new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre demostrativo")) {
      pat = new PosAndType(LexinfoOnt.demonstrativePronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre indefinido")) {
      pat = new PosAndType(LexinfoOnt.indefinitePronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre interrogativo")) {
      pat = new PosAndType(LexinfoOnt.interrogativePronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre personal")) {
      pat = new PosAndType(LexinfoOnt.personalPronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre posesivo")) {
      pat = new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre relativo")) {
      pat = new PosAndType(LexinfoOnt.relativePronoun, OntolexOnt.Word);
    } else if (pos.startsWith("pronombre")) {
      pat = new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word);
    } else if (pos.startsWith("refrán")) {
      pat = new PosAndType(LexinfoOnt.proverb, OntolexOnt.Word);
    } else if (pos.startsWith("sigla")) {
      pat = new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry);
    }

    // TODO handle extra information (genre, ...) from pos
    log.debug("Handling POS String: {} --> {} || in {}", pos, posResource(pat),
        currentPage.getName());
    // PosAndType pat = posAndTypeValueMap.get(pos);
    Resource typeR = typeResource(pat);
    initializeLexicalEntry(spos, posResource(pat), typeR);
  }

  private final static String posPatternString = "(?:verbo|sustantivo|adjetivo|adverbio)";
  private final static String glossWithPosValue =
      "(?:^\\s*(?:como\\s+)?(" + posPatternString + ")\\s*$|" //
          + "^.*\\((" + posPatternString + ")\\)\\s*$|" //
          + "^\\s*(" + posPatternString + "):.*$)";
  private final Pattern glossWithPossPattern = Pattern.compile(glossWithPosValue);
  private final Matcher glossWithPos = glossWithPossPattern.matcher("");

  @Override
  public void registerTranslation(String lang, Resource currentGloss, String usage, String word) {
    // TODO: the super implementation will register the translation to the lexical entry if there is
    // only one defined in the current etymology, however, sometimes, translations are given for
    // inflected forms (which are not extracted). Such translation should not be attached to
    // anything.
    super.registerTranslation(lang, currentGloss, usage, word);
  }

  @Override
  protected List<List<Resource>> getLexicalEntriesUsingGloss(Resource structuredGloss) {
    ArrayList<List<Resource>> res = new ArrayList<>();
    // TODO: Should I take the sense Number into account, as it should be correctly processed by
    // the extractor ?
    Statement senseNumStmt = structuredGloss.getProperty(DBnaryOnt.senseNumber);
    if (null != senseNumStmt) {
      String translationSenseNum = senseNumStmt.getString();
      Pair<Resource, Resource> pair = senses.get(translationSenseNum);
      if (null != pair) {
        // Add the lexical entry first so that the translation id follows the usual convention
        res.add(List.of(pair.getRight(), pair.getLeft()));
        return res;
      }
    }
    Statement glossStmt = structuredGloss.getProperty(RDF.value);
    if (null == glossStmt) {
      return res;
    }
    String gloss = glossStmt.getString();
    if (null == gloss) {
      return res;
    }
    addAllResourceOfPoS(res, gloss.toLowerCase().trim());
    if (!res.isEmpty()) {
      return res;
    }
    glossWithPos.reset(gloss.toLowerCase().trim());
    if (glossWithPos.matches()) {
      String pos = Stream.of(glossWithPos.group(1), glossWithPos.group(2), glossWithPos.group(3))
          .filter(Objects::nonNull).findFirst().orElse(null);
      addAllResourceOfPoS(res, getPosResource(pos));
    }
    return res;
  }

  private Resource getPosResource(String pos) {
    if ("verbo".equals(pos))
      return LexinfoOnt.verb;
    else if ("sustantivo".equals(pos))
      return LexinfoOnt.noun;
    else if ("adjetivo".equals(pos))
      return LexinfoOnt.adjective;
    else if ("adverbio".equals(pos))
      return LexinfoOnt.adverb;
    else
      return null;
  }

}
