package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Mode;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Person;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Tense;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Valency;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Voice;
import org.getalp.dbnary.morphology.InflectionData;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanKonjugationTableExtractor extends GermanTableExtractor {
  private final Logger log = LoggerFactory.getLogger(GermanKonjugationTableExtractor.class);

  public GermanKonjugationTableExtractor() {
    super();
  }


  @Override
  protected boolean isNormalCell(Element cell) {
    return cell.tagName().equalsIgnoreCase("td");
  }

  @Override
  protected boolean isHeaderCell(Element cell) {
    if (cell.tagName().equalsIgnoreCase("th")) {
      return true;
    }
    if (cell.tagName().equalsIgnoreCase("td")) {
      // Test if background color is grey.
      String color = getBackgroundColor(cell);
      if (color != null && (color.equalsIgnoreCase("#F4F4F4") || color.equalsIgnoreCase("#DEDEDE")
          || color.equalsIgnoreCase("#C1C1C1") || color.equalsIgnoreCase("#CCCCFF"))) {
        return true;
      }
      // Test special cases when no gender/number metadata is given, but pronouns (ich, du, etc. are
      // given)
      if (!cell.select("i").isEmpty()) {
        String text = cell.text();
        return text.equalsIgnoreCase("ich") || text.equalsIgnoreCase("du")
            || text.equalsIgnoreCase("er") || text.equalsIgnoreCase("wir")
            || text.equalsIgnoreCase("ihr") || text.equalsIgnoreCase("sie");
      } else {
        return false;
      }
    }
    return false;
  }

  @Override
  protected List<InflectionData> getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    for (String h : context) {
      h = h.trim();
      if (h.startsWith("Gerundivum")) {
        h = "Gerundivum";
      }
      if ("transitive Verwendung".equals(h) || "transitive".equals(h)) {
        inflection.valency = Valency.TRANSITIVE;
      } else if ("reflexive Verwendung".equals(h) || "reflexiv".equals(h)) {
        inflection.voice = Voice.REFLEXIV;
      } else if ("intransitive".equals(h)) {
        inflection.valency = Valency.INTRANSITIVE;
      } else if ("(nichterweiterte) Infinitive".equals(h) || "nichterweitert".equals(h)
          || "Infinitive".equals(h)) {
        inflection.mode = Mode.INFINITIV;
      } else if ("erweiterte Infinitive".equals(h) || "erweitert".equals(h)) {
        inflection.mode = Mode.ZU_INFINITIV;
      } else if ("Partizipien".equals(h)) {
        inflection.mode = Mode.PARTIZIPIEN;
      } else if ("Infinitiv Präsens".equals(h)) {
        inflection.mode = Mode.INFINITIV;
        inflection.tense = Tense.PRÄSENS;
      } else if ("Infinitiv Perfekt".equals(h)) {
        inflection.mode = Mode.INFINITIV;
        inflection.tense = Tense.PERFEKT;
      } else if ("Infinitiv Futur I".equals(h)) {
        inflection.mode = Mode.INFINITIV;
        inflection.tense = Tense.FUTURE1;
      } else if ("Infinitiv Futur II".equals(h)) {
        inflection.mode = Mode.INFINITIV;
        inflection.tense = Tense.FUTURE2;
      } else if ("Partizip Präsens".equals(h)) {
        inflection.mode = Mode.PARTIZIPIEN;
        inflection.tense = Tense.PRÄSENS;
      } else if ("Partizip Perfekt".equals(h)) {
        inflection.mode = Mode.PARTIZIPIEN;
        inflection.tense = Tense.PRÄSENS;
      } else if ("Aktiv".equals(h)) {
        inflection.voice = Voice.AKTIV;
      } else if ("Vorgangspassiv".equals(h)) {
        inflection.voice = Voice.VORGANGSPASSIV;
      } else if ("Zustandspassiv".equals(h)) {
        inflection.voice = Voice.ZUSTANDSPASSIV;
      } else if ("Präsens Aktiv".equals(h)) {
        inflection.tense = Tense.PRÄSENS;
        inflection.voice = Voice.AKTIV;
      } else if ("Perfekt Passiv".equals(h)) {
        inflection.tense = Tense.PERFEKT;
        inflection.voice = Voice.PASSIV;
      } else if ("Gerundivum".equals(h)) { // WARN: title contains 2 lines : "Nur attributive
                                           // Verwendung"
        inflection.mode = Mode.GERUNDIVUM;
      } else if ("Imperative".equals(h)) {
        inflection.mode = Mode.IMPERATIV;
      } else if ("Präsens Vorgangspassiv".equals(h)) {
        inflection.tense = Tense.PRÄSENS;
        inflection.voice = Voice.VORGANGSPASSIV;
      } else if ("Präsens Zustandspassiv".equals(h)) {
        inflection.tense = Tense.PRÄSENS;
        inflection.voice = Voice.ZUSTANDSPASSIV;
      } else if ("Perfekt Aktiv".equals(h)) {
        inflection.tense = Tense.PERFEKT;
        inflection.voice = Voice.AKTIV;
      } else if ("Perfekt Vorgangspassiv".equals(h)) {
        inflection.tense = Tense.PERFEKT;
        inflection.voice = Voice.VORGANGSPASSIV;
      } else if ("Perfekt Zustandspassiv".equals(h)) {
        inflection.tense = Tense.PERFEKT;
        inflection.voice = Voice.ZUSTANDSPASSIV;
      } else if ("Zustandsreflexiv".equals(h)) {
        inflection.voice = Voice.ZUSTANDSREFLEXIVEPASSIV;
      } else if ("Höflichkeitsform".equals(h)) {
        inflection.person = Person.HÖFLICHKEITSFORM;
      } else if ("Person".equals(h)) {
      } else if ("Hauptsatzkonjugation".equals(h) || "Nebensatzkonjugation".equals(h)) {
        inflection.note.add(h);
      } else if ("Indikativ".equals(h)) {
        inflection.mode = Mode.INDICATIV;
      } else if ("Konjunktiv I".equals(h)) {
        inflection.mode = Mode.KONJUNKTIV1;
      } else if ("Konjunktiv II".equals(h)) {
        inflection.mode = Mode.KONJUNKTIV2;
        // Gender/number
      } else if ("1. Person Singular".equals(h) || "Sg. 1. Pers.".equals(h) || "ich".equals(h)) {
        inflection.number = GNumber.SINGULAR;
        inflection.person = Person.FIRST;
      } else if ("2. Person Singular".equals(h) || "Sg. 2. Pers.".equals(h) || "(du)".equals(h)
          || "du".equals(h)) {
        inflection.number = GNumber.SINGULAR;
        inflection.person = Person.SECOND;
      } else if ("3. Person Singular".equals(h) || "Sg. 3. Pers.".equals(h) || "er".equals(h)) {
        inflection.number = GNumber.SINGULAR;
        inflection.person = Person.THIRD;
      } else if ("1. Person Plural".equals(h) || "Pl. 1. Pers.".equals(h) || "wir".equals(h)) {
        inflection.number = GNumber.PLURAL;
        inflection.person = Person.FIRST;
      } else if ("2. Person Plural".equals(h) || "Pl. 2. Pers.".equals(h) || "(ihr)".equals(h)
          || "ihr".equals(h)) {
        inflection.number = GNumber.PLURAL;
        inflection.person = Person.SECOND;
      } else if ("3. Person Plural".equals(h) || "Pl. 3. Pers.".equals(h) || "sie".equals(h)) {
        inflection.number = GNumber.PLURAL;
        inflection.person = Person.THIRD;
        // Tenses
      } else if ("Präsens".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.PRÄSENS;
      } else if ("Präteritum".equals(h) || "Präteritum (Imperfekt)".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.PRÄTERITUM;
      } else if ("Perfekt".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.PERFEKT;
      } else if ("Plusquamperfekt".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.PLUSQUAMPERFEKT;
      } else if ("Futur I".equals(h) || "Futur I.".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.FUTURE1;
      } else if ("Futur II".equals(h) || "Futur II.".equals(h)) {
        inflection.note.clear();
        inflection.tense = Tense.FUTURE2;
      } else if ("Text".equals(h)) {
        inflection.note.clear();
      } else if (h.startsWith("Hilfsverb")) {// TODO: how do I represent the hilfsverbs ?
      } else if ("Infinitive und Partizipien".equals(h) || "Finite Formen".equals(h)) {// comes from
                                                                                       // a <h4>
                                                                                       // tag: just
                                                                                       // add as a
                                                                                       // note
        inflection.note.add(h);
      } else if ("—".equals(h) || "".equals(h) || " ".equals(h)) {
      } else if ("Flexion der Verbaladjektive".equals(h)) {// This table header is the last one and
                                                           // cells under it should be ignored.
        return null;
      } else {
        log.debug("Deklination Extraction: Unhandled header {} in {}", h, currentEntry);
      }
    }
    List<InflectionData> inflections = new ArrayList<>();
    inflections.add(inflection);
    return inflections;
  }

  private static final Pattern reflexive = Pattern.compile("\\breflexiv\\b");

  @Override
  protected Collection<? extends String> decodeH2Context(String text) {
    LinkedList<String> res = new LinkedList<>();
    if (reflexive.matcher(text).find()) {
      res.add("Reflexiv");
    }
    return res;
  }

}
