package org.getalp.dbnary.languages.fra.morphology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Literal;
import org.getalp.dbnary.model.DbnaryModel;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.StrictInflexionScheme;
import org.getalp.dbnary.morphology.StrictInflexionScheme.INCOHERENT_INFLECTION_SCHEME;
import org.getalp.dbnary.wiki.WikiCharSequence;
import org.getalp.dbnary.wiki.WikiPattern;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.model.lexinfo.Gender;
import org.getalp.model.lexinfo.Mood;
import org.getalp.model.lexinfo.Number;
import org.getalp.model.lexinfo.Person;
import org.getalp.model.lexinfo.Tense;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jakse
 */

public class FrenchInflectionDecoder {

  private static Logger log = LoggerFactory.getLogger(FrenchInflectionDecoder.class);

  public static final Literal trueLiteral = DbnaryModel.tBox.createTypedLiteral(true);

  // public static final Property extractedFromConjTable =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromConjTable");
  // public static final Property extractedFromFrenchSentence =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromFrenchSentence");
  // public static final Property extractedFromInflectionTable =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromInflectionTable");

  private static Pattern frAccordPattern = Pattern.compile("^\\{\\{(?:fr-accord|fr-rég)");

  private static ArrayList<String> explode(char sep, String str) {
    int lastI = 0;
    ArrayList<String> res = new ArrayList<>();
    int pos = str.indexOf(sep, lastI);

    while (pos != -1) {
      res.add(str.substring(lastI, pos));
      lastI = pos + 1;
      pos = str.indexOf(sep, lastI);
    }

    res.add(str.substring(lastI, str.length()));
    return res;
  }

  static void addAtomicMorphologicalInfo(InflectionScheme infos, String word) {
    word = word.replaceAll("passé simple", "passé_simple");
    word = word.replaceAll("première personne", "première_personne");
    word = word.replaceAll("deuxième personne", "deuxième_personne");
    word = word.replaceAll("troisième personne", "troisième_personne");
    word = word.replaceAll("futur simple", "futur_simple");
    word = word.replaceAll("participe passé", "participe_passé");
    word = word.replaceAll("participe présent", "participe_présent");
    decodeMorphologicalInfo(infos, word);
  }

  static void decodeMorphologicalInfo(InflectionScheme infos, String word) {
    switch (word) {
      case "singulier":
        infos.add(Number.SINGULAR);
        break;
      case "pluriel":
        infos.add(Number.PLURAL);
        break;
      case "masculin":
      case "masculinet": // happens when we should have "masculin et féminin", the "et" gets sticked
        // to the "masculin".
        infos.add(Gender.MASCULINE);
        break;
      case "féminin":
        infos.add(Gender.FEMININE);
        break;
      case "présent":
        infos.add(Tense.PRESENT);
        break;
      case "imparfait":
        infos.add(Tense.IMPERFECT);
        break;
      case "passé":
        infos.add(Tense.PAST); // ???
        break;
      case "futur":
        infos.add(Tense.FUTURE);
        break;
      case "indicatif":
        infos.add(Mood.INDICATIVE);
        break;
      case "subjonctif":
        infos.add(Mood.SUBJUNCTIVE);
        break;
      case "conditionnel":
        infos.add(Mood.CONDITIONAL);
        break;
      case "impératif":
        infos.add(Mood.IMPERATIVE);
        break;
      case "participe":
        infos.add(Mood.PARTICIPLE);
        break;
      case "première_personne":
        infos.add(Person.FIRST);
        break;
      case "deuxième_personne":
        infos.add(Person.SECOND);
        break;
      case "troisième_personne":
        infos.add(Person.THIRD);
        break;
      case "futur_simple":
        infos.add(Tense.FUTURE);
        infos.add(Mood.INDICATIVE);
        break;
      case "passé_simple":
        infos.add(Tense.PRETERITE);
        infos.add(Mood.INDICATIVE);
        break;
      case "masculin singulier":
        infos.add(Gender.MASCULINE);
        infos.add(Number.SINGULAR);
        break;
      case "féminin singulier":
        infos.add(Gender.FEMININE);
        infos.add(Number.SINGULAR);
        break;
      case "masculin pluriel":
        infos.add(Gender.MASCULINE);
        infos.add(Number.PLURAL);
        break;
      case "féminin pluriel":
        infos.add(Gender.FEMININE);
        infos.add(Number.PLURAL);
        break;
      case "participe_passé":
      case "participe_passé masculin singulier":
        infos.add(Mood.PARTICIPLE);
        infos.add(Tense.PAST);
        infos.add(Gender.MASCULINE);
        infos.add(Number.SINGULAR);
        break;
      case "participe_passé féminin singulier":
        infos.add(Mood.PARTICIPLE);
        infos.add(Tense.PAST);
        infos.add(Gender.FEMININE);
        infos.add(Number.SINGULAR);
        break;
      case "participe_passé masculin pluriel":
        infos.add(Mood.PARTICIPLE);
        infos.add(Tense.PAST);
        infos.add(Gender.MASCULINE);
        infos.add(Number.PLURAL);
        break;
      case "participe_passé féminin pluriel":
        infos.add(Mood.PARTICIPLE);
        infos.add(Tense.PAST);
        infos.add(Gender.FEMININE);
        infos.add(Number.PLURAL);
        break;
      case "participe_présent":
        infos.add(Mood.PARTICIPLE);
        infos.add(Tense.PRESENT);
        break;
      default:
        ArrayList<String> multiwords = explode(' ', word);
        if (multiwords.size() > 1) {
          for (String w : multiwords) {
            decodeMorphologicalInfo(infos, w);
          }
        }
    }
  }

  private static final Pattern inflectionPattern = WikiPattern
      .compile("^(.*(?:de|d\\’|du verbe|du nom|de l’adjectif))\\s*(\\p{InternalLink}).\\s*$");

  public static Stream<Pair<InternalLink, LexicalForm>> getOtherForms(IndentedItem ident,
      String pronunciation) {
    List<Pair<InternalLink, LexicalForm>> result = new LinkedList<>();
    WikiCharSequence inflectionSource = new WikiCharSequence(ident.asIndentedItem().getContent())
        .mutateString(s -> s.toLowerCase().replaceAll("''+", "").trim());
    Matcher m = inflectionPattern.matcher(inflectionSource);
    if (m.matches()) {
      String inflectionDescription = m.group(1);
      InternalLink target = inflectionSource.getToken(m.group(2)).asInternalLink();
      try {
        InflectionScheme infl = new StrictInflexionScheme();
        Arrays.stream(inflectionDescription.split("\\bde l’|\\bdu\\b|\\bde\\b"))
            .forEach(w -> addAtomicMorphologicalInfo(infl, w.trim()));
        LexicalForm form = new LexicalForm(infl);
        result.add(new ImmutablePair<>(target, form));
      } catch (INCOHERENT_INFLECTION_SCHEME e) {
        // An incoherent inflection scheme has been detected, just ignore it.
        log.debug("Incoherent inflection scheme while extracting {}",
            inflectionSource.getSourceContent(m.group()));
      }
    }
    return result.stream();
  }
}
