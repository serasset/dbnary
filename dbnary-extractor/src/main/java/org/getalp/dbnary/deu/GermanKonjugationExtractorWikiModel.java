package org.getalp.dbnary.deu;

import static org.getalp.dbnary.deu.GermanInflectionData.GNumber;
import static org.getalp.dbnary.deu.GermanInflectionData.Mode;
import static org.getalp.dbnary.deu.GermanInflectionData.Person;
import static org.getalp.dbnary.deu.GermanInflectionData.Tense;
import static org.getalp.dbnary.deu.GermanInflectionData.Valency;
import static org.getalp.dbnary.deu.GermanInflectionData.Voice;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanKonjugationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log = LoggerFactory.getLogger(GermanKonjugationExtractorWikiModel.class);

  public GermanKonjugationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh);
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
  protected GermanInflectionData getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    boolean isArticleColumn = false;
    for (String h : context) {
      h = h.trim();
      if (h.startsWith("Gerundivum")) {
        h = "Gerundivum";
      }
      switch (h) {
        case "transitive Verwendung":
        case "transitive":
          inflection.valency = Valency.TRANSITIVE;
          break;
        case "reflexive Verwendung":
        case "reflexiv":
          inflection.voice = Voice.REFLEXIV;
          break;
        case "intransitive":
          inflection.valency = Valency.INTRANSITIVE;
          break;
        case "(nichterweiterte) Infinitive":
        case "nichterweitert":
        case "Infinitive":
          inflection.mode = Mode.INFINITIV;
          break;
        case "erweiterte Infinitive":
        case "erweitert":
          inflection.mode = Mode.ZU_INFINITIV;
          break;
        case "Partizipien":
          inflection.mode = Mode.PARTIZIPIEN;
          break;
        case "Infinitiv Präsens":
          inflection.mode = Mode.INFINITIV;
          inflection.tense = Tense.PRÄSENS;
          break;
        case "Infinitiv Perfekt":
          inflection.mode = Mode.INFINITIV;
          inflection.tense = Tense.PERFEKT;
          break;
        case "Infinitiv Futur I":
          inflection.mode = Mode.INFINITIV;
          inflection.tense = Tense.FUTURE1;
          break;
        case "Infinitiv Futur II":
          inflection.mode = Mode.INFINITIV;
          inflection.tense = Tense.FUTURE2;
          break;
        case "Partizip Präsens":
          inflection.mode = Mode.PARTIZIPIEN;
          inflection.tense = Tense.PRÄSENS;
          break;
        case "Partizip Perfekt":
          inflection.mode = Mode.PARTIZIPIEN;
          inflection.tense = Tense.PRÄSENS;
          break;
        case "Aktiv":
          inflection.voice = Voice.AKTIV;
          break;
        case "Vorgangspassiv":
          inflection.voice = Voice.VORGANGSPASSIV;
          break;
        case "Zustandspassiv":
          inflection.voice = Voice.ZUSTANDSPASSIV;
          break;
        case "Präsens Aktiv":
          inflection.tense = Tense.PRÄSENS;
          inflection.voice = Voice.AKTIV;
          break;
        case "Perfekt Passiv":
          inflection.tense = Tense.PERFEKT;
          inflection.voice = Voice.PASSIV;
          break;
        case "Gerundivum": // WARN: title contains 2 lines : "Nur attributive Verwendung"
          inflection.mode = Mode.GERUNDIVUM;
          break;
        case "Imperative":
          inflection.mode = Mode.IMPERATIV;
          break;
        case "Präsens Vorgangspassiv":
          inflection.tense = Tense.PRÄSENS;
          inflection.voice = Voice.VORGANGSPASSIV;
          break;
        case "Präsens Zustandspassiv":
          inflection.tense = Tense.PRÄSENS;
          inflection.voice = Voice.ZUSTANDSPASSIV;
          break;
        case "Perfekt Aktiv":
          inflection.tense = Tense.PERFEKT;
          inflection.voice = Voice.AKTIV;
          break;
        case "Perfekt Vorgangspassiv":
          inflection.tense = Tense.PERFEKT;
          inflection.voice = Voice.VORGANGSPASSIV;
          break;
        case "Perfekt Zustandspassiv":
          inflection.tense = Tense.PERFEKT;
          inflection.voice = Voice.ZUSTANDSPASSIV;
          break;
        case "Zustandsreflexiv":
          inflection.voice = Voice.ZUSTANDSREFLEXIVEPASSIV;
        case "Höflichkeitsform":
          inflection.person = Person.HÖFLICHKEITSFORM;
          break;
        case "Person":
          break;
        case "Hauptsatzkonjugation":
        case "Nebensatzkonjugation":
          inflection.note.add(h);
          break;
        case "Indikativ":
          inflection.mode = Mode.INDICATIV;
          break;
        case "Konjunktiv I":
          inflection.mode = Mode.KONJUNKTIV1;
          break;
        case "Konjunktiv II":
          inflection.mode = Mode.KONJUNKTIV2;
          break;
        // Gender/number
        case "1. Person Singular":
        case "Sg. 1. Pers.":
        case "ich":
          inflection.number = GNumber.SINGULAR;
          inflection.person = Person.FIRST;
          break;
        case "2. Person Singular":
        case "Sg. 2. Pers.":
        case "(du)":
        case "du":
          inflection.number = GNumber.SINGULAR;
          inflection.person = Person.SECOND;
          break;
        case "3. Person Singular":
        case "Sg. 3. Pers.":
        case "er":
          inflection.number = GNumber.SINGULAR;
          inflection.person = Person.THIRD;
          break;
        case "1. Person Plural":
        case "Pl. 1. Pers.":
        case "wir":
          inflection.number = GNumber.PLURAL;
          inflection.person = Person.FIRST;
          break;
        case "2. Person Plural":
        case "Pl. 2. Pers.":
        case "(ihr)":
        case "ihr":
          inflection.number = GNumber.PLURAL;
          inflection.person = Person.SECOND;
          break;
        case "3. Person Plural":
        case "Pl. 3. Pers.":
        case "sie":
          inflection.number = GNumber.PLURAL;
          inflection.person = Person.THIRD;
          break;
        // Tenses
        case "Präsens":
          inflection.note.clear();
          inflection.tense = Tense.PRÄSENS;
          break;
        case "Präteritum":
        case "Präteritum (Imperfekt)":
          inflection.note.clear();
          inflection.tense = Tense.PRÄTERITUM;
          break;
        case "Perfekt":
          inflection.note.clear();
          inflection.tense = Tense.PERFEKT;
          break;
        case "Plusquamperfekt":
          inflection.note.clear();
          inflection.tense = Tense.PLUSQUAMPERFEKT;
          break;
        case "Futur I":
        case "Futur I.":
          inflection.note.clear();
          inflection.tense = Tense.FUTURE1;
          break;
        case "Futur II":
        case "Futur II.":
          inflection.note.clear();
          inflection.tense = Tense.FUTURE2;
          break;
        case "Text":
          inflection.note.clear();
          break;
        case "Hilfsverb":
          // TODO: how do I represent the hilfsverbs ?
          break;
        case "Infinitive und Partizipien":
        case "Finite Formen":
          // comes from a <h4> tag: just add as a note
          inflection.note.add(h);
          break;
        case "—":
        case "":
        case " ":
          break;
        case "Flexion der Verbaladjektive":
          // This table header is the last one and cells under it should be ignored.
          return null;
        default:
          log.debug("Deklination Extraction: Unhandled header {} in {}", h, wdh.currentLexEntry());
      }
    }
    if (isArticleColumn) {
      return null;
    }
    return inflection;
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

  // Catch non German verb templates to avoid expanding them.
  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (templateName.contains("Niederländisch")) {
      log.debug("German Verb Conjugation Extraction: Ignoring template call: {}", templateName);
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

}
