package org.getalp.dbnary.enhancer.preprocessing;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

public class StatsModule {

  private class Stat {
    private int nbTranslations = 0;
    private int translationsWithoutGlosses = 0;
    private int nbGlossesWithSenseNumberOnly = 0;
    private int nbGlossesWithTextOnly = 0;
    private int nbGlossesWithSensNumberAndText = 0;

    public void registerTranslation(Resource trans) {
      nbTranslations++;

      Statement g = trans.getProperty(DBnaryOnt.gloss);
      if (null == g) {
        translationsWithoutGlosses++;
      } else {
        StructuredGloss sg = extractGlossStructure(g);
        if (null != sg) {
          String senseNumbers = sg.getSenseNumber();
          String descr = sg.getGloss();

          if (null != senseNumbers && null != descr)
            nbGlossesWithSensNumberAndText++;
          else if (null != senseNumbers)
            nbGlossesWithSenseNumberOnly++;
          else if (null != descr)
            nbGlossesWithTextOnly++;
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

  public static String getHeaders() {
    return "Translations,noGlosses,textOnlyGlosses,senseNumberOnlyGlosses,textAndSenseNumberGlosses";
  }

  HashMap<String, Stat> stats = new HashMap<String, Stat>();
  Stat currentStat;

  public StatsModule() {
    super();
  }

  public void reset(String lang) {
    currentStat = new Stat();
    stats.put(lang, currentStat);
  }

  public void registerTranslation(Resource trans) {
    currentStat.registerTranslation(trans);
  }

  private static StructuredGloss extractGlossStructure(Statement g) {
    if (null == g)
      return null;
    RDFNode gloss = g.getObject();
    if (gloss.isLiteral())
      return new StructuredGloss(null, gloss.asLiteral().getString());
    if (gloss.isResource()) {
      Resource glossResource = gloss.asResource();
      Statement sn = glossResource.getProperty(DBnaryOnt.senseNumber);
      String senseNumber = null;
      if (sn != null)
        senseNumber = sn.getString();
      Statement glossValue = glossResource.getProperty(RDF.value);
      String glossString = null;
      if (glossValue != null)
        glossString = glossValue.getString();
      return new StructuredGloss(senseNumber, glossString);
    }
    return null;
  }

  public void printStat(String lang, PrintWriter out) {
    Stat lstat = stats.get(lang);
    lstat.displayStats(out);
  }

  public void displayStats(PrintStream w) {
    displayStats(new PrintWriter(w));
  }

  public void displayStats(PrintWriter w) {
    w.println("Language," + getHeaders());
    for (Entry<String, Stat> e : stats.entrySet()) {
      w.print(e.getKey());
      e.getValue().displayStats(w);
      w.println();
    }
  }

}
