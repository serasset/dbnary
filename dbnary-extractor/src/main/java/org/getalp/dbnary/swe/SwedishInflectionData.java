package org.getalp.dbnary.swe;

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

public class SwedishInflectionData extends InflectionData {

  private Logger log = LoggerFactory.getLogger(SwedishInflectionData.class);

  public enum Gender {
    MASCULINE, FEMININE, NEUTRUM, COMMON, NOTHING
  }

  public enum GNumber {
    SINGULAR, PLURAL, UNCOUNTABLE, NOTHING
  }

  public enum Definiteness {
    DEFINITE, INDEFINITE, NOTHING
  }

  public enum GrammaticalCase {
    NOMINATIVE, GENITIVE, ACCUSATIVE, DATIVE, NOTHING
  }

  public enum SubClass {
    ATTRIBUTIVE, PREDICATIVE, NOTHING
  }

  public enum SyntacticFunction {
    ADVERBIAL, NOMINAL, ADJECTIVAL, VERBAL, NOTHING
  }


  // -- kept from German
  public enum Mode {
    INFINITIV, ZU_INFINITIV, PRESENT_PARTICIPLE, PAST_PARTICIPLE, GERUNDIVUM, IMPERATIV, INDICATIV, KONJUNKTIV2, KONJUNKTIV1, NOTHING
  }

  public enum Voice {
    AKTIV, VORGANGSPASSIV, ZUSTANDSPASSIV, PASSIV, ZUSTANDSREFLEXIVEPASSIV, REFLEXIV, NOTHING
  }

  public enum Tense {
    PRESENT, PRETERIT, PERFEKT, SUPINUM, FUTURE1, FUTURE2, PLUSQUAMPERFEKT, NOTHING
  }

  public enum Degree {
    POSITIVE, COMPARATIVE, SUPERLATIVE, NOTHING
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

  public GNumber number = GNumber.NOTHING;
  public GrammaticalCase grammaticalCase = GrammaticalCase.NOTHING;
  public Gender gender = Gender.NOTHING;
  public Definiteness definiteness = Definiteness.NOTHING;
  public Set<String> note = new HashSet<>();
  public SubClass subClass = SubClass.NOTHING;
  public SyntacticFunction function = SyntacticFunction.NOTHING;

  public Degree degree = Degree.NOTHING;
  public Mode mode = Mode.NOTHING;
  public Voice voice = Voice.NOTHING;
  public Tense tense = Tense.NOTHING;
  public Person person = Person.NOTHING;
  public InflectionType inflectionType = InflectionType.NOTHING;
  public Valency valency = Valency.NOTHING;


  // ######### SETTERS ##########
  public void singular() {
    this.number = GNumber.SINGULAR;
  }

  public void plural() {
    this.number = GNumber.PLURAL;
  }

  public void uncountable() {
    this.number = GNumber.UNCOUNTABLE;
  }

  public void neutrum() {
    this.gender = Gender.NEUTRUM;
  }

  public void common() {
    this.gender = Gender.COMMON;
  }

  public void nominative() {
    this.grammaticalCase = GrammaticalCase.NOMINATIVE;
  }

  public void genitive() {
    this.grammaticalCase = GrammaticalCase.GENITIVE;
  }

  public void indefinite() {
    this.definiteness = Definiteness.INDEFINITE;
  }

  public void definite() {
    this.definiteness = Definiteness.DEFINITE;
  }

  public void note(String note) {
    this.note.add(note);
  }

  public void active() {
    this.voice = Voice.AKTIV;
  }

  public void passive() {
    this.voice = Voice.PASSIV;
  }

  public void present() {
    this.tense = Tense.PRESENT;
  }

  public void infinitive() {
    this.mode = Mode.INFINITIV;
  }

  public void preterit() {
    this.tense = Tense.PRETERIT;
  }

  public void imperative() {
    this.mode = Mode.IMPERATIV;
  }

  public void supinum() {
    this.tense = Tense.SUPINUM;
  }

  public void presentParticiple() {
    this.mode = Mode.PRESENT_PARTICIPLE;
  }

  public void pastParticiple() {
    this.mode = Mode.PAST_PARTICIPLE;
  }

  public void attributive() {
    this.subClass = SubClass.ATTRIBUTIVE;
  }

  public void predicative() {
    this.subClass = SubClass.PREDICATIVE;
  }

  public void positive() {
    this.degree = Degree.POSITIVE;
  }

  public void comparative() {
    this.degree = Degree.COMPARATIVE;
  }

  public void superlative() {
    this.degree = Degree.SUPERLATIVE;
  }

  public void masculine() {
    this.gender = Gender.MASCULINE;
  }

  public void feminine() {
    this.gender = Gender.FEMININE;
  }

  public void adverbial() {
    this.function = SyntacticFunction.ADVERBIAL;
  }

  public static Model model = ModelFactory.createDefaultModel();

  @Override
  public HashSet<PropertyObjectPair> toPropertyObjectMap() {
    HashSet<PropertyObjectPair> inflections = new HashSet<>();
    switch (this.gender) {
      case MASCULINE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Masculine));
        break;
      case FEMININE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Feminine));
        break;
      case NEUTRUM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Neuter));
        break;
      case COMMON:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.CommonGender));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for gender", this.gender);
        break;
    }
    switch (this.number) {
      case SINGULAR:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Singular));
        break;
      case PLURAL:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Plural));
        break;
      case UNCOUNTABLE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Uncountable));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for number", this.number);
        break;
    }
    switch (this.grammaticalCase) {
      case NOMINATIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Nominative));
        break;
      case GENITIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.GenitiveCase));
        break;
      case ACCUSATIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Accusative));
        break;
      case DATIVE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.DativeCase));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for case", this.grammaticalCase);
        break;
    }
    switch (this.definiteness) {
      case DEFINITE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasDefiniteness, OliaOnt.Definite));
        break;
      case INDEFINITE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasDefiniteness, OliaOnt.Indefinite));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for case", this.grammaticalCase);
        break;
    }
    switch (this.tense) {
      case PRETERIT:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Past));
        break;
      case PRESENT:
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
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.FuturePerfect));
        break;
      case SUPINUM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasTense, OliaOnt.Supine));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for tense", this.tense);
        break;
    }
    switch (this.subClass) {
      case ATTRIBUTIVE:
        inflections
            .add(PropertyObjectPair.get(LexinfoOnt.partOfSpeech, OliaOnt.AttributiveAdjective));
        break;
      case PREDICATIVE:
        inflections
            .add(PropertyObjectPair.get(LexinfoOnt.partOfSpeech, OliaOnt.PredicativeAdjective));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for subClass", this.subClass);
        break;
    }
    switch (this.function) {
      case ADVERBIAL:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasSyntacticFunction, OliaOnt.Adverbial));
        break;
      case NOTHING:
        break;
      default:
        log.debug("Unexpected value {} for Syntactic function", this.function);
        break;
    }


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
      case PAST_PARTICIPLE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.PastParticiple));
        break;
      case PRESENT_PARTICIPLE:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.PresentParticiple));
        break;
      case INFINITIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.Infinitive)); // TODO:
        // Infinitive
        // is a part
        // of speech,
        // not a
        // mood...
        break;
      case GERUNDIVUM:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.AdverbialParticiple));
        // TODO: AdverbialParticiple is a part of speech, not a mood...
        break;
      case ZU_INFINITIV:
        inflections.add(PropertyObjectPair.get(OliaOnt.hasMood, OliaOnt.Infinitive));
        note.add("Zu-Infinitive"); // TODO:
        // Infinitive
        // is a part
        // of speech,
        // not a
        // mood...

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
