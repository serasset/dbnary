package org.getalp.dbnary.deu;

import static org.getalp.dbnary.deu.GermanInflectionData.Cas;
import static org.getalp.dbnary.deu.GermanInflectionData.GNumber;
import static org.getalp.dbnary.deu.GermanInflectionData.Genre;
import java.util.List;
import java.util.Locale;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.concurrent.Debug;


public class GermanSubstantiveDeklinationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private Logger log =
      LoggerFactory.getLogger(GermanSubstantiveDeklinationExtractorWikiModel.class);

  public GermanSubstantiveDeklinationExtractorWikiModel(IWiktionaryDataHandler wdh,
      WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh);
  }

  @Override
  protected GermanInflectionData getInflectionDataFromCellContext(List<String> context) {
    GermanInflectionData inflection = new GermanInflectionData();
    boolean isArticleColumn = false;
    boolean hasGender = false;
    String numberIndex = "";
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
              wdh.currentLexEntry());
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
    return inflection;
  }

  // brutal: get number by removing all non-numbers
  // "Plural 1" => "1"
  public String getNumberIndex(String str) {
    return str.replaceAll("[^0-9]+", "");
  }

}
