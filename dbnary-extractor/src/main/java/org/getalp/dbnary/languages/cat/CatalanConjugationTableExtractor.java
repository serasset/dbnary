package org.getalp.dbnary.languages.cat;

import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;
import org.getalp.dbnary.morphology.RefactoredTableExtractor;
import org.getalp.dbnary.morphology.RelaxInflexionScheme;
import org.getalp.dbnary.tools.ArrayMatrix;
import org.getalp.model.lexinfo.*;
import org.getalp.model.lexinfo.Number;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class CatalanConjugationTableExtractor extends RefactoredTableExtractor {

  private Logger log = LoggerFactory.getLogger(CatalanConjugationTableExtractor.class);

  public CatalanConjugationTableExtractor(String entryName, String language, List<String> context) {
    super(entryName, language, context);
  }

  @Override
  protected InflectionScheme getInflectionSchemeFromContext(List<String> context) {
    InflectionScheme inflection = new RelaxInflexionScheme();

    MorphoSyntacticFeature currentForm = null;
    context = cleanContext(context);

    if (context.contains("formes personals compostes i perifràstiques")
        || context.contains("compostes") || context.contains("verb")
        || context.contains("plusquamperfet"))
      return inflection;

    for (String h : context) {
      switch (h) {
        case "indicatiu":
          inflection.add(Mood.INDICATIVE);
          break;
        case "subjuntiu":
          inflection.add(Mood.SUBJUNCTIVE);
          break;
        case "imperatiu":
          inflection.add(Mood.IMPERATIVE);
          break;
        case "jo":
        case "io, yo, jo":
          inflection.add(Number.SINGULAR);
          inflection.add(Person.FIRST);
          break;
        case "tu":
          inflection.add(Number.SINGULAR);
          inflection.add(Person.SECOND);
          break;
        case "ell":
        case "ell/ella/vostè":
        case "él, eyl, ell":
          inflection.add(Number.SINGULAR);
          inflection.add(Person.THIRD);
          break;
        case "nosaltres":
        case "nós, nosaltres":
          inflection.add(Number.PLURAL);
          inflection.add(Person.FIRST);
          break;
        case "vosaltres":
        case "vosaltres/vós":
        case "vós, vosaltres":
          inflection.add(Number.PLURAL);
          inflection.add(Person.SECOND);
          break;
        case "ells/elles/vostès":
        case "éls, eyls, ells":
        case "ells":
          inflection.add(Number.PLURAL);
          inflection.add(Person.THIRD);
          break;
        case "present":
          inflection.add(Tense.PRESENT);
          break;
        case "imperfet":
          inflection.add(Tense.IMPERFECT);
          break;
        case "passat simple":
          inflection.add(Tense.PAST);
          break;
        case "futur":
          inflection.add(Tense.FUTURE);
          break;
        case "condicional":
          inflection.add(Mood.CONDITIONAL);
          break;
        case "infinitiu":
          inflection.add(Mood.INFINITIVE);
          break;
        case "gerundi":
          inflection.add(Mood.GERUNDIVE);
          break;
        case "participis":
        case "participi":
          inflection.add(Mood.PARTICIPLE);
          break;
        case "simples":
          break;
        case "compostes":
          break;
        case "formes personals simples":
          if (currentForm == null)
            currentForm = ReferentType.PERSONAL;
          break;
        case "formes no personals":
        case "–":
          break;
        default:
          log.debug("{} => Conjugation field non handled -> {} ---> {}", entryName, h,
              "https://ca.wiktionary.org/wiki/" + entryName);
          break;
      }
    }

    if (currentForm != null)
      inflection.add(currentForm);

    return inflection;
  }

  private List<String> cleanContext(final List<String> context) {
    ArrayList<String> clean = new ArrayList<>();
    Collections.reverse(context);
    for (String val : context)
      if (!clean.contains(val.trim().toLowerCase(Locale.ROOT)) && !"-".equals(val))
        clean.add(val.trim().toLowerCase(Locale.ROOT));
    return clean;
  }

  @Override
  protected List<String> getRowAndColumnContext(int nrow, int ncol,
      ArrayMatrix<Element> columnHeaders) {
    LinkedList<String> res = new LinkedList<>();
    int closestRow = -1;
    int secondClosestRow = -1;
    int closestCol = -1;

    for (int i = 0; i < nrow; i++)
      if (addToContext(columnHeaders, i, ncol, new ArrayList<>())
          && (closestRow == -1 || !columnHeaders.get(closestRow, ncol).text()
              .equals(columnHeaders.get(i, ncol).text()))) {
        secondClosestRow = closestRow;
        closestRow = i;
      }
    for (int i = 0; i < ncol; i++)
      if (addToContext(columnHeaders, nrow, i, new ArrayList<>()))
        closestCol = i;

    if (closestRow != -1) {
      addToContext(columnHeaders, closestRow, 0, res);
      addToContext(columnHeaders, closestRow, ncol, res);
    }

    if (secondClosestRow != -1)
      addToContext(columnHeaders, secondClosestRow, ncol, res);

    if (closestCol != -1)
      addToContext(columnHeaders, nrow, closestCol, res);

    return res;
  }

  @Override
  protected boolean shouldProcessCell(Element cell) {
    if (cell.toString().equals("<td>–</td>"))
      return false;
    return super.shouldProcessCell(cell);
  }

}
