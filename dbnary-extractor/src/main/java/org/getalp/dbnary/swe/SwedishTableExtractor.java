package org.getalp.dbnary.swe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.getalp.dbnary.morphology.TableExtractor;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwedishTableExtractor extends TableExtractor {

  private Logger log = LoggerFactory.getLogger(SwedishTableExtractor.class);

  public SwedishTableExtractor() {
    super();
  }

  @Override
  protected List<String> getRowAndColumnContext(int nrow, int ncol,
      ArrayMatrix<Element> columnHeaders) {
    List<String> rowAndColumnContext = super.getRowAndColumnContext(nrow, ncol, columnHeaders);
    for (int i = 0; i < ncol; i++) {
      addToContext(columnHeaders, nrow, i, rowAndColumnContext);
    }

    // Collect all bold header cells that are supposed to apply to all cells.
    for (int i = 0; i < nrow; i++) {
      for (int j = 0; j < ncol; j++) {
        Element headerCell = columnHeaders.get(i, j);
        if (headerCell != null) {
          String clazz = headerCell.attr("class");
          if (clazz.trim().equals("main")) {
            // Headers with class "main" are global headers applying to all cells.
            rowAndColumnContext.add(headerCell.text());
          }
        }
      }
    }
    rowAndColumnContext =
        rowAndColumnContext.stream().map(c -> c.startsWith("|") ? c.substring(1) : c)
            .map(c -> c.toLowerCase().startsWith("böjningar av") ? c.replaceAll(" ", "_") : c)
            .map(c -> c.toLowerCase().startsWith("kompareras inte") ? c.replaceAll(" ", "_") : c)
            .map(c -> c.toLowerCase().endsWith("pronomen") ? c.replaceAll(" ", "_") : c)
            .map(c -> c.toLowerCase().startsWith("ackusativ /") ? c.replaceAll(" ", "_") : c)
            .flatMap(c -> Arrays.stream(c.split(" "))).filter(c -> c.trim().length() > 0)
            .collect(Collectors.toList());
    return rowAndColumnContext;
  }

  private static Map<String, Consumer<SwedishInflectionData>> actions = new HashMap<>();

  static {
    actions.put("singular", SwedishInflectionData::singular);
    actions.put("sing.", SwedishInflectionData::singular);
    actions.put("plural", SwedishInflectionData::plural);
    actions.put("neutrum", SwedishInflectionData::neutrum);
    actions.put("utrum", SwedishInflectionData::common);
    actions.put("maskulinum", SwedishInflectionData::masculine);
    actions.put("femininum", SwedishInflectionData::feminine);

    actions.put("nominativ", SwedishInflectionData::nominative);
    actions.put("genitiv", SwedishInflectionData::genitive);
    actions.put("obestämd", SwedishInflectionData::indefinite);
    actions.put("bestämd", SwedishInflectionData::definite);
    actions.put("oräknebart", SwedishInflectionData::uncountable);
    actions.put("aktiv", SwedishInflectionData::active);
    actions.put("passiv", SwedishInflectionData::passive);
    actions.put("infinitiv", SwedishInflectionData::infinitive);
    actions.put("presens", SwedishInflectionData::present);
    actions.put("preteritum", SwedishInflectionData::preterit);
    actions.put("supinum", SwedishInflectionData::supinum);
    actions.put("imperativ", SwedishInflectionData::imperative);

    actions.put("attributivt", SwedishInflectionData::attributive);
    actions.put("predikativt", SwedishInflectionData::predicative);
    actions.put("adverbavledning", SwedishInflectionData::adverbial);

    actions.put("positiv", SwedishInflectionData::positive);
    actions.put("komparativ", SwedishInflectionData::comparative);
    actions.put("superlativ", SwedishInflectionData::superlative);

    actions.put("ackusativ", SwedishInflectionData::accusative);
    actions.put("dativ", SwedishInflectionData::dative);

    actions.put("1:a", SwedishInflectionData::firstPerson);
    actions.put("2:a", SwedishInflectionData::secondPerson);
    actions.put("3:e", SwedishInflectionData::thirdPerson);

    actions.put("possessiva_pronomen", SwedishInflectionData::possessive);
    actions.put("reflexiva_pronomen", SwedishInflectionData::reflexive);
    actions.put("personliga_pronomen", SwedishInflectionData::personnal);
    actions.put("reflexiva_possessiva_pronomen", SwedishInflectionData::possessive);

    assert actions.keySet().stream().filter(s -> !s.toLowerCase().equals(s)).findFirst()
        .equals(Optional.empty());
  }

  @Override
  protected List<SwedishInflectionData> getInflectionDataFromCellContext(List<String> context) {
    List<SwedishInflectionData> inflections = new ArrayList<>();
    inflections.add(new SwedishInflectionData());

    context = context.stream().map(String::toLowerCase).collect(Collectors.toList());

    // remove unused context to avoid warning while debugging
    context.removeIf("person"::equals);
    context.removeIf("p."::equals);

    if (context.contains("ackusativ_/_dativ")) {
      // We should generate 2 inflection data, one accusative, the other dativ
      inflections.get(0).accusative();
      SwedishInflectionData dativInflection = new SwedishInflectionData();
      dativInflection.dative();
      inflections.add(dativInflection);
      context.removeIf("ackusativ_/_dativ"::equals);
    }

    context.stream().filter(s -> s.startsWith("böjningar_av"))
        .forEach(s -> inflections.forEach(i -> i.note(s.replaceAll("_", " "))));
    context.removeIf(s -> s.startsWith("böjningar_av"));
    if (context.contains("particip")) {
      if (context.contains("presens")) {
        inflections.forEach(SwedishInflectionData::presentParticiple);
        context.removeIf("presens"::equals);
      } else if (context.contains("perfekt")) {
        inflections.forEach(SwedishInflectionData::pastParticiple);
        context.removeIf("perfekt"::equals);
      }
      context.removeIf("particip"::equals);
      // The table context also contains Aktiv and Passiv as the headers
    }

    context.stream().map(String::toLowerCase)
        .map(s -> actions.getOrDefault(s,
            infl -> log.debug("Unused context value {} while extracting Swedish morphology in {}",
                s, currentEntry)))
        .forEach(a -> inflections.forEach(a::accept));

    return inflections;
  }

  @Override
  protected Set<String> getInflectedForms(Element cell) {
    Set<String> forms = super.getInflectedForms(cell);
    forms.removeIf(""::equals);
    forms.removeIf("-"::equals);
    forms.removeIf("–"::equals);
    forms.removeIf("—"::equals);
    return forms;
  }

  @Override
  protected boolean shouldProcessCell(Element cell) {
    // Ignore note cells in morphology tables
    String clazz = cell.attr("class");
    if ("note".equals(clazz)) {
      // The cell should be ignored and nested tables should be marked as already processed.
      Elements tables = cell.select("table");
      alreadyParsedTables.addAll(tables);
      return false;
    } else {
      return super.shouldProcessCell(cell);
    }
  }
}
