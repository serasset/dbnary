package org.getalp.dbnary.enhancer;

import org.apache.commons.cli.HelpFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.enhancer.disambiguation.InvalidContextException;
import org.getalp.dbnary.enhancer.disambiguation.InvalidEntryException;
import org.getalp.dbnary.enhancer.disambiguation.SenseNumberBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.disambiguation.TverskyBasedTranslationDisambiguationMethod;
import org.getalp.dbnary.enhancer.preprocessing.StructuredGloss;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class DisambiguateTranslationSources extends DBnaryEnhancer {

    private static final String USE_GLOSSES_OPTION = "g";
    private static final String PARAM_DEGREE_OPTION = "pdg";
    private static final String DEFAULT_DEGREE_VALUE = "1";
    private static final String PARAM_DELTA_OPTION = "pdl";
    private static final String DEFAULT_DELTA_VALUE = "0.05";
    private static final String PARAM_ALPHA_OPTION = "pda";
    private static final String DEFAULT_ALPHA_VALUE = "0.1";
    private static final String PARAM_BETA_OPTION = "pdb";
    private static final String DEFAULT_BETA_VALUE = "0.9";


    static {
        options.addOption(USE_GLOSSES_OPTION, false, "Use translation glosses for disambiguation when available (default=false)");
        options.addOption(PARAM_ALPHA_OPTION, true, "Alpha parameter for the Tversky index (default=" + DEFAULT_ALPHA_VALUE + ")");
        options.addOption(PARAM_BETA_OPTION, true, "Beta parameter for the Tversky index (default=" + DEFAULT_BETA_VALUE + ")");
        options.addOption(PARAM_DELTA_OPTION, true, "Delta parameter for the choice of disambiguations to keep as a solution (default=" + DEFAULT_DELTA_VALUE + ")");
        options.addOption(PARAM_DEGREE_OPTION, true, "Degree of the transitive closure (default=" + DEFAULT_DEGREE_VALUE + ")");
    }

    private boolean useGlosses = false;
    private double delta;
    private double alpha;
    private double beta;
    private int degree;

    private DisambiguateTranslationSources() {
    }

    public static void main(String[] args) throws IOException {

        DisambiguateTranslationSources lld = new DisambiguateTranslationSources();
        lld.loadArgs(args);

        lld.doit();

    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String help =
                "urlOrFile must point on an RDF model file extracted from wiktionary by DBnary.\n" +
                        "Alternatively specifying a directory will process all files named ??_dbnary_ontolex.ttl in the given dir";
        formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources [OPTIONS] (urlOrFile ...|DIR)",
                "With OPTIONS in:", options,
                help, false);
    }

    protected void loadArgs(String[] args) {
        super.loadArgs(args);
        useGlosses = cmd.hasOption(DisambiguateTranslationSources.USE_GLOSSES_OPTION);

        delta = Double.valueOf(cmd.getOptionValue(PARAM_DELTA_OPTION, DEFAULT_DELTA_VALUE));
        alpha = Double.valueOf(cmd.getOptionValue(PARAM_ALPHA_OPTION, DEFAULT_ALPHA_VALUE));
        beta = Double.valueOf(cmd.getOptionValue(PARAM_BETA_OPTION, DEFAULT_BETA_VALUE));
        degree = Integer.valueOf(cmd.getOptionValue(PARAM_DEGREE_OPTION, DEFAULT_DEGREE_VALUE));

    }

    @Override
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

