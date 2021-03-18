package org.getalp.dbnary.fra;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.RefactoredTableExtractor;
import org.getalp.dbnary.morphology.RelaxInflexionScheme;
import org.getalp.lexinfo.model.Mood;
import org.getalp.lexinfo.model.Number;
import org.getalp.lexinfo.model.Person;
import org.getalp.lexinfo.model.Tense;
import org.getalp.ontolex.model.LexicalForm;
import org.getalp.ontolex.model.PhoneticRepresentation;
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
        String pron = standardizeValue(cell.text().trim());
        lexFormsOnTheLeft.forEach(f -> f.addValue(new PhoneticRepresentation(pron, language)));
      }
      return new LinkedHashSet<>();
    } else {
      Set<LexicalForm> forms = super.getLexicalFormsFromCell(i, j, cell, context);
      if (context.contains("Impératif")) {
        // we have to specify the Number and Person from the cell position for imperative mood.
        forms.forEach(f -> handleNumberPerson(f, i, j));
      }
      return forms;
    }

  }

  private void handleNumberPerson(LexicalForm f, int i, int j) {
    switch (i) {
      case 1:
        f.getFeature().add(Number.SINGULAR);
        f.getFeature().add(Person.THIRD);
        break;
      case 2:
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.SECOND);
        break;
      case 3:
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.THIRD);
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
