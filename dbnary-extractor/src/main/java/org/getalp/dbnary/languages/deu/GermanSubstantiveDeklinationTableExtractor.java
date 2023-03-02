package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.List;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Cas;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.morphology.InflectionData;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanSubstantiveDeklinationTableExtractor extends GermanTableExtractor {
  private final Logger log =
      LoggerFactory.getLogger(GermanSubstantiveDeklinationTableExtractor.class);

  public GermanSubstantiveDeklinationTableExtractor() {
    super();
  }

  @Override
  public InflectedFormSet parseHTML(String htmlCode, String pagename) {
    return super.parseHTML(htmlCode, pagename);
  }

  @Override
  protected boolean isHeaderCell(Element cell) {
    // German table building has a bug generating a td cell instead of a th cell in case of
    // different plurals
    if (cell.tagName().equalsIgnoreCase("td")) {
      Elements anchors = cell.select("a");
      if (anchors.stream().anyMatch(a -> a.attr("href").startsWith("/Hilfe:")))
        return true;
    }
    return super.isHeaderCell(cell);
  }

  @Override
  protected List<InflectionData> getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    boolean isArticleColumn = false;
    String numberIndex;
    for (String h : context) {
      h = h.trim();
      switch (h) {
        case "Singular":
          inflection.number = GNumber.SINGULAR;
          break;
        case "Singular 1":
        case "Singular 2":
        case "Singular 3":
        case "Singular 4":
          // TODO: for Morphisto we would need to "bundle" the Plurals according to their index
          // (1-4)
          inflection.number = GNumber.SINGULAR;
          numberIndex = getNumberIndex(h);
          if (!numberIndex.isEmpty()) {
            inflection.note.add("number:" + numberIndex);
          }
          break;
        case "Singular m":
          inflection.number = GNumber.SINGULAR;
          inflection.genre = Genre.MASCULIN;
          break;
        case "Singular f":
          inflection.number = GNumber.SINGULAR;
          inflection.genre = Genre.FEMININ;
          break;
        case "Plural":
          inflection.number = GNumber.PLURAL;
          break;

        case "Plural 1":
        case "Plural 2":
        case "Plural 3":
        case "Plural 4":
          // TODO: for Morphisto we would need to "bundle" the Plurals according to their index
          // (1-4)
          inflection.number = GNumber.PLURAL;
          numberIndex = getNumberIndex(h);
          // log.debug("h={} numberIndex={}", h, numberIndex);
          if (!numberIndex.isEmpty()) {
            inflection.note.add("number:" + numberIndex);
          }
          break;
        case "Maskulinum":
          inflection.genre = Genre.MASCULIN;
          break;
        case "Femininum":
          inflection.genre = Genre.FEMININ;
          break;
        case "Neutrum":
          inflection.genre = Genre.NEUTRUM;
          break;
        case "Artikel":
          isArticleColumn = true;
          break;
        case "Wortform":
          isArticleColumn = false;
          break;
        case "Nominativ":
          inflection.cas = Cas.NOMINATIF;
          break;
        case "Genitiv":
          inflection.cas = Cas.GENITIF;
          break;
        case "Dativ":
          inflection.cas = Cas.DATIF;
          break;
        case "Akkusativ":
          inflection.cas = Cas.ACCUSATIF;
          break;
        case "—":
        case "":
        case " ":
          break;
        default:
          log.debug("Substantiv Deklination Extraction: Unhandled header {} in {}", h,
              currentEntry);
      }
    }
    if (isArticleColumn) {
      return null;
    }
    List<InflectionData> inflections = new ArrayList<>();
    inflections.add(inflection);
    return inflections;
  }

  // brutal: get number by removing all non-numbers
  // "Plural 1" => "1"
  public String getNumberIndex(String str) {
    return str.replaceAll("[^0-9]+", "");
  }

}
