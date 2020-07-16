package org.getalp.dbnary.swe;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.morphology.TableExtractor;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    List<String> rowAndColumnContext  = new LinkedList<>();
    boolean previousRowHasHeader = false;
    for (int i = nrow - 1; i >= 0; i--) {
      Element headerCell = columnHeaders.get(i, ncol);
      String header;
      if (null != headerCell && (header = headerCell.text()) != null && header.trim().length() != 0) {
        rowAndColumnContext.add(header);
        previousRowHasHeader = true;
      } else {
        // Current row contains a value, break if the previous contained a header
        // so that higher row's headers are voided.
        if (previousRowHasHeader) break;
      }
    }
    for (int i = 0; i < ncol; i++) {
      addToContext(columnHeaders, nrow, i, rowAndColumnContext);
    }

    // Collect all bold header cells that are supposed to apply to all cells.
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
    rowAndColumnContext = rowAndColumnContext.stream()
        .map(c -> c.startsWith("|") ? c.substring(1) : c)
        .map(c -> c.toLowerCase().startsWith("böjningar av") ? c.replaceAll(" ", "_") : c)
        .map(c -> c.toLowerCase().startsWith("kompareras inte") ? c.replaceAll(" ", "_") : c)
        .flatMap(c -> Arrays.stream(c.split(" ")))
        .filter(c -> c.trim().length() > 0)
        .collect(Collectors.toList());
    return rowAndColumnContext;
  }

  private static Map<String, Consumer<SwedishInflectionData>> actions = new HashMap<>();

  static {
    actions.put("singular", SwedishInflectionData::singular);
    actions.put("plural", SwedishInflectionData::plural);
    actions.put("neutrum", SwedishInflectionData::neutrum);
    actions.put("utrum", SwedishInflectionData::common);
    actions.put("maskulinum", SwedishInflectionData::masculine);


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

    assert actions.keySet().stream().filter(s -> !s.toLowerCase().equals(s)).findFirst()
        .equals(Optional.empty());
  }

  @Override
  protected SwedishInflectionData getInflectionDataFromCellContext(List<String> context) {
    SwedishInflectionData inflection = new SwedishInflectionData();
    context = context.stream().map(String::toLowerCase).collect(Collectors.toList());

    context.stream().filter(s -> s.startsWith("böjningar_av")).forEach(s -> inflection.note(s.replaceAll("_", " ")));
    context.removeIf(s -> s.startsWith("böjningar_av"));
    if (context.contains("particip")) {
      if (context.contains("presens")) {
        inflection.presentParticiple();
        context.removeIf(c -> "presens".equals(c));
      } else if (context.contains("perfekt")) {
        inflection.pastParticiple();
        context.removeIf(c -> "perfekt".equals(c));
      }
      context.removeIf(c -> "particip".equals(c));
      // The table context also contains Aktiv and Passiv as the headers
    }



    context.stream().map(String::toLowerCase)
        .map(s -> actions.getOrDefault(s,
            infl -> log.debug("Unused context value {} while extracting Swedish morphology in {}",
                s, currentEntry)))
        .forEach(a -> a.accept(inflection));

    return inflection;
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
      for (Element nestedTable : tables) {
        alreadyParsedTables.add(nestedTable);
      }
      return false;
    } else {
      return super.shouldProcessCell(cell);
    }
  }
}
