package org.getalp.dbnary.experiment.disambiguation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.similarity.string.TverskiIndex;
import org.getalp.dbnary.experiment.translation.BingAPITranslator;
import org.getalp.dbnary.experiment.translation.CachedTranslator;
import org.getalp.dbnary.experiment.translation.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wcohen.ss.ScaledLevenstein;

public class XlingualTverskyBasedTranslationDisambiguationMethod implements
DisambiguationMethod {

	private double delta;
	//    private double alpha;
	//    private double beta;
	private TverskiIndex tversky;
	private Map<String, Model> models;
	private Translator translator;

	private Logger log = LoggerFactory.getLogger(XlingualTverskyBasedTranslationDisambiguationMethod.class);


	public XlingualTverskyBasedTranslationDisambiguationMethod(Map<String, Model> models, double alpha, double beta, double threshold, String translatorId, String translatorPass, String translationCache) {
		delta = threshold;
		//        this.alpha = alpha;
		//        this.beta = beta;
		tversky = new TverskiIndex(alpha, beta, true, false, new ScaledLevenstein());
		this.models = models;
		translator = new CachedTranslator(translationCache, new BingAPITranslator(translatorId, translatorPass), true);
	}

	private class WeigthedSensePair {
		protected double weight;
		protected Resource sourceSense;
		protected Resource targetSense;

		public WeigthedSensePair(double weight, Resource sourceSense, Resource targetSense) {
			super();
			this.weight = weight;
			this.sourceSense = sourceSense;
			this.targetSense = targetSense;
		}
	}

	@Override
	public Set<Resource> selectWordSenses(Resource lexicalEntry,
			Object context) throws InvalidContextException,
			InvalidEntryException {
		HashSet<Resource> res = new HashSet<Resource>();

		if (! lexicalEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType) &&
				!lexicalEntry.hasProperty(RDF.type, DbnaryModel.wordEntryType) && 
				!lexicalEntry.hasProperty(RDF.type, DbnaryModel.phraseEntryType)) 
			throw new InvalidEntryException("Expecting a LEMON Lexical Entry.");
		if (context instanceof Resource) {
			Resource trans = (Resource) context;
			if (! trans.hasProperty(RDF.type, DbnaryModel.translationType)) throw new InvalidContextException("Expecting a DBnary Translation Resource.");

			String slang = getLanguage(lexicalEntry);
			String tlang = getTargetLanguage(trans);
			
			List<Resource> targets = getTargetSenses(trans, null);

			if (null != targets && ! targets.isEmpty()) {
				List<Resource> swsList = getLexicalSenses(lexicalEntry);

				ArrayList<WeigthedSensePair> weightedList = new ArrayList<WeigthedSensePair>();

				for (Resource sws : swsList) {
					Statement sdRef = sws.getProperty(DbnaryModel.lemonDefinitionProperty);
					Statement sdVal = sdRef.getProperty(DbnaryModel.lemonValueProperty);
					String sdef = sdVal.getString();
					
					if (! "eng".equals(slang)) 
						sdef = translator.translate(sdef, slang, "eng");
							
					for (Resource tws : targets) {
						Statement tdRef = tws.getProperty(DbnaryModel.lemonDefinitionProperty);
						Statement tdVal = tdRef.getProperty(DbnaryModel.lemonValueProperty);
						String tdef = tdVal.getString();
						
						if (! "eng".equals(slang)) 
							tdef = translator.translate(tdef, tlang, "eng");

						double sim = tversky.compute(sdef, tdef);
						log.debug("[{}]: \"{}\" <<<===>>> [{}]: \"{}\" ----> {}", slang, sdef, tlang, tdef, sim);
						insert(weightedList, sws, tws, sim);
					}
				}
				if (weightedList.size() == 0) return res;

				int i = 0;
				double worstScore = weightedList.get(0).weight - delta;
				while(i != weightedList.size() && weightedList.get(i).weight >= worstScore) {
					res.add(weightedList.get(i).sourceSense);
					i++;
				}
				
				// TODO: adapt whole program to get a couple (source/target) of word senses.
				
			}
		} else {
			throw new InvalidContextException("Expecting a JENA Resource.");
		}

		return res;
	}

	StmtIterator getTranslationLexicalEntryStmtIterator(Resource translation, String targetLang) {
		// TODO: duplicate code with Translation closure. Keep this one and factorize...
		String writtenForm = translation.getProperty(DbnaryModel.equivalentTargetProperty).getString();
		String uri = DbnaryModel.DBNARY_NS_PREFIX + "/" + targetLang + "/" + DbnaryModel.uriEncode(writtenForm);
		Resource r = models.get(targetLang).getResource(uri);
		return models.get(targetLang).listStatements(r, DbnaryModel.refersTo, (RDFNode) null);
	}

	private List<Resource> getTargetSenses(Resource trans, String pos) {
		String lang = getTargetLanguage(trans);
		List<Resource> res = new ArrayList<Resource>();
		

		if (models.containsKey(lang)) {
			StmtIterator lexEntries = getTranslationLexicalEntryStmtIterator(trans, lang);
			while (lexEntries.hasNext()) {
				Statement lnext = lexEntries.next();
				Statement stmtPos = lnext.getObject().asResource().getProperty(DbnaryModel.posProperty);
				String foreignpos = null;
				if (stmtPos != null) {
					foreignpos = stmtPos.getResource().getLocalName();
				}
				if (pos == null || (pos != null && foreignpos != null && pos.equals(foreignpos))) {
					Resource lexEntryNode = lnext.getResource();

					res.addAll(getLexicalSenses(lexEntryNode));

				}
			}
		}

		return res;
	}

	private String getTargetLanguage(Resource trans) {
		String lang;
		Resource lexvoLang = trans.getPropertyResourceValue(DbnaryModel.targetLanguageProperty);
		if (lexvoLang == null) {
			lang = trans.getProperty(DbnaryModel.targetLanguageCodeProperty).getString();
		} else {
			lang = lexvoLang.getLocalName();
		}
		return ISO639_3.sharedInstance.getIdCode(lang);
	}

	private String getLanguage(Resource lexEntry) {
		return ISO639_3.sharedInstance.getIdCode(lexEntry.getProperty(DbnaryModel.languageProperty).getString());
	}

	private List<Resource> getLexicalSenses(Resource lexEntryNode) {
		List<Resource> res = new ArrayList<Resource>();
		StmtIterator ws = lexEntryNode.listProperties(DbnaryModel.lemonSenseProperty);
		while (ws.hasNext()) {
			res.add(ws.next().getResource());
		}
		return res;
	}

	private void insert(ArrayList<WeigthedSensePair> weightedList,
			Resource sws, Resource tws, double sim) {
		weightedList.add(null);
		int i = weightedList.size()-1;
		while (i != 0 && weightedList.get(i-1).weight < sim) {
			weightedList.set(i,weightedList.get(i-1));
			i--;
		}
		weightedList.set(i, new WeigthedSensePair(sim, sws, tws));
	}

}
