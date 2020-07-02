package org.getalp.dbnary.enhancer.evaluation;

import java.io.PrintWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.getalp.dbnary.DBnaryOnt;

public class TranslationGlossesStat {

  private int nbTranslations = 0;
  private int translationsWithoutGlosses = 0;

  public int getNbTranslations() {
    return nbTranslations;
  }

  public int getTranslationsWithoutGlosses() {
    return translationsWithoutGlosses;
  }

  public int getNbGlossesWithSenseNumberOnly() {
    return nbGlossesWithSenseNumberOnly;
  }

  public int getNbGlossesWithTextOnly() {
    return nbGlossesWithTextOnly;
  }

  public int getNbGlossesWithSensNumberAndText() {
    return nbGlossesWithSensNumberAndText;
  }

  private int nbGlossesWithSenseNumberOnly = 0;
  private int nbGlossesWithTextOnly = 0;
  private int nbGlossesWithSensNumberAndText = 0;

  public void registerTranslation(Resource trans) {
    nbTranslations++;

    Statement g = trans.getProperty(DBnaryOnt.gloss);
    if (null == g) {
      translationsWithoutGlosses++;
    } else {
      StructuredGloss sg = TranslationGlossesStatsModule.extractGlossStructure(g);
      if (null != sg) {
        String senseNumbers = sg.getSenseNumber();
        String descr = sg.getGloss();

        if (null != senseNumbers && null != descr) {
          nbGlossesWithSensNumberAndText++;
        } else if (null != senseNumbers) {
          nbGlossesWithSenseNumberOnly++;
        } else if (null != descr) {
          nbGlossesWithTextOnly++;
        }
      } else {
        translationsWithoutGlosses++;
      }
    }
  }

  public void displayStats(PrintWriter w) {
    w.format("%d,%d,%d,%d,%d", nbTranslations, translationsWithoutGlosses, nbGlossesWithTextOnly,
        nbGlossesWithSenseNumberOnly, nbGlossesWithSensNumberAndText);
  }
}
