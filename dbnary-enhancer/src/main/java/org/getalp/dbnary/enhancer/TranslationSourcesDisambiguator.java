package org.getalp.dbnary.enhancer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.enhancer.disambiguation.InvalidContextException;
import org.getalp.dbnary.enhancer.disambiguation.InvalidEntryException;
import org.getalp.dbnary.enhancer.disambiguation.SenseNumberBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.disambiguation.TverskyBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStatsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranslationSourcesDisambiguator {

  private double alpha;
  private double beta;
  private double delta;
  private boolean useGlosses;
  private TranslationGlossesStatsModule stats;
  private EvaluationStats evaluator;

  private Logger log = LoggerFactory.getLogger(TranslationSourcesDisambiguator.class);

  public TranslationSourcesDisambiguator(double alpha, double beta, double delta,
      boolean useGlosses, TranslationGlossesStatsModule stats, EvaluationStats evaluator) {
    this.alpha = alpha;
    this.beta = beta;
    this.delta = delta;
    this.useGlosses = useGlosses;
    this.stats = stats;
    this.evaluator = evaluator;
  }

  public void processTranslations(Model inputModel, Model outputModel, String lang) {

    if (null != evaluator) {
      evaluator.reset(lang);
    }
    if (null != stats) {
      stats.reset(lang);
    }

    SenseNumberBasedTranslationDisambiguationMethod snumDisamb =
        new SenseNumberBasedTranslationDisambiguationMethod();
    TverskyBasedTranslationDisambiguationMethod tverskyDisamb =
        new TverskyBasedTranslationDisambiguationMethod(alpha, beta, delta);

    StmtIterator translations =
        inputModel.listStatements(null, DBnaryOnt.isTranslationOf, (RDFNode) null);

    HashMap<Resource, Set<Resource>> translationToWSMap = new HashMap<>();
    while (translations.hasNext()) {
      Statement next = translations.next();

      Resource trans = next.getSubject();

      Resource lexicalEntry = next.getResource();
      if (lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry)
          || lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word)
          || lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)
          || lexicalEntry.hasProperty(RDF.type, DBnaryOnt.Page)) {
        try {
          log.trace("Enhancing translation resource {} for entry {}", trans.getLocalName(),
              lexicalEntry.getLocalName());

          if (null != stats) {
            stats.registerTranslation(trans);
          }

          Set<Resource> resSenseNum = snumDisamb.selectWordSenses(lexicalEntry, trans);

          Set<Resource> resSim = null;

          if (null != evaluator || resSenseNum.size() == 0) {
            // disambiguate by similarity

            if (useGlosses) {
              resSim = tverskyDisamb.selectWordSenses(lexicalEntry, trans);
            }

            // compute confidence if snumdisamb is not empty and confidence is required
            if (null != evaluator && resSenseNum.size() != 0) {
              if (resSim == null) {
                resSim = new HashSet<>();
              }
              int nsense = getNumberOfSenses(lexicalEntry);
              evaluator.registerAnswer(resSenseNum, resSim, nsense);
            }
          }

          Set<Resource> res = (resSenseNum.isEmpty()) ? resSim : resSenseNum;

          // register links that will be created in enhancement model after the iteration
          // as dataset should not be modified and read at the same time when back by a TDB.
          if (res != null && !res.isEmpty()) {
            translationToWSMap.put(trans, res);
          }

        } catch (InvalidContextException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (InvalidEntryException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    translationToWSMap.forEach((t, wss) -> wss.forEach(ws -> {
      outputModel.add(outputModel.createStatement(t, DBnaryOnt.isTranslationOf, ws));
      // Also add the lexical entry if not already available (case of a translation linked to a page
      ResIterator lexentries = inputModel.listResourcesWithProperty(OntolexOnt.sense, ws);
      while (lexentries.hasNext()) {
        Resource le = lexentries.next();
        if (!t.hasProperty(DBnaryOnt.isTranslationOf, le)) {
          log.debug(
              "Adding LexEntry {} to Page related translation {} while registering WordSense {}",
              le.getLocalName(), t.getLocalName(), ws.getLocalName());
          outputModel.add(outputModel.createStatement(t, DBnaryOnt.isTranslationOf, le));
        }
      }
    }));

  }

  private int getNumberOfSenses(Resource lexicalEntry) {
    StmtIterator senses = lexicalEntry.listProperties(OntolexOnt.sense);
    int n = 0;
    while (senses.hasNext()) {
      n++;
      senses.next();
    }
    return n;
  }

}

