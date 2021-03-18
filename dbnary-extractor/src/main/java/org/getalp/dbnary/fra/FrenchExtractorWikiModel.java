package org.getalp.dbnary.fra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Literal;
import org.getalp.dbnary.fra.morphology.MorphologyWikiModel;
import org.getalp.dbnary.model.DbnaryModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author jakse
 */

public class FrenchExtractorWikiModel extends MorphologyWikiModel {

  private static Logger log = LoggerFactory.getLogger(FrenchExtractorWikiModel.class);

  private IWiktionaryDataHandler delegate;

  public static final Literal trueLiteral = DbnaryModel.tBox.createTypedLiteral(true);

  // public static final Property extractedFromConjTable =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromConjTable");
  // public static final Property extractedFromFrenchSentence =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromFrenchSentence");
  // public static final Property extractedFromInflectionTable =
  // DbnaryModel.tBox.createProperty(DBnaryOnt.getURI() + "extractedFromInflectionTable");

  private static Pattern frAccordPattern = Pattern.compile("^\\{\\{(?:fr-accord|fr-rég)");

  public FrenchExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = wdh;
  }

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

  static void addAtomicMorphologicalInfo(Set<PropertyObjectPair> infos, String word) {
    switch (word) {
      case "singulier":
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case "pluriel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case "masculin":
      case "masculinet": // happens when we should have "masculin et féminin", the "et" gets sticked
        // to the "masculin".
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        break;
      case "féminin":
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        break;
      case "présent":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.present));
        break;
      case "imparfait":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.imperfect));
        break;
      case "passé":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        break;
      case "futur":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.future));
        break;
      case "indicatif":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.indicative));
        break;
      case "subjonctif":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.subjunctive));
        break;
      case "conditionnel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.conditional));
        break;
      case "impératif":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.imperative));
        break;
      case "participe":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        break;
      case "première personne":
        infos.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.firstPerson));
        break;
      case "deuxième personne":
        infos.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.secondPerson));
        break;
      case "troisième personne":
        infos.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.thirdPerson));
        break;
      case "futur simple":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.future));
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.indicative));
        break;
      case "passé simple":
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.indicative));
        break;
      case "masculin singulier":
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case "féminin singulier":
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case "masculin pluriel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case "féminin pluriel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case "participe passé masculin singulier":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case "participe passé féminin singulier":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case "participe passé masculin pluriel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case "participe passé féminin pluriel":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        infos.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        infos.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case "participe présent":
        infos.add(PropertyObjectPair.get(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
        infos.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.present));
        break;
      default:
        ArrayList<String> multiwords = explode(' ', word);
        if (multiwords.size() > 1) {
          for (String w : multiwords) {
            addAtomicMorphologicalInfo(infos, w);
          }
        }
    }
  }
}
