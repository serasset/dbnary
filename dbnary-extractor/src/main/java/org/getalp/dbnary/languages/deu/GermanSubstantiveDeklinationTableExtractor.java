package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.List;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Cas;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.morphology.InflectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanSubstantiveDeklinationTableExtractor extends GermanTableExtractor {
  private Logger log = LoggerFactory.getLogger(GermanSubstantiveDeklinationTableExtractor.class);

  public GermanSubstantiveDeklinationTableExtractor() {
    super();
  }

  @Override
  protected List<InflectionData> getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    boolean isArticleColumn = false;
    boolean hasGender = false;
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
            inflection.note.add("number:" + h);
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
          log.debug("h={} numberIndex={}", h, numberIndex);
          if (!numberIndex.isEmpty()) {
            inflection.note.add("number:" + h);
          }
          break;
        case "Maskulinum":
          inflection.genre = Genre.MASCULIN;
          hasGender = true;
          break;
        case "Femininum":
          inflection.genre = Genre.FEMININ;
          hasGender = true;
          break;
        case "Neutrum":
          inflection.genre = Genre.NEUTRUM;
          hasGender = true;
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
    // check whether Substantive does have a gender - warn otherwise
    // TODO: not quite sure what to do. One possibility is to guess Gender from the article
    if (!hasGender) {
      log.debug("Warning: no gender in Substantive entry.");
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
