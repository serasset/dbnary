package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.cli.HelpFormatter;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.experiment.disambiguation.*;

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
	private static final String USE_STRUCTURE_OPTION = "st";
	private static final String TRANSLATIONAPI_ID_OPTION = "tid";
	private static final String TRANSLATIONAPI_PASS_OPTION = "tpw";
	private static final String DEFAULT_TRANSLATIONAPI_ID = "DBnary_def_xans";
	private static final String TRANSLATIONAPI_CACHE_OPTION = "tcache";
	private static final String DEFAULT_TRANSLATIONAPI_CACHE = "./xlationCache";


    static {
		options.addOption(USE_GLOSSES_OPTION,false,"Use translation glosses for disambiguation when available (default=false)");
		options.addOption(USE_STRUCTURE_OPTION,false,"Use structure for disambiguation when available (default=false)");
		options.addOption(TRANSLATIONAPI_ID_OPTION,true,"Use this ID for BING translation API to compute xlingual similarity (default=" + DEFAULT_TRANSLATIONAPI_ID + ")");
		options.addOption(TRANSLATIONAPI_PASS_OPTION,true,"Use this password for BING translation API to compute xlingual similarity (no default, mandatory if " + TRANSLATIONAPI_ID_OPTION + " option was specified)");
		options.addOption(TRANSLATIONAPI_PASS_OPTION,true,"folder containing the h2db used for translation caching (default=" + DEFAULT_TRANSLATIONAPI_CACHE + ")");
		options.addOption(PARAM_ALPHA_OPTION,true,"Alpha parameter for the Tversky index (default="+DEFAULT_ALPHA_VALUE+")");
		options.addOption(PARAM_BETA_OPTION,true,"Beta parameter for the Tversky index (default="+DEFAULT_BETA_VALUE+")");
		options.addOption(PARAM_DELTA_OPTION,true,"Delta parameter for the choice of disambiguations to keep as a solution (default="+DEFAULT_DELTA_VALUE+")");
		options.addOption(PARAM_DEGREE_OPTION,true,"Degree of the transitive closure (default="+DEFAULT_DEGREE_VALUE+")");
	}

	private boolean useGlosses = false;
	private boolean useStructure = false;
	private double delta;
	private double alpha;
	private double beta;
	private int degree;
	private boolean useTranslator = false;
	private String translatorId;
	private String translatorPass;
	private String translationCache;

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
                "urlOrFile must point on an RDF model file extracted from wiktionary by DBnary.";
        formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources [OPTIONS] urlOrFile ...",
                "With OPTIONS in:", options,
                help, false);
    }

    protected void loadArgs(String[] args) {
        super.loadArgs(args);
        useGlosses = cmd.hasOption(DisambiguateTranslationSources.USE_GLOSSES_OPTION);
        useStructure = cmd.hasOption(DisambiguateTranslationSources.USE_STRUCTURE_OPTION);
        useTranslator  = cmd.hasOption(DisambiguateTranslationSources.TRANSLATIONAPI_ID_OPTION);
        translatorId = cmd.getOptionValue(DisambiguateTranslationSources.TRANSLATIONAPI_ID_OPTION, DisambiguateTranslationSources.DEFAULT_TRANSLATIONAPI_ID);
        translatorPass = cmd.getOptionValue(DisambiguateTranslationSources.TRANSLATIONAPI_PASS_OPTION);

        if (useTranslator && (translatorPass == null || translatorPass.length() == 0)) {
            System.err.println("Translation API secret is mandatory when translkation API is requested.");
            printUsage();
            System.exit(0);
        }
        translationCache = cmd.getOptionValue(DisambiguateTranslationSources.TRANSLATIONAPI_CACHE_OPTION, DisambiguateTranslationSources.DEFAULT_TRANSLATIONAPI_CACHE);

        delta = Double.valueOf(cmd.getOptionValue(DisambiguateTranslationSources.PARAM_DELTA_OPTION, DisambiguateTranslationSources.DEFAULT_DELTA_VALUE));
        alpha = Double.valueOf(cmd.getOptionValue(DisambiguateTranslationSources.PARAM_ALPHA_OPTION, DisambiguateTranslationSources.DEFAULT_ALPHA_VALUE));
        beta = Double.valueOf(cmd.getOptionValue(DisambiguateTranslationSources.PARAM_BETA_OPTION, DisambiguateTranslationSources.DEFAULT_BETA_VALUE));
        degree = Integer.valueOf(cmd.getOptionValue(DisambiguateTranslationSources.PARAM_DEGREE_OPTION, DisambiguateTranslationSources.DEFAULT_DEGREE_VALUE));

       }


	protected void processTranslations(Model outputModel, String lang) throws FileNotFoundException {

		if (null != evaluator) evaluator.reset(lang);
		SenseNumberBasedTranslationDisambiguationMethod snumDisamb = new SenseNumberBasedTranslationDisambiguationMethod();
		TverskyBasedTranslationDisambiguationMethod tverskyDisamb = new TverskyBasedTranslationDisambiguationMethod(alpha, beta, delta);
		TransitiveTranslationClosureDisambiguationMethod transitDisamb = new TransitiveTranslationClosureDisambiguationMethod(degree,lang,modelMap,delta);
		XlingualTverskyBasedTranslationDisambiguationMethod xlingualTverskyDisamb = null;
		if (useTranslator)
			xlingualTverskyDisamb = new XlingualTverskyBasedTranslationDisambiguationMethod(modelMap, alpha, beta, delta, translatorId, translatorPass, translationCache);
		
		Model inputModel = modelMap.get(lang);
		StmtIterator translations = inputModel.listStatements(null, DBnaryOnt.isTranslationOf, (RDFNode) null);

		while (translations.hasNext()) {
			Statement next = translations.next();
			
			Resource trans = next.getSubject();

			Resource lexicalEntry = next.getResource();
			if (lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry) ||
					lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word) ||
					lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)) {
				try {
					Set<Resource> resSenseNum = snumDisamb.selectWordSenses(lexicalEntry, trans);
					
					Set<Resource> resSim = null;

					if (null != evaluator || resSenseNum.size() == 0) {
						// disambiguate by similarity

						if (useGlosses) resSim = tverskyDisamb.selectWordSenses(lexicalEntry, trans);

						if ((resSim == null || resSim.isEmpty()) && useTranslator) {
							if (! modelMap.containsKey(getTargetLanguage(trans))) continue;
							resSim = xlingualTverskyDisamb.selectWordSenses(lexicalEntry,trans);
						}

						if ((resSim == null || resSim.isEmpty()) && useStructure) { //No gloss!
							if (! modelMap.containsKey(getTargetLanguage(trans))) continue;
							resSim = transitDisamb.selectWordSenses(lexicalEntry,trans);
						}
												
						// compute confidence if snumdisamb is not empty and confidence is required
						if (null != evaluator && resSenseNum.size() != 0) {
							if(resSim==null){
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

	private Object getTargetLanguage(Resource trans) {
		try {
			Resource lang = trans.getPropertyResourceValue(DBnaryOnt.targetLanguage);
			if (lang == null) {
				Statement slang = trans.getProperty(DBnaryOnt.targetLanguageCode);
				return slang.getLiteral();
			} else {
				return lang.getLocalName();
			}
		} catch (LiteralRequiredException e) {
			return null;
		}
	}

}

