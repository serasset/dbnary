package org.getalp.dbnary.experiment.disambiguation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.similarity.string.TverskiIndex;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wcohen.ss.ScaledLevenstein;

public class TverskyBasedTranslationDisambiguationMethod implements
		DisambiguationMethod {

	private double delta;
	private TverskiIndex tversky = new TverskiIndex(.1, .9, true, false, new ScaledLevenstein());
	
	public TverskyBasedTranslationDisambiguationMethod(double threshold) {
		delta = threshold;
	}

	private class WeigthedSense {
		protected double weight;
		protected Resource sense;
		
		public WeigthedSense(double weight, Resource sense) {
			super();
			this.weight = weight;
			this.sense = sense;
		}
	}
	
	@Override
	public Set<Resource> selectWordSenses(Resource lexicalEntry,
			Object context) throws InvalidContextException,
			InvalidEntryException {
		HashSet<Resource> res = new HashSet<Resource>();
      
		if (! lexicalEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType)) throw new InvalidEntryException("Expecting a LEMON Lexical Entry.");
		if (context instanceof Resource) {
			Resource trans = (Resource) context;
			if (! trans.hasProperty(RDF.type, DbnaryModel.translationType)) throw new InvalidContextException("Expecting a DBnary Translation Resource.");

            Statement glossStmt = trans.getProperty(DbnaryModel.glossProperty);

			if (null != glossStmt) {
                String gloss = glossStmt.getString();
				ArrayList<WeigthedSense> weightedList = new ArrayList<WeigthedSense>();

				// get a list of wordsenses, sorted by decreasing similarity.
				StmtIterator senses = lexicalEntry.listProperties(DbnaryModel.lemonSenseProperty);
				while (senses.hasNext()) {
					Statement nextSense = senses.next();
					Resource wordsense = nextSense.getResource();
					Statement dRef = wordsense.getProperty(DbnaryModel.lemonDefinitionProperty);
					Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
					String deftext = dVal.getObject().toString();

					double sim = tversky.compute(deftext, gloss);

					insert(weightedList, wordsense, sim);
				}
				
				if (weightedList.size() == 0) return res;
				
				int i = 0;
				double worstScore = weightedList.get(0).weight - delta;
				while(i != weightedList.size() && weightedList.get(i).weight >= worstScore) {
					res.add(weightedList.get(i).sense);
					i++;
				}
			}
		} else {
			throw new InvalidContextException("Expecting a JENA Resource.");
		}
		
		return res;
	}

	private void insert(ArrayList<WeigthedSense> weightedList,
			Resource wordsense, double sim) {
		weightedList.add(null);
		int i = weightedList.size()-1;
		while (i != 0 && weightedList.get(i-1).weight < sim) {
			weightedList.set(i,weightedList.get(i-1));
			i--;
		}
		weightedList.set(i, new WeigthedSense(sim, wordsense));
	}

}
