package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Cas;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Degree;
import org.getalp.dbnary.languages.deu.GermanInflectionData.GNumber;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.languages.deu.GermanInflectionData.InflectionType;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Mode;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Person;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Tense;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Valency;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Voice;
import org.getalp.dbnary.morphology.InflectionData;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanInPageKonjugationTableExtractor extends GermanKonjugationTableExtractor {

  private Logger log = LoggerFactory.getLogger(GermanInPageKonjugationTableExtractor.class);

  public GermanInPageKonjugationTableExtractor() {
    super();
  }

  Set<String> significativeValues =
      Set.of("singular", "plural", "ich", "du", "er, sie, es", "wir", "ihr", "sie");

  protected boolean isHeaderCell(Element cell) {
    // Ignore note cells in morphology tables
    if (super.isHeaderCell(cell)) {
      return true;
    } else if (!cell.select("small").isEmpty() || "text-align:right".equals(cell.attr("style"))) {
      String ctext = cell.text().trim().toLowerCase();
      if (significativeValues.contains(ctext)) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
}
