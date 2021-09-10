package org.getalp.dbnary.fra.morphology;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.RefactoredTableExtractor;
import org.getalp.dbnary.morphology.RelaxInflexionScheme;
import org.getalp.model.lexinfo.Gender;
import org.getalp.model.lexinfo.Number;
import org.getalp.model.lexinfo.Person;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.PhoneticRepresentation;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchAccordsTableExtractor extends RefactoredTableExtractor {

  private Logger log = LoggerFactory.getLogger(FrenchAccordsTableExtractor.class);

  public FrenchAccordsTableExtractor(String currentEntry, String language, List<String> context) {
    super(currentEntry, language, context);
  }

  @Override
  protected InflectionScheme getInflectionSchemeFromContext(List<String> context) {
    InflectionScheme inflection = new RelaxInflexionScheme();
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
          inflection.add(Number.SINGULAR);
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
          inflection.add(Number.PLURAL);
          break;
        case "Singulier et pluriel":
        case "singulier et pluriel":
        case "Singulier ou pluriel":
        case "singulier ou pluriel":
        case "Singulier et pluriel identiques":
          inflection.add(Number.SINGULAR);
          inflection.add(Number.PLURAL);
          break;
        case "Indénombrable":
        case "indénombrable":
        case "Singulare tantum, indénombrable":
          // TODO: UNCOUNTABLE is not in LexInfo, see if we add olia uncountable (to the form) ?
          inflection.add(Number.SINGULAR);
          // inflection.add(Number.UNCOUNTABLE);
          break;
        case "Masculin":
        case "masculin":
          inflection.add(Gender.MASCULINE);
          hasGender = true;
          break;
        case "Masculin singulier":
        case "masculin singulier":
        case "Masculin singulier invariable":
          inflection.add(Gender.MASCULINE);
          inflection.add(Number.SINGULAR);
          hasGender = true;
          break;
        case "Masculin pluriel":
        case "masculin pluriel":
          inflection.add(Gender.MASCULINE);
          inflection.add(Number.PLURAL);
          hasGender = true;
          break;
        case "Féminin":
        case "féminin":
          inflection.add(Gender.FEMININE);
          hasGender = true;
          break;
        case "Féminin pluriel":
        case "féminin pluriel":
          inflection.add(Gender.FEMININE);
          inflection.add(Number.PLURAL);
          hasGender = true;
          break;
        case "Féminin singulier":
        case "féminin singulier":
        case "Féminin singulier invariable":
        case "Féminin singulier, invariable":
          inflection.add(Gender.FEMININE);
          inflection.add(Number.SINGULAR);
          hasGender = true;
          break;
        case "Masculin et féminin":
        case "Masculin et féminin identiques":
          inflection.add(Gender.FEMININE);
          inflection.add(Gender.MASCULINE);
          hasGender = true;
          break;
        case "1e personne":
          inflection.add(Person.FIRST);
          break;
        case "2e personne":
          inflection.add(Person.SECOND);
          break;
        case "3e personne":
          inflection.add(Person.THIRD);
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
          log.debug("Unhandled header {} in {}", h, this.entryName);
      }
    }
    // check whether Substantive does have a gender - warn otherwise
    // TODO: not quite sure what to do. One possibility is to guess Gender from the article
    if (!hasGender) {
      log.debug("Warning: no gender in Substantive entry.");
    }
    return inflection;
  }

  // TODO : remove the other word form that correspond to the canonical form...
  // TODO: les formes extraites à partir des définitions de formes fléchies sont en double
  @Override
  protected Set<LexicalForm> getLexicalFormsFromCell(int i, int j, Element cell,
      List<String> context) {
    // In the French language edition, pronunciation are often given in independant cells below
    // the lexical Form written rep. In case of a pronunication information, we attach the
    // ponounciation to the lexicalForm that were extracted from the cell above.
    String pron;
    if ((pron = isIsolatedPronunciation(cell)) != null) {
      Set<LexicalForm> lexFormsAbove = results.get(i - 1, j);
      if (null != lexFormsAbove) {
        Arrays.stream(pron.split("\\\\ ou \\\\")).map(Utils::standardizePronunciation)
            .filter(s -> s.length() > 0 && !"Prononciation ?".equalsIgnoreCase(s))
            .forEach(p -> lexFormsAbove.forEach(
                f -> f.addValue(new PhoneticRepresentation(standardizeValue(p), language))));
      } else {
        log.warn("No lexical form above as we have an isolated pronunciation in {}",
            this.entryName);
      }
      return new LinkedHashSet<>();
    } else {
      return super.getLexicalFormsFromCell(i, j, cell, context);
    }
  }

  @Override
  protected boolean elementIsAValidForm(Element anchor) {
    return !(anchor.attr("href").contains("Annexe:Prononciation")
        || anchor.attr("href").contains("action=edit")
        || anchor.attr("href").contains("/H_aspir%C3%A9")
        || anchor.attr("href").contains("/H_muet"));
  }

  /**
   * returns a pronunciation string iff the cell is a pronunciation only cell returns
   * 
   * @param cell a table cell
   * @return null or a string
   */
  private String isIsolatedPronunciation(Element cell) {
    // The expander does not produce an anchor, but only the pronunciation text around '\'
    String pron = cell.text();
    pron = pron.replaceAll("\\(h muet\\)", "");
    pron = pron.replaceAll("\\(h aspiré\\)", "");
    pron = pron.trim();
    if (pron.startsWith("\\") && pron.endsWith("\\"))
      return pron;
    else
      return null;
  }

  @Override
  protected boolean shouldProcessCell(Element cell) {
    return !cell.classNames().contains("invisible") && super.shouldProcessCell(cell);
  }
}
