package org.getalp.dbnary.languages.deu;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.getalp.dbnary.morphology.InflectionData;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.SkosOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanInflectionData extends InflectionData {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  public enum Genre {
    MASCULIN, FEMININ, NEUTRUM, NOTHING
  }

  public enum Cas {
    NOMINATIF, GENITIF, DATIF, ACCUSATIF, NOTHING
  }

  public enum Mode {
    INFINITIV, ZU_INFINITIV, PARTIZIPIEN, GERUNDIVUM, IMPERATIV, INDICATIV, KONJUNKTIV2, KONJUNKTIV1, NOTHING
  }

  public enum Voice {
    AKTIV, VORGANGSPASSIV, ZUSTANDSPASSIV, PASSIV, ZUSTANDSREFLEXIVEPASSIV, REFLEXIV, NOTHING
  }

  public enum Tense {
    PRÄSENS, PRÄTERITUM, PERFEKT, FUTURE1, FUTURE2, PLUSQUAMPERFEKT, NOTHING
  }

  public enum Degree {
    POSITIVE, COMPARATIVE, SUPERLATIVE, NOTHING
  }

  public enum GNumber {
    SINGULAR, PLURAL, NOTHING
  }

  public enum Person {
    FIRST, SECOND, THIRD, HÖFLICHKEITSFORM, NOTHING
  }

  public enum InflectionType {
    STRONG, WEAK, MIXED, NOTHING
  }

  public enum Valency {
    TRANSITIVE, INTRANSITIVE, NOTHING
  }


  public Degree degree = Degree.NOTHING;
  public Mode mode = Mode.NOTHING;
  public Voice voice = Voice.NOTHING;
  public Tense tense = Tense.NOTHING;
  public GNumber number = GNumber.NOTHING;
  public Cas cas = Cas.NOTHING;
  public Genre genre = Genre.NOTHING;
  public Person person = Person.NOTHING;
  public InflectionType inflectionType = InflectionType.NOTHING;
  public Valency valency = Valency.NOTHING;
  public Set<String> note = new HashSet<>();

  public static Model model = ModelFactory.createDefaultModel();


  @Override
  public HashSet<PropertyObjectPair> toPropertyObjectMap() {
    HashSet<PropertyObjectPair> inflections = new HashSet<>();
    switch (this.degree) {
      case POSITIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasDegree, OliaOnt.Positive));
        break;
      case COMPARATIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasDegree, OliaOnt.Comparative));
        break;
      case SUPERLATIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasDegree, OliaOnt.Superlative));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for degree", this.degree);
        break;
    }
    switch (this.cas) {
      case NOMINATIF:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Nominative));
        break;
      case GENITIF:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.GenitiveCase));
        break;
      case ACCUSATIF:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Accusative));
        break;
      case DATIF:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.DativeCase));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for case", this.cas);
        break;
    }
    switch (this.genre) {
      case MASCULIN:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Masculine));
        break;
      case FEMININ:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Feminine));
        break;
      case NEUTRUM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Neuter));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for genre", this.genre);
        break;
    }
    switch (this.number) {
      case SINGULAR:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Singular));
        break;
      case PLURAL:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Plural));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for number", this.number);
        break;
    }
    switch (this.tense) {
      case PRÄTERITUM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Past));
        break;
      case PRÄSENS:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Present));
        break;
      case PERFEKT:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Perfect));
        break;
      case PLUSQUAMPERFEKT:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.PastPerfectTense));
        break;
      case FUTURE1:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Future));
        break;
      case FUTURE2:
        // Confirmed by Ch. Chiarcos...
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.FuturePerfect));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for tense", this.tense);
        break;
    }
    switch (this.mode) {
      case IMPERATIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.ImperativeMood));
        break;
      case INDICATIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.IndicativeMood));
        break;
      case KONJUNKTIV1:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.QuotativeMood));
        break;
      case KONJUNKTIV2:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.SubjunctiveMood));
        break;
      case PARTIZIPIEN:
        // TODO: Participle is a part of speech, not a mood...
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.Participle));
        break;
      case INFINITIV:
        // TODO: Infinitive is a part of speech, not a mood...
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.Infinitive));
        break;
      case GERUNDIVUM:
        // TODO: AdverbialParticiple is a part of speech, not a mood...
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.AdverbialParticiple));
        break;
      case ZU_INFINITIV:
        // TODO: Infinitive is a part of speech, not a mood...
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.Infinitive));
        note.add("Zu-Infinitive");
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for mode", this.mode);
        break;
    }
    switch (this.person) {
      case FIRST:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasPerson, OliaOnt.First));
        break;
      case SECOND:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasPerson, OliaOnt.Second));
        break;
      case THIRD:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasPerson, OliaOnt.Third));
        break;
      case HÖFLICHKEITSFORM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasPerson, OliaOnt.SecondPolite));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for person", this.person);
        break;
    }
    switch (this.inflectionType) {
      case STRONG:
        inflections
            .add(PropertyObjectPair.get(OliaOnt.hasInflectionType, OliaOnt.StrongInflection));
        break;
      case WEAK:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasInflectionType, OliaOnt.WeakInflection));
        break;
      case MIXED:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasInflectionType, OliaOnt.MixedInflection));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for inflection type", this.inflectionType);
        break;
    }
    switch (this.voice) {
      case AKTIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasVoice, OliaOnt.ActiveVoice));
        break;
      case VORGANGSPASSIV:
      case PASSIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasVoice, OliaOnt.PassiveVoice));
        break;
      case ZUSTANDSPASSIV:
        note.add("Zustandpassiv");
        break;
      case ZUSTANDSREFLEXIVEPASSIV:
        note.add("Zustandreflexiv");
        break;
      case REFLEXIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasVoice, OliaOnt.ReflexiveVoice));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for voice", this.voice);
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
