package org.getalp.dbnary.fra.morphology;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.RefactoredTableExtractor;
import org.getalp.dbnary.morphology.RelaxInflexionScheme;
import org.getalp.model.lexinfo.Gender;
import org.getalp.model.lexinfo.Mood;
import org.getalp.model.lexinfo.Number;
import org.getalp.model.lexinfo.Person;
import org.getalp.model.lexinfo.Tense;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.PhoneticRepresentation;
import org.getalp.model.ontolex.Representation;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpersonalMoodTableExtractor extends RefactoredTableExtractor {

  private Logger log = LoggerFactory.getLogger(ImpersonalMoodTableExtractor.class);

  public ImpersonalMoodTableExtractor(String currentEntry, String language, List<String> context) {
    super(currentEntry, language, context);
  }

  @Override
  protected InflectionScheme getInflectionSchemeFromContext(List<String> context) {
    InflectionScheme inflection = new RelaxInflexionScheme();
    for (String h : context) {
      h = h.trim();
      switch (h) {
        case "Présent":
          inflection.add(Tense.PRESENT);
          break;
        case "Passé":
          inflection.add(Tense.PAST);
          break;
        case "Infinitif":
          if (inflection.contains(Tense.PAST))
            return null; // Past infinitive is regular
          inflection.add(Mood.INFINITIVE);
          break;
        case "Gérondif":
          if (inflection.contains(Tense.PAST))
            return null; // Past gerundive is regular
          inflection.add(Mood.GERUNDIVE);
          break;
        case "Participe":
          inflection.add(Mood.PARTICIPLE);
          break;
        case "Impératif":
          if (inflection.contains(Tense.PAST))
            return null; // Past imperative is regular
          inflection.add(Mood.IMPERATIVE);
          break;
        case "—":
        case "":
        case " ":
        case "Modes impersonnels":
          // Silently ignore these
          break;
        default:
          log.debug("Unhandled header {} in {}", h, this.entryName);
      }
    }
    return inflection;
  }

  // TODO : remove otherForms corresponding to the canonical form in entry post processing ?
  // TODO: les formes extraites à partir des définitions de formes fléchies sont en double
  @Override
  protected Set<LexicalForm> getLexicalFormsFromCell(int i, int j, Element cell,
      List<String> context) {
    // In the French language edition, pronunciation are often given in independant cells below
    // the lexical Form written rep. In case of a pronunication information, we attach the
    // ponounciation to the lexicalForm that were extracted from the cell immediately on the left.
    if (isIsolatedPronunciation(cell)) {
      Set<LexicalForm> lexFormsOnTheLeft = results.get(i, j - 1);
      if (null != lexFormsOnTheLeft) {
        String pron = Utils.standardizePronunciation(standardizeValue(cell.text()));
        lexFormsOnTheLeft.forEach(f -> f.addValue(new PhoneticRepresentation(pron, language)));
      }
      return new LinkedHashSet<>();
    } else {
      Set<LexicalForm> forms = super.getLexicalFormsFromCell(i, j, cell, context);
      if (context.contains("Impératif")) {
        // we have to specify the Number and Person from the cell position for imperative mood.
        forms.forEach(f -> handleNumberPerson(f, i, j));
      }
      // some forms may have several orthographies, separated by " ou "
      forms.forEach(f -> {
        for (Representation v : f.getValues()) {
          if (v instanceof WrittenRepresentation && v.getValue().contains(" ou ")) {
            f.removeValue(v);
            Arrays.stream(v.getValue().split(" ou "))
                .forEach(wr -> f.addValue(new WrittenRepresentation(wr, v.getLanguage())));
          }
        }
      });
      // Specify the past participle form as being masculine and singular by default
      forms.forEach(f -> {
        if (f.getFeature().contains(Tense.PAST) && f.getFeature().contains(Mood.PARTICIPLE)) {
          f.getFeature().add(Gender.MASCULINE);
          f.getFeature().add(Number.SINGULAR);
        }
      });
      return forms;
    }
  }

  private void handleNumberPerson(LexicalForm f, int i, int j) {
    switch (i) {
      case 1:
        f.getFeature().add(Number.SINGULAR);
        f.getFeature().add(Person.SECOND);
        break;
      case 2:
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.FIRST);
        break;
      case 3:
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.SECOND);
        break;
      default:
        log.warn("Unexpected cell position {} in Imperative table in {}", i, this.entryName);
    }
  }

  private boolean isIsolatedPronunciation(Element cell) {
    // The expander does not produce an anchor, but only the pronunciation text around '\'
    String pron = cell.text().trim();
    return pron.startsWith("\\") && pron.endsWith("\\");
  }

  @Override
  protected boolean isHeaderCell(Element cell) {
    // In verbal inflexion table, some headers are given in bold inside normal cells
    return super.isHeaderCell(cell) || !cell.select("b").isEmpty();
  }

  @Override
  protected boolean shouldProcessCell(Element cell) {
    // the French verbal tables use cells that contains subjects ("je/tu/il") or other grammatical
    // contexts of the inflection. We should only process cells containing a selflink or an anchor.
    return super.shouldProcessCell(cell) && (!cell.select("strong.selflink").isEmpty()
        || !cell.select("a").isEmpty() || isIsolatedPronunciation(cell));
  }
}
