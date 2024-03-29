package org.getalp.dbnary.enhancer.disambiguation;

import com.wcohen.ss.ScaledLevenstein;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.SkosOnt;
import org.getalp.dbnary.enhancer.similarity.string.TverskiIndex;

public class TverskyBasedTranslationDisambiguationMethod implements DisambiguationMethod {

  private final double delta;
  // private double alpha;
  // private double beta;
  private final TverskiIndex tversky;

  public TverskyBasedTranslationDisambiguationMethod(double alpha, double beta, double threshold) {
    delta = threshold;
    // this.alpha = alpha;
    // this.beta = beta;
    tversky = new TverskiIndex(alpha, beta, true, false, new ScaledLevenstein());
  }

  private static class WeigthedSense {

    protected double weight;
    protected Resource sense;

    public WeigthedSense(double weight, Resource sense) {
      super();
      this.weight = weight;
      this.sense = sense;
    }
  }

  // Caching word sense selection as it is called once for each translation and its result depends
  // solely on the (shared) gloss
  private final Map<String, Set<Resource>> gloss2SensesCache = new HashMap<>();

  @Override
  public Set<Resource> selectWordSenses(Resource lexicalEntry, Object context)
      throws InvalidContextException, InvalidEntryException {
    HashSet<Resource> res = new HashSet<>();

    if (!lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry)
        && !lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word)
        && !lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)
        && !lexicalEntry.hasProperty(RDF.type, DBnaryOnt.Page)) {
      throw new InvalidEntryException("Expecting an ontolex Lexical Entry.");
    }
    if (context instanceof Resource) {
      Resource trans = (Resource) context;
      if (!trans.hasProperty(RDF.type, DBnaryOnt.Translation)) {
        throw new InvalidContextException("Expecting a DBnary Translation Resource.");
      }

      Statement glossStmt = trans.getProperty(DBnaryOnt.gloss);

      if (null != glossStmt) {
        Statement glossValueStmt = glossStmt.getObject().asResource().getProperty(RDF.value);
        if (null != glossValueStmt) {
          String gloss = glossValueStmt.getString();
          String cacheKey = String.format("%s/%s", lexicalEntry, gloss);

          // Get result from cache if it is available
          if (gloss2SensesCache.containsKey(cacheKey)) {
            return gloss2SensesCache.get(cacheKey);
          }

          ArrayList<WeigthedSense> weightedList = new ArrayList<>();

          // get a list of wordsenses, sorted by decreasing similarity.
          ArrayList<Resource> senses = new ArrayList<>();
          fillInSenseOf(senses, lexicalEntry);

          for (Resource wordsense : senses) {
            Statement dRef = wordsense.getProperty(SkosOnt.definition);
            Statement dVal = dRef.getProperty(RDF.value);
            String deftext = dVal.getObject().toString();

            double sim = tversky.compute(deftext, gloss);

            insert(weightedList, wordsense, sim);
          }

          if (weightedList.size() == 0) {
            gloss2SensesCache.put(cacheKey, res);
            return res;
          }

          int i = 0;
          double worstScore = weightedList.get(0).weight - delta;
          while (i != weightedList.size() && weightedList.get(i).weight >= worstScore) {
            res.add(weightedList.get(i).sense);
            i++;
          }
          gloss2SensesCache.put(cacheKey, res);
        }
      }
    } else {
      throw new InvalidContextException("Expecting a JENA Resource.");
    }

    return res;
  }

  private void fillInSenseOf(ArrayList<Resource> senses, Resource pageOrLexEntry) {
    if (pageOrLexEntry.hasProperty(RDF.type, DBnaryOnt.Page)) {
      StmtIterator entries = pageOrLexEntry.listProperties(DBnaryOnt.describes);
      while (entries.hasNext()) {
        fillInSenseOf(senses, entries.next().getResource());
      }
    } else {
      StmtIterator lexEntrySenses = pageOrLexEntry.listProperties(OntolexOnt.sense);
      while (lexEntrySenses.hasNext()) {
        senses.add(lexEntrySenses.next().getResource());
      }
    }
  }

  private void insert(ArrayList<WeigthedSense> weightedList, Resource wordsense, double sim) {
    weightedList.add(null);
    int i = weightedList.size() - 1;
    while (i != 0 && weightedList.get(i - 1).weight < sim) {
      weightedList.set(i, weightedList.get(i - 1));
      i--;
    }
    weightedList.set(i, new WeigthedSense(sim, wordsense));
  }

}
