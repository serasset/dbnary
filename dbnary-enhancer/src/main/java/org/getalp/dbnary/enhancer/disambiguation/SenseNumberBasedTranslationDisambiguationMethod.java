package org.getalp.dbnary.enhancer.disambiguation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseNumberBasedTranslationDisambiguationMethod implements DisambiguationMethod {

  public static int NUMSN = 0;
  private static final Logger log =
      LoggerFactory.getLogger(SenseNumberBasedTranslationDisambiguationMethod.class);

  public SenseNumberBasedTranslationDisambiguationMethod() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public Set<Resource> selectWordSenses(Resource lexicalEntry, Object context)
      throws InvalidContextException, InvalidEntryException {
    if (!lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry)
        && !lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word)
        && !lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)
        && !lexicalEntry.hasProperty(RDF.type, DBnaryOnt.Page)) {
      throw new InvalidEntryException("Expecting an ontolex Lexical Entry or DBnary page.");
    }
    if (context instanceof Resource) {
      Resource trans = (Resource) context;
      if (!trans.hasProperty(RDF.type, DBnaryOnt.Translation)) {
        throw new InvalidContextException("Expecting a DBnary Translation Resource.");
      }
      // TODO: the sense number property is not defined for translations... However, it is added by
      // the preprocessing.
      Statement gloss = trans.getProperty(DBnaryOnt.gloss);
      if (null != gloss) {
        Statement s = gloss.getObject().asResource().getProperty(DBnaryOnt.senseNumber);
        if (null != s) {
          // Process sense number
          // System.out.println("Avoiding treating " + s.toString());
          return selectNumberedSenses(lexicalEntry, s);
        }
      }
      // Returns empty set if other processing did not return valid result
      return new HashSet<Resource>();
    } else {
      throw new InvalidContextException("Expecting a JENA Resource.");
    }

  }

  private Set<Resource> selectNumberedSenses(Resource lexicalEntry, Statement s) {
    Set<Resource> res = new HashSet<Resource>();

    String nums = s.getString();

    ArrayList<String> ns = getSenseNumbers(nums);
    for (String n : ns) {
      addNumberedWordSenseToResult(res, lexicalEntry, n);
    }
    return res;
  }

  private void addNumberedWordSenseToResult(Set<Resource> res, Resource lexicalEntry, String n) {
    if (lexicalEntry.hasProperty(RDF.type, DBnaryOnt.Page)) {
      int previousSize = res.size();
      StmtIterator entries = lexicalEntry.listProperties(DBnaryOnt.describes);
      while (entries.hasNext()) {
        addNumberedWordSenseToResult(res, entries.next().getResource(), n);
      }
      if (res.size() - previousSize > 1)
        log.debug("ENHANCER: more than 1 word sense for number {} while enhancing {}", n,
            lexicalEntry);
    } else {
      StmtIterator senses = lexicalEntry.listProperties(OntolexOnt.sense);
      while (senses.hasNext()) {
        Resource sense = senses.next().getResource();
        Statement senseNumStatement = sense.getProperty(DBnaryOnt.senseNumber);
        if (n.equalsIgnoreCase(senseNumStatement.getString())) {
          res.add(sense);
        }
      }
    }
  }


  public static ArrayList<String> getSenseNumbers(String nums) {
    ArrayList<String> ns = new ArrayList<String>();

    if (nums.contains(",")) {
      String[] ni = nums.split(",");
      for (int i = 0; i < ni.length; i++) {
        ns.addAll(getSenseNumbers(ni[i]));
      }
    } else if (nums.contains("-") || nums.contains("—") || nums.contains("–")) {
      String[] ni = nums.split("[-—–]");
      if (ni.length != 2) {
        log.debug("Strange split on dash: {}", nums);
      } else {
        try {
          int s = Integer.parseInt(ni[0].trim());
          int e = Integer.parseInt(ni[1].trim());

          if (e <= s) {
            log.debug("end of range is lower than beginning in: {}", nums);
          } else {
            for (int i = s; i <= e; i++) {
              ns.add(Integer.toString(i));
            }
          }
        } catch (NumberFormatException e) {
          log.error(e.getLocalizedMessage());
        }
      }
    } else {
      try {
        ns.add(nums.trim());
      } catch (NumberFormatException e) {
        log.error(e.getLocalizedMessage() + ": " + nums);
      }
    }
    return ns;
  }

}
