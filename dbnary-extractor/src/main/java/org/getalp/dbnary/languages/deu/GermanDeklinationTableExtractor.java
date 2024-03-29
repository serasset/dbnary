package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Cas;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Degree;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.languages.deu.GermanInflectionData.InflectionType;
import org.getalp.dbnary.morphology.InflectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanDeklinationTableExtractor extends GermanTableExtractor {
  private Logger log = LoggerFactory.getLogger(GermanDeklinationTableExtractor.class);

  public GermanDeklinationTableExtractor() {
    super();
  }

  @Override
  protected List<InflectionData> getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    boolean isArticleColumn = false;
    // log.debug("== getInflectionDataFromCellContext for {} ==", wdh.currentLexEntry());
    for (String h : context) {
      h = h.trim();
      // log.debug(" h = {}", h);
      switch (h) {
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
        case "Singular":
          inflection.number = GNumber.SINGULAR;
          break;
        case "Plural":
          inflection.number = GNumber.PLURAL;
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
          log.debug("Deklination Extraction: Unhandled header {} in {}", h, currentEntry);
      }
    }
    if (isArticleColumn) {
      return null;
    }
    List<InflectionData> inflections = new ArrayList<>();
    inflections.add(inflection);
    return inflections;
  }

  private static HashSet<String> declinatedFormMarker;

  static {
    declinatedFormMarker = new HashSet<>();
    declinatedFormMarker.add("adjektivische Deklination");
  }

}
