package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.cli.HelpFormatter;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.VarTransOnt;
import org.getalp.dbnary.experiment.disambiguation.*;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

// import org.getalp.dbnary.DbnaryModel;


public class TranslationSourcesTarget extends DBnaryEnhancer {

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
    private static final String TARGET_TDB_FOLDER_OPTION = "t";

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
		options.addOption(TARGET_TDB_FOLDER_OPTION, true, "Path to the folder containing the TDB version of DBnary data used to query target languages.");
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
	private Dataset dataset ;
	private String tdbFolder;
    private Logger log = LoggerFactory.getLogger(TranslationSourcesTarget.class);

	private TranslationSourcesTarget() {
	}

	public static void main(String[] args) throws IOException {

		TranslationSourcesTarget lld = new TranslationSourcesTarget();
		lld.loadArgs(args);

		lld.doit();

	}

	@Override
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

		useGlosses = cmd.hasOption(USE_GLOSSES_OPTION);
		useStructure = cmd.hasOption(USE_STRUCTURE_OPTION);
		useTranslator  = cmd.hasOption(TRANSLATIONAPI_ID_OPTION);
		translatorId = cmd.getOptionValue(TRANSLATIONAPI_ID_OPTION, DEFAULT_TRANSLATIONAPI_ID);
		translatorPass = cmd.getOptionValue(TRANSLATIONAPI_PASS_OPTION);
		
		if (useTranslator && (translatorPass == null || translatorPass.length() == 0)) {
			System.err.println("Translation API secret is mandatory when translation API is requested.");
			printUsage();
			System.exit(0);
		}
		translationCache = cmd.getOptionValue(TRANSLATIONAPI_CACHE_OPTION, DEFAULT_TRANSLATIONAPI_CACHE);

