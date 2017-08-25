package org.getalp.dbnary.enhancer;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.enhancer.disambiguation.InvalidContextException;
import org.getalp.dbnary.enhancer.disambiguation.InvalidEntryException;
import org.getalp.dbnary.enhancer.disambiguation.SenseNumberBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.disambiguation.TverskyBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.preprocessing.StatsModule;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;


public class TranslationSourcesDisambiguator {

    private double alpha;
    private double beta;
    private double delta;
    private boolean useGlosses;
    private StatsModule stats;
    private EvaluationStats evaluator;

    public TranslationSourcesDisambiguator(double alpha, double beta, double delta, boolean useGlosses,
                                           StatsModule stats, EvaluationStats evaluator) {
        this.alpha = alpha;
        this.beta = beta;
        this.delta = delta;
        this.useGlosses = useGlosses;
        this.stats = stats;
        this.evaluator = evaluator;
    }

    protected void processTranslations(Model inputModel, Model outputModel, String lang) throws FileNotFoundException {

        if (null != evaluator) evaluator.reset(lang);
        if (null != stats) stats.reset(lang);

        SenseNumberBasedTranslationDisambiguationMethod snumDisamb = new SenseNumberBasedTranslationDisambiguationMethod();
        TverskyBasedTranslationDisambiguationMethod tverskyDisamb = new TverskyBasedTranslationDisambiguationMethod(alpha, beta, delta);

        StmtIterator translations = inputModel.listStatements(null, DBnaryOnt.isTranslationOf, (RDFNode) null);

        while (translations.hasNext()) {
            Statement next = translations.next();

            Resource trans = next.getSubject();

            Resource lexicalEntry = next.getResource();
            if (lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry) ||
                    lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word) ||
                    lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)) {
                try {
                    if (null != stats) stats.registerTranslation(trans);

                    Set<Resource> resSenseNum = snumDisamb.selectWordSenses(lexicalEntry, trans);

                    Set<Resource> resSim = null;

                    if (null != evaluator || resSenseNum.size() == 0) {
                        // disambiguate by similarity

                        if (useGlosses)
                            resSim = tverskyDisamb.selectWordSenses(lexicalEntry, trans);

                        // compute confidence if snumdisamb is not empty and confidence is required
                        if (null != evaluator && resSenseNum.size() != 0) {
                            if (resSim == null) {
                                resSim = new HashSet<>();
                            }
                            int nsense = getNumberOfSenses(lexicalEntry);
                            evaluator.registerAnswer(resSenseNum, resSim, nsense);
                        }
                    }

                    // Register results in output Model
                    Resource translation = outputModel.createResource(trans.getURI());

                    Set<Resource> res = (resSenseNum.isEmpty()) ? resSim : resSenseNum;

                    if (res != null && !res.isEmpty()) {
                        for (Resource ws : res) {
                            outputModel.add(outputModel.createStatement(translation, DBnaryOnt.isTranslationOf, outputModel.createResource(ws.getURI())));
                        }
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

