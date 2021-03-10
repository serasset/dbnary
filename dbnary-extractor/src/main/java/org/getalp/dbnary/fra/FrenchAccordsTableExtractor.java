package org.getalp.dbnary.fra;

import java.util.ArrayList;
import java.util.List;
import org.getalp.dbnary.fra.FrenchInflectionData.GNumber;
import org.getalp.dbnary.fra.FrenchInflectionData.Genre;
import org.getalp.dbnary.fra.FrenchInflectionData.Person;
import org.getalp.dbnary.morphology.InflectionData;
import org.getalp.dbnary.morphology.TableExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchAccordsTableExtractor extends TableExtractor {

  private final List<String> entryContext;
  private Logger log = LoggerFactory.getLogger(FrenchAccordsTableExtractor.class);

  public FrenchAccordsTableExtractor(String currentEntry, List<String> context) {
    super(currentEntry);
    this.entryContext = context;
  }

  @Override
  protected List<InflectionData> getInflectionDataFromCellContext(List<String> context) {
    if (null != entryContext) {
      context.addAll(0, entryContext);
    }
    FrenchInflectionData inflection = new FrenchInflectionData();
    boolean hasGender = false;
    for (String h : context) {
      h = h.trim();
      switch (h) {
        case "Singulier":
        case "singulier":
        case "Au singulier":
        case "Au singulier uniquement":
        case "au singulier uniquement":
        case "Singulier uniquement":
        case "singulier uniquement":
        case "Singulier invariable":
        case "Singulare tantum":
        case "singulare tantum":
        case "Uniquement au singulier":
          inflection.number = GNumber.SINGULIER;
          break;
        case "Pluriel":
        case "pluriel":
        case "Au pluriel":
        case "Au pluriel uniquement":
        case "au pluriel uniquement":
        case "Plurale tantum":
        case "plurale tantum":
        case "Sigle (au pluriel)":
        case "Nom propre au pluriel":
          inflection.number = GNumber.PLURIEL;
          break;
        case "Singulier et pluriel":
        case "Singulier ou pluriel":
        case "singulier ou pluriel":
        case "Singulier et pluriel identiques":
          inflection.number = GNumber.SINGULIER_ET_PLURIEL;
          break;
        case "Indénombrable":
        case "indénombrable":
        case "Singulare tantum, indénombrable":
          inflection.number = GNumber.INDENOMBRABLE;
          break;
        case "Masculin":
        case "masculin":
          inflection.genre = Genre.MASCULIN;
          hasGender = true;
          break;
        case "Masculin singulier":
        case "masculin singulier":
        case "Masculin singulier invariable":
          inflection.genre = Genre.MASCULIN;
          inflection.number = GNumber.SINGULIER;
          hasGender = true;
          break;
        case "Masculin pluriel":
        case "masculin pluriel":
          inflection.genre = Genre.MASCULIN;
          inflection.number = GNumber.PLURIEL;
          hasGender = true;
          break;
        case "Féminin":
        case "féminin":
          inflection.genre = Genre.FEMININ;
          hasGender = true;
          break;
        case "Féminin pluriel":
        case "féminin pluriel":
          inflection.genre = Genre.FEMININ;
          inflection.number = GNumber.PLURIEL;
          hasGender = true;
          break;
        case "Féminin singulier":
        case "féminin singulier":
        case "Féminin singulier invariable":
        case "Féminin singulier, invariable":
          inflection.genre = Genre.FEMININ;
          inflection.number = GNumber.SINGULIER;
          hasGender = true;
          break;
        case "Masculin et féminin":
          inflection.genre = Genre.MASCULIN_ET_FEMININ;
          hasGender = true;
          break;
        case "1e personne":
          inflection.person = Person.PREMIÈRE;
          break;
        case "2e personne":
          inflection.person = Person.SECONDE;
          break;
        case "3e personne":
          inflection.person = Person.TROISIÈME;
          break;
        case "—":
        case "":
        case " ":
        case "Acronyme":
        case "acronyme":
        case "Adjectif":
        case "Adjectif invariable":
        case "Abréviation":
        case "Adverbe":
        case "adverbe":
        case "Locution adverbiale":
        case "Locution verbale":
        case "Adverbe de temps":
        case "Conjonction":
        case "Interjection":
        case "Invariable":
        case "invariable":
        case "Nom":
        case "Nom invariable":
        case "Nom propre":
        case "Nom Propre":
        case "Nom propre, acronyme":
        case "nom propre":
        case "Nom de famille":
        case "Nom famille":
        case "Sigle":
        case "sigle":
        case "Prénom":
        case "Préposition":
        case "préposition":
        case "Pronom":
        case "Pronom indéfini":
        case "Nom commun":
        case "Onomatopée":
        case "Patronyme":
        case "Locution-phrase":
        case "Locution interjective":
          // these mean invariable... Don't register anything
          break;
        default:
          log.debug("Unhandled header {} in {}", h, currentEntry);
      }
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
}