		delta = Double.valueOf(cmd.getOptionValue(PARAM_DELTA_OPTION,DEFAULT_DELTA_VALUE));
		alpha = Double.valueOf(cmd.getOptionValue(PARAM_ALPHA_OPTION,DEFAULT_ALPHA_VALUE));
		beta = Double.valueOf(cmd.getOptionValue(PARAM_BETA_OPTION,DEFAULT_BETA_VALUE));
		degree = Integer.valueOf(cmd.getOptionValue(PARAM_DEGREE_OPTION,DEFAULT_DEGREE_VALUE));

	}

    @Override
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

		Model tmpModel = ModelFactory.createDefaultModel();

		/*
		int nbwslost2 = 0 ;
		int nbwskept = 0 ;
		int nbcfmiss = 0 ;
		int nbcf = 0 ;
		int nbcfcf = 0 ;
		int nbcfelse = 0 ;
		int nbcfle = 0 ;
		int nblemiss = 0 ;
		int nble = 0 ;
		int nbleposmisstle = 0 ;
		int nbleposmisssle = 0 ;
		int nblepos = 0 ;
		int nblewrongpos = 0 ;
		int nblelewrongpos = 0 ;
		int nblelepos = 0 ;
		*/
		int nbwsadd = 0 ;
		int nbleadd = 0 ;
		int nbtrans = 0 ;
		int nbwslost1 = 0 ;
		int nbwslost2 = 0 ;
		CounterSet nbwskept = new CounterSet() ;
		CounterSet nbcfmiss = new CounterSet() ;
		CounterSet nbcf = new CounterSet() ;
		CounterSet nbcfcf = new CounterSet() ;
		CounterSet nbcfelse = new CounterSet() ;
		CounterSet nbcfle = new CounterSet() ;
		CounterSet nblemiss = new CounterSet() ;
		CounterSet nble = new CounterSet() ;
		CounterSet nbleposmisstle = new CounterSet() ;
		CounterSet nbleposmisssle = new CounterSet() ;
		CounterSet nblepos = new CounterSet() ;
		CounterSet nblewrongpos = new CounterSet() ;
		CounterSet nblelewrongpos = new CounterSet() ;
		CounterSet nblelepos = new CounterSet() ;

		int nbTransLinked = 0 ;
		int nbTransNotLinked = 0 ;

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

					// Register results in temporary Model
					Resource translation = tmpModel.createResource(trans.getURI());

					Set<Resource> res = (resSenseNum.isEmpty()) ? resSim : resSenseNum;

					if (res != null && !res.isEmpty()) {
						for (Resource ws : res) {
							tmpModel.add(tmpModel.createStatement(translation, DBnaryOnt.isTranslationOf, tmpModel.createResource(ws.getURI())));
							nbwsadd = nbwsadd+1 ;
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

        // add isTranslationOf links to Lexicalentries
        StmtIterator isTranslationOf = inputModel.listStatements(null, DBnaryOnt.isTranslationOf, (RDFNode) null);
        while (isTranslationOf.hasNext()) {
        	nbleadd = nbleadd+1 ;
            Statement next = isTranslationOf.next();
            tmpModel.add(next);
        }
        StmtIterator translationTypes = inputModel.listStatements(null, RDF.type, DBnaryOnt.Translation);
        while (translationTypes.hasNext()) {
            Statement next = translationTypes.next();
            tmpModel.add(next);
        }

		// add relation to writtenForm
		StmtIterator writtenforms = inputModel.listStatements(null, DBnaryOnt.writtenForm, (RDFNode) null);
		while (writtenforms.hasNext()) {
			Statement next = writtenforms.next();
			Resource trans = next.getSubject();
			RDFNode wf = next.getObject();
			tmpModel.add(tmpModel.createStatement(tmpModel.createResource(trans.getURI()), DBnaryOnt.writtenForm, wf));
		}

		// add relation to targetLanguage
		StmtIterator targetlanguages = inputModel.listStatements(null, DBnaryOnt.targetLanguage, (RDFNode) null);
		while (targetlanguages.hasNext()) {
			Statement next = targetlanguages.next();
			Resource trans = next.getSubject();
			Resource tl = next.getResource();
			tmpModel.add(tmpModel.createStatement(tmpModel.createResource(trans.getURI()), DBnaryOnt.targetLanguage, tmpModel.createResource(tl.getURI())));
		}

		// add relation to usage
		StmtIterator usages = inputModel.listStatements(null, DBnaryOnt.usage, (RDFNode) null);
		while (usages.hasNext()) {
			Statement next = usages.next();
			Resource trans = next.getSubject();
			RDFNode use = next.getObject();
			tmpModel.add(tmpModel.createStatement(tmpModel.createResource(trans.getURI()), DBnaryOnt.usage, use));
		}



		// process the temporary model
		StmtIterator stmt = tmpModel.listStatements(null, RDF.type, DBnaryOnt.Translation) ;
		while(stmt.hasNext()){
			Statement next = stmt.next() ;
			Resource e = next.getSubject();
			boolean transLinked = false ;

			StmtIterator stmtws = e.listProperties(DBnaryOnt.isTranslationOf);
			if(stmtws != null){
				while(stmtws.hasNext()){
					Statement statement = stmtws.next() ;
					Resource ws = statement.getResource().inModel(inputModel);
					nbtrans = nbtrans + 1 ;
					Statement stmtwf = e.getProperty(DBnaryOnt.writtenForm);
					Statement stmttl = e.getProperty(DBnaryOnt.targetLanguage);
					if(stmtwf != null && stmttl != null){
						RDFNode wf = stmtwf.getObject();
						//int nbLexicalEntries = 0 ;
						//int nbLexEntriesPoS = 0 ;
						// get canonical form
						RDFNode tl = stmttl.getObject();
						String l = guessLanguage(""+tl);
						String directory = tdbFolder + File.separator + l ;

						if(!(new File(directory).exists())){ // no extraction is available for this language
							nbwslost2 = nbwslost2 + 1 ;
						} else {
							nbwskept.incr(l) ;// = nbwskept + 1 ;
							Dataset dataset = TDBFactory.createDataset(directory) ;
							dataset.begin(ReadWrite.READ) ;
							Model model = dataset.getDefaultModel() ;

							//nbKept = nbKept+1 ;

							StmtIterator stmtcf = model.listStatements(null, OntolexOnt.writtenRep, wf); // can give statement that are lexicalentries and not cf
							boolean stmtcfIsEmpty = true ;
							// si vide nbcfmiss+1
							while(stmtcf.hasNext()){
								stmtcfIsEmpty=false;
								nbcf.incr(l);// = nbcf + 1 ;
								Statement stm = stmtcf.next();
								Resource cf = stm.getSubject(); // not necessarily a canonical form, it  can be a lexical entry
								// check whether we have a lexical entry or a canonical form
								//Statement s = cf.getProperty(RDF.type) ;
								if(cf.hasProperty(RDF.type,OntolexOnt.Form)/*s!=null && s.getResource().equals(LemonOnt.Form)*/){
									nbcfcf.incr(l);// = nbcfcf + 1 ;
									// get LexicalEntry
									StmtIterator stmtle = model.listStatements(null,OntolexOnt.canonicalForm,cf);
									// si vide nblemiss+1
									boolean stmtleIsEmpty = true ;
									while(stmtle.hasNext()){
										stmtleIsEmpty = false ;
										nble.incr(l);// = nble + 1 ;
										Statement statementLexEntry = stmtle.next() ;
										Resource le = statementLexEntry.getSubject();
										//nbLexicalEntries = nbLexicalEntries+1 ;
										// check the part of speech
										Statement st = le.getProperty(LexinfoOnt.partOfSpeech) ;
										if(st != null) {
											Resource posLE =  st.getResource();
											Model source = modelMap.get(lang);
											if (ws.hasProperty(RDF.type, OntolexOnt.LexicalEntry)||
                                                    ws.hasProperty(RDF.type, OntolexOnt.Word) ||
                                                    ws.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)) {
                                                Statement pos = ws.getProperty(LexinfoOnt.partOfSpeech);
                                                if(pos != null) {
                                                    Resource posWS = pos.getResource();
                                                    if (posLE.equals(posWS)) {
                                                        //nbLexEntriesPoS = nbLexEntriesPoS+1 ;
                                                        //outputModel.add(outputModel.createStatement(outputModel.createResource(r.getURI()), LemonOnt.canonicalForm, outputModel.createResource(le.getURI()))); // lexical entry to lexical entry
                                                        outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), VarTransOnt.translatableAs, outputModel.createResource(le.getURI()))); //ws to lexical entry
                                                        nblepos.incr(l);// = nblepos + 1 ;
														transLinked = true ;
                                                        log.debug("Linked Lexical Entry {} "+lang+" to {} "+l+" with POS {}", ws.getLocalName(), le.getLocalName(), posWS);
                                                    } else {
                                                        log.debug("Different POS between {} "+lang+" and {} "+l+" // {} is not {}", ws.getLocalName(), le.getLocalName(), posWS, posLE);
                                                        nblewrongpos.incr(l);// = nblewrongpos + 1 ;
                                                    }
                                                } else {
                                                    nbleposmisssle.incr(l);// = nbleposmisssle + 1 ;
                                                }
                                            } else {
                                                // We have a word sense

                                                StmtIterator stmtwsPoS = source.listStatements(null, OntolexOnt.sense, ws);
                                                int nbsense = 0;
                                                while (stmtwsPoS.hasNext()) {
                                                    nbsense = nbsense + 1;
                                                    Statement stmtpos = stmtwsPoS.next();
                                                    Resource r = stmtpos.getSubject();
                                                    Statement pos = r.getProperty(LexinfoOnt.partOfSpeech);
                                                    if (pos != null) {
                                                        Resource posWS = pos.getResource();
                                                        if (posLE.equals(posWS)) {
                                                            //nbLexEntriesPoS = nbLexEntriesPoS+1 ;
                                                            //outputModel.add(outputModel.createStatement(outputModel.createResource(r.getURI()), LemonOnt.canonicalForm, outputModel.createResource(le.getURI()))); // lexical entry to lexical entry
                                                            outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), VarTransOnt.translatableAs, outputModel.createResource(le.getURI()))); //ws to lexical entry
                                                            nblepos.incr(l);// = nblepos + 1;
															transLinked = true ;
                                                            log.debug("Linked Lexical Entry {} "+lang+" to {} "+l+" with POS {}", ws.getLocalName(), le.getLocalName(), posWS);
                                                        } else {
                                                            log.debug("Different POS between {} "+lang+" and {} "+l+" // {} is not {}", ws.getLocalName(), le.getLocalName(), posWS, posLE);
                                                            nblewrongpos.incr(l);// = nblewrongpos + 1;
                                                        }
                                                    } else {
                                                        nbleposmisssle.incr(l);// = nbleposmisssle + 1;
                                                    }
                                                }
                                                if (nbsense != 1) {
                                                	// Not supposed to happen
                                                    log.debug("(Looking for the part of speech) nb of lexical entries with the sense we're looking for : " + nbsense);
                                                }
                                            }
										} else {
											nbleposmisstle.incr(l);// = nbleposmisstle + 1 ;
										}
									}
									if(stmtleIsEmpty){
										nblemiss.incr(l);// = nblemiss+1 ;
									}
								}else if(cf.hasProperty(RDF.type,OntolexOnt.LexicalEntry)/*s!=null && s.getResource().equals(LemonOnt.LexicalEntry)*/){
									nbcfle.incr(l);// = nbcfle + 1 ;
									Resource le = stm.getSubject();
									log.debug("Lexical Entry with a lemon:writtenRep : {}",le.getLocalName()) ;
									Statement st = le.getProperty(LexinfoOnt.partOfSpeech) ;
									if(st != null){
										Resource posLE =  st.getResource();
										Model source = modelMap.get(lang);

										if (ws.hasProperty(RDF.type, OntolexOnt.LexicalEntry)||
												ws.hasProperty(RDF.type, OntolexOnt.Word) ||
												ws.hasProperty(RDF.type, OntolexOnt.MultiWordExpression)) {
											Statement pos = ws.getProperty(LexinfoOnt.partOfSpeech);
											if(pos != null) {
												Resource posWS = pos.getResource();
												if (posLE.equals(posWS)) {
													//nbLexEntriesPoS = nbLexEntriesPoS+1 ;
													//outputModel.add(outputModel.createStatement(outputModel.createResource(r.getURI()), LemonOnt.canonicalForm, outputModel.createResource(le.getURI()))); // lexical entry to lexical entry
													outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), VarTransOnt.translatableAs, outputModel.createResource(le.getURI()))); //ws to lexical entry
													nblelepos.incr(l);// = nblelepos + 1 ;
													transLinked = true ;
                                                    log.debug("Linked Lexical Entry {} "+lang+" to {} "+l+" with POS {}", ws.getLocalName(), le.getLocalName(), posWS);
												} else {
                                                    log.debug("Different POS between {} "+lang+" and {} "+l+" // {} is not {}", ws.getLocalName(), le.getLocalName(), posWS, posLE);
													nblelewrongpos.incr(l);// = nblelewrongpos + 1 ;
												}
											} else {
												nbleposmisssle.incr(l);// = nbleposmisssle + 1 ;
											}
										} else {
											// We have a word sense

											StmtIterator stmtwsPoS = source.listStatements(null, OntolexOnt.sense, ws);
											int nbsense = 0;
											while (stmtwsPoS.hasNext()) {
												nbsense = nbsense + 1;
												Statement stmtpos = stmtwsPoS.next();
												Resource r = stmtpos.getSubject();
												Statement pos = r.getProperty(LexinfoOnt.partOfSpeech);
												if (pos != null) {
													Resource posWS = pos.getResource();
													if (posLE.equals(posWS)) {
														//nbLexEntriesPoS = nbLexEntriesPoS+1 ;
														//outputModel.add(outputModel.createStatement(outputModel.createResource(r.getURI()), LemonOnt.canonicalForm, outputModel.createResource(le.getURI()))); // lexical entry to lexical entry
														outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), VarTransOnt.translatableAs, outputModel.createResource(le.getURI()))); //ws to lexical entry
														nblelepos.incr(l);// = nblelepos + 1;
														transLinked = true ;
														log.debug("Linked Lexical Entry {} "+lang+" to {} "+l+" with POS {}", ws.getLocalName(), le.getLocalName(), posWS);
													} else {
														log.debug("Different POS between {} "+lang+" and {} "+l+" // {} is not {}", ws.getLocalName(), le.getLocalName(), posWS, posLE);
														nblelewrongpos.incr(l);// = nblelewrongpos + 1;
													}
												} else {
													nbleposmisssle.incr(l);// = nbleposmisssle + 1;
												}
											}
											if (nbsense != 1) {
												// Not supposed to happen
												log.debug("(Looking for the part of speech) nb of lexical entries with the sense we're looking for : " + nbsense);
											}
										}
									}else{
										nbleposmisstle.incr(l);// = nbleposmisstle + 1 ;
									}
								}else{
									nbcfelse.incr(l);// = nbcfelse + 1 ;
									log.debug("lemon:WrittenRep {} on something else : {}",wf,cf.getLocalName()) ;
								}
							}
							if(stmtcfIsEmpty){
								nbcfmiss.incr(l);// = nbcfmiss+1 ;
							}
							dataset.end() ;
							//System.out.println(ws+" "+wf+"\n\t"+nbLexicalEntries+" LexicalEntries\n\t"+nbLexEntriesPoS+" LexicalEntries with correct part of speech") ;
						}
					}else{
						nbwslost1 = nbwslost1 + 1 ;
						log.debug("Word Sense or Target Language missing for this translation : {}",e.getLocalName()) ;
					}
					/*Statement stmttl = e.getProperty(DBnaryOnt.targetLanguage);
					if(stmttl != null){
						Resource tl = stmttl.getResource();
						outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), DBnaryOnt.targetLanguage, outputModel.createResource(tl.getURI())));
					}
					Statement stmtuse = e.getProperty(DBnaryOnt.usage);
					if(stmtuse != null) {
						RDFNode use = stmtuse.getObject();
						outputModel.add(outputModel.createStatement(outputModel.createResource(ws.getURI()), DBnaryOnt.writtenForm, use));
					}*/
				}
			}
			if(transLinked){
				nbTransLinked = nbTransLinked + 1 ;
			}else{
				nbTransNotLinked = nbTransNotLinked + 1 ;
			}
		}
		/*
		System.out.println(nbwsadd+"\ttranslations from a word sense added") ;
		System.out.println(nbleadd+"\ttranslations from a lexical entry added") ;
		System.out.println();
		System.out.println(nbtrans+"\ttranslations from a word sense or lexical entry in the model") ;
		System.out.println();
		System.out.println(nbwslost1+"\ttranslations to a word sense lost because written form or target language missing") ;
		System.out.println(nbwslost2+"\ttranslations to a word sense lost because model unavailable in this language") ;
		System.out.println(nbwskept+"\ttranslations to a word sense kept") ;
		System.out.println();
		System.out.println(nbcfmiss+"\ttimes where canonical form or lexical entry corresponding to the written form was not found") ;
		System.out.println(nbcf+"\ttimes where canonical form or lexical entry corresponding to the written form was found") ;
		System.out.println();
		System.out.println(nbcfcf+"\ttimes where it was indeed a canonical form");
		System.out.println(nbcfle+"\ttimes where it was a lexical entry");
		System.out.println(nbcfelse+"\ttimes where it was neither a canonical form nor a lexical entry");
		System.out.println();
		System.out.println(nblemiss+"\ttimes where no lexical entry corresponding to this canonical form was found") ;
		System.out.println(nble+"\ttimes where a lexical entry corresponding to this canonical form was found") ;
		System.out.println();
		System.out.println(nblepos+"\ttimes where a lexical entry corresponding to this canonical form with the right part of speech was found") ;
		System.out.println(nblelepos+"\ttimes where a lexical entry with the right part of speech was found directly (not through canonical form)") ;
		System.out.println();
		System.out.println(nblewrongpos+"\ttimes where a lexical entry corresponding to this canonical form with the wrong part of speech was found") ;
		System.out.println(nblelewrongpos+"\ttimes where a lexical entry with the wrong part of speech was found directly (not through canonical form)") ;
		System.out.println();
		System.out.println(nbleposmisstle+"\ttimes where part of speech was missing from the target") ;
		System.out.println(nbleposmisssle+"\ttimes where part of speech was missing from the source") ;
		*/
		log.debug(nbwsadd+"\ttranslations from a word sense added") ;
		log.debug(nbleadd+"\ttranslations from a lexical entry added") ;
		log.debug("\n");
		log.debug(nbtrans+"\ttranslations from a word sense or lexical entry in the model") ;
		log.debug("\n");
		log.debug(nbwslost1+"\ttranslations to a word sense lost because written form or target language missing") ;
		log.debug(nbwslost2+"\ttranslations to a word sense lost because model unavailable in this language") ;
		log.debug("translations to a word sense kept") ;
		nbwskept.logCounters(log);
		log.debug("\n");
		log.debug("times where canonical form or lexical entry corresponding to the written form was not found") ;
		nbcfmiss.logCounters(log);
		log.debug("times where canonical form or lexical entry corresponding to the written form was found") ;
		nbcf.logCounters(log);
		log.debug("\n");
		log.debug("times where it was indeed a canonical form");
		nbcfcf.logCounters(log);
		log.debug("times where it was a lexical entry");
		nbcfle.logCounters(log);
		log.debug("times where it was neither a canonical form nor a lexical entry");
		nbcfelse.logCounters(log);
		log.debug("\n");
		log.debug("times where no lexical entry corresponding to this canonical form was found") ;
		nblemiss.logCounters(log);
		log.debug("times where a lexical entry corresponding to this canonical form was found") ;
		nble.logCounters(log);
		log.debug("\n");
		log.debug("times where a lexical entry corresponding to this canonical form with the right part of speech was found") ;
		nblepos.logCounters(log);
		log.debug("times where a lexical entry with the right part of speech was found directly (not through canonical form)") ;
		nblelepos.logCounters(log);
		log.debug("\n");
		log.debug("times where a lexical entry corresponding to this canonical form with the wrong part of speech was found") ;
		nblewrongpos.logCounters(log);
		log.debug("times where a lexical entry with the wrong part of speech was found directly (not through canonical form)") ;
		nblelewrongpos.logCounters(log);
		log.debug("\n");
		log.debug("times where part of speech was missing from the target") ;
		nbleposmisstle.logCounters(log);
		log.debug("times where part of speech was missing from the source") ;
		nbleposmisssle.logCounters(log);
		log.debug("{} translations where at least one link was created",nbTransLinked) ;
		log.debug("{} translations where no links were created",nbTransNotLinked) ;
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

