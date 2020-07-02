package org.getalp.dbnary.enhancer.evaluation;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;

public class TranslationGlossesStatsModule {

  public static String getHeaders() {
    return "Translations,noGlosses,textOnlyGlosses,senseNumberOnlyGlosses,textAndSenseNumberGlosses";
  }

  public HashMap<String, TranslationGlossesStat> getStatsMap() {
    return stats;
  }

  private HashMap<String, TranslationGlossesStat> stats =
      new HashMap<String, TranslationGlossesStat>();
  private TranslationGlossesStat currentStat;

  public TranslationGlossesStatsModule() {
    super();
  }

  public void reset(String lang) {
    currentStat = new TranslationGlossesStat();
    stats.put(lang, currentStat);
  }

  public void registerTranslation(Resource trans) {
    currentStat.registerTranslation(trans);
  }

  protected static StructuredGloss extractGlossStructure(Statement g) {
    if (null == g) {
      return null;
    }
    RDFNode gloss = g.getObject();
    if (gloss.isLiteral()) {
      return new StructuredGloss(null, gloss.asLiteral().getString());
    }
    if (gloss.isResource()) {
      Resource glossResource = gloss.asResource();
      Statement sn = glossResource.getProperty(DBnaryOnt.senseNumber);
      String senseNumber = null;
      if (sn != null) {
        senseNumber = sn.getString();
      }
      Statement glossValue = glossResource.getProperty(RDF.value);
      String glossString = null;
      if (glossValue != null) {
        glossString = glossValue.getString();
      }
      return new StructuredGloss(senseNumber, glossString);
    }
    return null;
  }

  public void printStat(String lang, PrintWriter out) {
    TranslationGlossesStat lstat = stats.get(lang);
    lstat.displayStats(out);
  }

  public void displayStats(PrintStream w) {
    displayStats(new PrintWriter(w));
  }

  public void displayStats(PrintWriter w) {
    w.println("Language," + getHeaders());
    for (Entry<String, TranslationGlossesStat> e : stats.entrySet()) {
      w.print(e.getKey());
      e.getValue().displayStats(w);
      w.println();
    }
  }

}
