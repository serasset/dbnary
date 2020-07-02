package org.getalp.dbnary.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryOnt;

public class Statistics {

  public static long countResourcesOfType(Resource type, Model m1) {
    ResIterator resit = m1.listResourcesWithProperty(RDF.type, type);
    int nb = 0;
    while (resit.hasNext()) {
      resit.next();
      nb++;
    }
    resit.close();
    return nb;
  }

  public static long countRelations(Property prop, Model m1) {
    ResIterator resit = m1.listResourcesWithProperty(prop);
    int nb = 0;

    while (resit.hasNext()) {
      resit.next();
      nb++;
    }
    resit.close();

    return nb;
  }


  protected static class IncrementableLong {

    long val;

    public IncrementableLong() {
      val = 0;
    }

    public void incr() {
      this.val++;
    }

    public String toString() {
      return Long.toString(this.val);
    }
  }

  private static String getCode(Resource resource) {
    return resource.getLocalName();
  }


  public static Map<String, Long> translationCounts(Model m1) {
    String targets = "deu,ell,eng,fin,fra,ita,jpn,por,rus,tur";
    // TODO: extract iso code from lexvo entity.
    SortedMap<String, IncrementableLong> counts = initCounts(targets);

    ResIterator relations = m1.listResourcesWithProperty(RDF.type, DBnaryOnt.Translation);
    HashSet<String> langs = new HashSet<String>();
    long total = 0, others = 0;
    while (relations.hasNext()) {
      Resource r = relations.next();
      Statement t = r.getProperty(DBnaryOnt.targetLanguage);
      if (null != t) {
        RDFNode lang = t.getObject();
        langs.add(getCode(lang.asResource()));
        if (counts.containsKey(getCode(lang.asResource()))) {
          counts.get(getCode(lang.asResource())).incr();
        } else {
          others++;
        }
      }
      total++;
    }
    relations.close();
    Map<String, Long> result = new HashMap<>();
    counts.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().val));
    result.put("mul", total);
    result.put("number_of_languages", (long) langs.size());
    result.put("others", others);

    return result;
  }

  private static SortedMap<String, IncrementableLong> initCounts(String targets) {
    SortedMap<String, IncrementableLong> counts = new TreeMap<>();

    String clangs[] = targets.split(",");
    int i = 0;
    while (i != clangs.length) {
      counts.put(LangTools.getCode(clangs[i]), new IncrementableLong());
      i = i + 1;
    }
    return counts;
  }


}
