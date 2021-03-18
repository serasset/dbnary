package org.getalp.dbnary.fra;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;
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

public class StandardMoodTableExtractor extends RefactoredTableExtractor {

  private Logger log = LoggerFactory.getLogger(StandardMoodTableExtractor.class);

  public StandardMoodTableExtractor(String currentEntry, String language, List<String> context) {
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
        case "Passé simple":
          inflection.add(Tense.PRETERITE);
          break;
        case "Futur simple":
          inflection.add(Tense.FUTURE);
          break;
        case "Imparfait":
          inflection.add(Tense.IMPERFECT);
          break;
        case "Indicatif":
          inflection.add(Mood.INDICATIVE);
          break;
        case "Subjonctif":
          if (inflection.contains(Tense.PAST))
            return null; // Past subjunctive is regular
          inflection.add(Mood.SUBJUNCTIVE);
          break;
        case "Conditionnel":
          if (inflection.contains(Tense.PAST))
            return null; // Past conditional is regular
          inflection.add(Mood.CONDITIONAL);
          break;
        case "Impératif":
          if (inflection.contains(Tense.PAST))
            return null; // Past imperative is regular
          inflection.add(Mood.IMPERATIVE);
          break;
        case "Passé composé":
        case "Plus-que-parfait":
        case "Passé antérieur":
        case "Futur antérieur":
          // All values in these tenses can be reconstructed grammatically
          return null;
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
    // In the standard mood tables of the French language edition, pronunciation are given in
    // two separate cells on the right of the lexical Form written rep. The first one is the
    // pronunciation of the subjects (grammatical context), the second one is the pronunciation
    // of the inflected form.
    // In case of an inflected form pronunciation information, we attach the
    // pronunciation to the lexicalForms that were extracted from the other cell on the left.
    if (isInflectedFormPronunciation(cell)) {
      if (j < 2) {
        log.warn("Unusual verbal inflection table geometry in {}", this.entryName);
        return new LinkedHashSet<>();
      }
      Set<LexicalForm> lexFormsOnTheLeft = results.get(i, j - 2);
      if (null != lexFormsOnTheLeft) {
        String pron = fixPronunciationValue(standardizeValue(cell.text().trim()));
        lexFormsOnTheLeft.forEach(f -> f.addValue(new PhoneticRepresentation(pron, language)));
      }
      return new LinkedHashSet<>();
    } else {
      Set<LexicalForm> forms = super.getLexicalFormsFromCell(i, j, cell, context);
      // The normal extraction process does not recognize the gender and number that are not
      // available in cells' headers. We get the info from grammatical context cell on the left.
      Element leftContext = cells.get(i, j - 1);
      forms.forEach(f -> handleGrammaticalContext(f, leftContext));
      return forms;
    }
  }

  private static Map<Pattern, Consumer<Set<MorphoSyntacticFeature>>> actions = new LinkedHashMap<>();
  static {
    actions.put(Pattern.compile("^(?:je|j’)"), Person.first.andThen(Number.singular));
    actions.put(Pattern.compile("^tu"), Person.second.andThen(Number.singular));
    actions.put(Pattern.compile("^il/elle"), Person.third.andThen(Number.singular));
    actions.put(Pattern.compile("^nous"), Person.first.andThen(Number.plural));
    actions.put(Pattern.compile("^vous"), Person.second.andThen(Number.plural));
    actions.put(Pattern.compile("^ils/elles"), Person.third.andThen(Number.plural));
  }

  private void handleGrammaticalContext(LexicalForm f, Element leftContext) {
    String c = standardizeValue(leftContext.text().trim()).replaceAll("^(?:que |qu’)", "");
    boolean noneMatch = true;
    for (Entry<Pattern, Consumer<Set<MorphoSyntacticFeature>>> entry : actions.entrySet()) {
      Pattern pattern = entry.getKey();
      Consumer<Set<MorphoSyntacticFeature>> action = entry.getValue();
      if (pattern.matcher(c).find()) {
        noneMatch = false;
        action.accept(f.getFeature());
      }
    }
    if (noneMatch) log.warn("Unexpected grammatical context {} in {}", c, this.entryName);
  }

  private void handleGrammaticalContextOld(LexicalForm f, Element leftContext) {
    String c = standardizeValue(leftContext.text().trim()).replaceAll("^(?:que |qu’)", "");
    switch (c) {
      case "je":
      case "j’":
        f.getFeature().add(Number.SINGULAR);
        f.getFeature().add(Person.FIRST);
        break;
      case "tu":
        f.getFeature().add(Number.SINGULAR);
        f.getFeature().add(Person.SECOND);
        break;
      case "il/elle/on":
        f.getFeature().add(Number.SINGULAR);
        f.getFeature().add(Person.THIRD);
        break;
      case "nous":
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.FIRST);
        break;
      case "vous":
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.SECOND);
        break;
      case "ils/elles":
        f.getFeature().add(Number.PLURAL);
        f.getFeature().add(Person.THIRD);
        break;
      default:
        log.warn("Unexpected grammatical context {} in {}", c, this.entryName);
    }
  }

  private String fixPronunciationValue(String value) {
    if (!value.startsWith("\\"))
      value = "\\" + value;
    return value;
  }

  private boolean isInflectedFormPronunciation(Element cell) {
    // The expander does not produce an anchor, but only the pronunciation text around '\'
    String pron = cell.text().trim();
    return pron.endsWith("\\");
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
        || !cell.select("a").isEmpty() || isInflectedFormPronunciation(cell));
  }


}
