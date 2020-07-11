package org.getalp.dbnary.swe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.getalp.dbnary.morphology.TableExtractor;
import org.getalp.dbnary.swe.SwedishInflectionData.Definiteness;
import org.getalp.dbnary.swe.SwedishInflectionData.Degree;
import org.getalp.dbnary.swe.SwedishInflectionData.GNumber;
import org.getalp.dbnary.swe.SwedishInflectionData.Gender;
import org.getalp.dbnary.swe.SwedishInflectionData.GrammaticalCase;
import org.getalp.dbnary.swe.SwedishInflectionData.InflectionType;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwedishTableExtractor extends TableExtractor {

  private Logger log = LoggerFactory.getLogger(SwedishTableExtractor.class);

  public SwedishTableExtractor(String currentEntry) {
    super(currentEntry);
  }

  @Override
  protected List<String> getRowAndColumnContext(int nrow, int ncol,
      ArrayMatrix<Element> columnHeaders) {
    List<String> rowAndColumnContext = super.getRowAndColumnContext(nrow, ncol, columnHeaders);

    for (int i = 0; i < nrow; i++) {
      for (int j = 0; j < ncol; j++) {
        Element headerCell = columnHeaders.get(i, j);
        if (headerCell != null) {
          String clazz = headerCell.attr("class");
          if (null != clazz && (clazz.trim().equals("main"))) {
            // Headers with class "main" are global headers applying to all cells.
            rowAndColumnContext.add(headerCell.text());
          }
        }
      }
    }
    return rowAndColumnContext;
  }

  private static Map<String, Consumer<SwedishInflectionData>> actions = new HashMap<>();

  static {
    actions.put("singular", SwedishInflectionData::singular);
    actions.put("plural", SwedishInflectionData::plural);
    actions.put("neutrum", SwedishInflectionData::neutrum);
    actions.put("utrum", SwedishInflectionData::common);
    actions.put("nominativ", SwedishInflectionData::nominative);
    actions.put("genitiv", SwedishInflectionData::genitive);
    actions.put("obestämd", SwedishInflectionData::indefinite);
    actions.put("bestämd", SwedishInflectionData::definite);

    assert actions.keySet().stream().filter(s -> !s.toLowerCase().equals(s)).findFirst()
        .equals(Optional.empty());
  }

  @Override
  protected SwedishInflectionData getInflectionDataFromCellContext(List<String> context) {
    SwedishInflectionData inflection = new SwedishInflectionData();

    context.stream().map(String::toLowerCase).filter(s -> s.startsWith("böjningar av"))
        .forEach(s -> inflection.note(s));
    context.removeIf(s -> s.toLowerCase().startsWith("böjningar av"));
    context.stream().map(String::toLowerCase)
        .map(s -> actions.getOrDefault(s,
            infl -> log.debug("Unused context value {} while extracting Swedish morphology in {}",
                s, currentEntry)))
        .forEach(a -> a.accept(inflection));

    return inflection;
  }

  protected SwedishInflectionData getInflectionDataFromCellContextOld(List<String> context) {
    SwedishInflectionData inflection = new SwedishInflectionData();
    for (String h : context) {
      h = h.trim();
      switch (h) {
        case "Singular":
          inflection.number = GNumber.SINGULAR;
          break;
        case "Plural":
          inflection.number = GNumber.PLURAL;
          break;
        case "Neutrum":
          inflection.gender = Gender.NEUTRUM;
          break;
        case "Nominativ":
          inflection.grammaticalCase = GrammaticalCase.NOMINATIVE;
          break;
        case "Genitiv":
          inflection.grammaticalCase = GrammaticalCase.GENITIVE;
          break;
        case "Obestämd":
          inflection.definiteness = Definiteness.INDEFINITE;
          break;
        case "Bestämd":
          inflection.definiteness = Definiteness.DEFINITE;
          break;

        case "Positiv":
          inflection.degree = Degree.POSITIVE;
          break;
        case "Komparativ":
          inflection.degree = Degree.COMPARATIVE;
          break;
        case "Superlativ":
          inflection.degree = Degree.SUPERLATIVE;
          break;
        case "Starke Deklination":
          inflection.inflectionType = InflectionType.STRONG;
          break;
        case "Schwache Deklination":
          inflection.inflectionType = InflectionType.WEAK;
          break;
        case "Gemischte Deklination":
          inflection.inflectionType = InflectionType.MIXED;
          break;
        case "Prädikativ":
          inflection.inflectionType = InflectionType.NOTHING;
          inflection.note.add("Prädikativ");
          break;
        case "Maskulinum":
          inflection.gender = Gender.MASCULIN;
          break;
        case "Femininum":
          inflection.gender = Gender.FEMININ;
          break;
        case "Dativ":
          inflection.grammaticalCase = GrammaticalCase.DATIVE;
          break;
        case "Akkusativ":
          inflection.grammaticalCase = GrammaticalCase.ACCUSATIVE;
          break;
        case "—":
        case "":
        case " ":
          break;
        default:
          log.debug("Swedish Inflection Extraction: Unhandled header {} in {}", h, currentEntry);
      }
    }

    return inflection;
  }

  @Override
  protected Set<String> getInflectedForms(Element cell) {
    // there are cells with <br> and commas to separate different values: split them
    // get rid of spurious html-formatting (<nbsp> <small> <i> etc.)
    Set<String> forms = new HashSet<>();
    forms.add(cell.text());

    for (String form : forms) {
      if (form.contains(",")) {
        log.trace("Comma found in morphological value: {}", form);
      }
    }
    return forms;
  }
}
