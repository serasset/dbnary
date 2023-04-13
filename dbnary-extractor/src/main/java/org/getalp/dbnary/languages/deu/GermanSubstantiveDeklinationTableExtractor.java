package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.List;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Cas;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.languages.deu.GermanInflectionData.InflectionType;
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
      String style = cell.attr("style").trim();
      if (style.contains("font-weight:bold"))
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
      if ("Singular".equals(h)) {
        inflection.number = GNumber.SINGULAR;
      } else if ("Singular 1".equals(h) || "Singular 2".equals(h) || "Singular 3".equals(h)
          || "Singular 4".equals(h)) {
        // TODO: for Morphisto we would need to "bundle" the Plurals according to their index
        // (1-4)
        inflection.number = GNumber.SINGULAR;
        numberIndex = getNumberIndex(h);
        if (!numberIndex.isEmpty()) {
          inflection.note.add("number:" + numberIndex);
        }
      } else if ("Singular m".equals(h)) {
        inflection.number = GNumber.SINGULAR;
        inflection.genre = Genre.MASCULIN;
      } else if ("Singular f".equals(h)) {
        inflection.number = GNumber.SINGULAR;
        inflection.genre = Genre.FEMININ;
      } else if ("Plural".equals(h)) {
        inflection.number = GNumber.PLURAL;
      } else if ("Plural 1".equals(h) || "Plural 2".equals(h) || "Plural 3".equals(h)
          || "Plural 4".equals(h)) {
        // TODO: for Morphisto we would need to "bundle" the Plurals according to their index
        // (1-4)
        inflection.number = GNumber.PLURAL;
        numberIndex = getNumberIndex(h);
        // log.debug("h={} numberIndex={}", h, numberIndex);
        if (!numberIndex.isEmpty()) {
          inflection.note.add("number:" + numberIndex);
        }
      } else if ("Maskulinum".equals(h)) {
        inflection.genre = Genre.MASCULIN;
      } else if ("Femininum".equals(h)) {
        inflection.genre = Genre.FEMININ;
      } else if ("Neutrum".equals(h)) {
        inflection.genre = Genre.NEUTRUM;
      } else if ("Artikel".equals(h)) {
        isArticleColumn = true;
      } else if ("Wortform".equals(h)) {
        isArticleColumn = false;
      } else if ("Nominativ".equals(h)) {
        inflection.cas = Cas.NOMINATIF;
      } else if ("Genitiv".equals(h)) {
        inflection.cas = Cas.GENITIF;
      } else if ("Dativ".equals(h)) {
        inflection.cas = Cas.DATIF;
      } else if ("Akkusativ".equals(h)) {
        inflection.cas = Cas.ACCUSATIF;
      } else if (h.toLowerCase().contains("starke deklination")) {
        inflection.inflectionType = InflectionType.STRONG;
      } else if (h.toLowerCase().contains("schwache deklination")) {
        inflection.inflectionType = InflectionType.WEAK;
      } else if (h.toLowerCase().contains("gemischte deklination")) {
        inflection.inflectionType = InflectionType.MIXED;
      } else if ("—".equals(h) || "".equals(h) || " ".equals(h)) {
      } else {
        log.debug("Substantiv Deklination Extraction: Unhandled header {} in {}", h, currentEntry);
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
