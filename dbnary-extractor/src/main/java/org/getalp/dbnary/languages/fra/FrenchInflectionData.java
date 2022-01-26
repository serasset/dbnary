package org.getalp.dbnary.languages.fra;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.SkosOnt;
import org.getalp.dbnary.morphology.InflectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchInflectionData extends InflectionData {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  public enum Genre {
    MASCULIN, FEMININ, MASCULIN_ET_FEMININ, NOTHING
  }

  public enum Mode {
    INFINITIF, INDICATIF, PARTICIPE, GERONDIF, IMPERATIF, SUBJONCTIF, CONDITIONNEL, NOTHING
  }

  public enum Tense {
    PRÉSENT, IMPARFAIT, PASSÉ_SIMPLE, FUTUR_SIMPLE, NOTHING
  }

  public enum Degree {
    POSITIF, COMPARATIF, SUPERLATIF, NOTHING
  }

  public enum GNumber {
    SINGULIER, PLURIEL, INDENOMBRABLE, SINGULIER_ET_PLURIEL, NOTHING
  }

  public enum Person {
    PREMIÈRE, SECONDE, TROISIÈME, NOTHING
  }

  public enum Valency {
    TRANSITIVE, INTRANSITIVE, NOTHING
  }


  public Degree degree = Degree.NOTHING;
  public Mode mode = Mode.NOTHING;
  public Tense tense = Tense.NOTHING;
  public GNumber number = GNumber.NOTHING;
  public Genre genre = Genre.NOTHING;
  public Person person = Person.NOTHING;
  public Valency valency = Valency.NOTHING;
  public Set<String> note = new HashSet<>();

  public static Model model = ModelFactory.createDefaultModel();


  @Override
  public HashSet<PropertyObjectPair> toPropertyObjectMap() {
    // TODO: Should I use lexinfo or Olia Space (this should be coherent with the canonical form)
    HashSet<PropertyObjectPair> inflections = new HashSet<>();
    switch (this.degree) {
      case POSITIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.degree, LexinfoOnt.positive));
        break;
      case COMPARATIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.degree, LexinfoOnt.comparative));
        break;
      case SUPERLATIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.degree, LexinfoOnt.superlative));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for degree", this.degree);
        break;
    }
    switch (this.genre) {
      case MASCULIN:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        break;
      case FEMININ:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        break;
      case MASCULIN_ET_FEMININ:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.feminine));
        inflections.add(PropertyObjectPair.get(LexinfoOnt.gender, LexinfoOnt.masculine));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for genre", this.genre);
        break;
    }
    switch (this.number) {
      case SINGULIER:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        break;
      case PLURIEL:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case SINGULIER_ET_PLURIEL:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.singular));
        inflections.add(PropertyObjectPair.get(LexinfoOnt.number, LexinfoOnt.plural));
        break;
      case INDENOMBRABLE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCountability, OliaOnt.Uncountable));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for number", this.number);
        break;
    }
    switch (this.tense) {
      case PASSÉ_SIMPLE:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.past));
        break;
      case PRÉSENT:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.present));
        break;
      case FUTUR_SIMPLE:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.future));
        break;
      case IMPARFAIT:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.tense, LexinfoOnt.imperfect));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for tense", this.tense);
        break;
    }
    switch (this.mode) {
      case IMPERATIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.imperative));
        break;
      case INDICATIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.indicative));
        break;
      case PARTICIPE:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.participle));
        break;
      case INFINITIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.infinitive));
        break;
      case GERONDIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.gerundive));
        break;
      case CONDITIONNEL:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.conditional));
        break;
      case SUBJONCTIF:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.mood, LexinfoOnt.subjunctive));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for mode", this.mode);
        break;
    }
    switch (this.person) {
      case PREMIÈRE:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.firstPerson));
        break;
      case SECONDE:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.secondPerson));
        break;
      case TROISIÈME:
        inflections.add(PropertyObjectPair.get(LexinfoOnt.person, LexinfoOnt.thirdPerson));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for person", this.person);
        break;
    }
    switch (this.valency) {
      case TRANSITIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasValency, OliaOnt.Transitive));
        break;
      case INTRANSITIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasValency, OliaOnt.Intransitive));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for valency", this.valency);
        break;
    }
    if (!note.isEmpty()) {
      StringBuffer notes = new StringBuffer();
      for (String s : note) {
        notes.append(s).append("|");
      }
      String tval = notes.toString().substring(0, notes.length() - 1);
      inflections.add(PropertyObjectPair.get(SkosOnt.note, model.createTypedLiteral(tval)));
    }
    return inflections;
  }
}
