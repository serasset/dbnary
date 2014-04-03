package org.getalp.dbnary.experiment.disambiguation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.getalp.dbnary.DbnaryModel;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class SenseNumberBasedTranslationDisambiguationMethod implements
		DisambiguationMethod {

	public SenseNumberBasedTranslationDisambiguationMethod() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<Resource> selectWordSenses(Resource lexicalEntry,
			Object context) throws InvalidContextException,
			InvalidEntryException {
		if (! lexicalEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType)) throw new InvalidEntryException("Expecting a LEMON Lexical Entry.");
		if (context instanceof Resource) {
			Resource trans = (Resource) context;
			if (! trans.hasProperty(RDF.type, DbnaryModel.translationType)) throw new InvalidContextException("Expecting a DBnary Translation Resource.");
			
			Statement s = trans.getProperty(DbnaryModel.senseNumberProperty);
			
			if (null != s) {
				// Process sense number
				// System.out.println("Avoiding treating " + s.toString());
				return selectNumberedSenses(lexicalEntry, s);
			} else 
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

	private void addNumberedWordSenseToResult(Set<Resource> res,
			Resource lexicalEntry, String n) {
		StmtIterator senses = lexicalEntry.listProperties(DbnaryModel.lemonSenseProperty);
		while (senses.hasNext()) {
			Resource sense = senses.next().getResource();
			Statement senseNumStatement = sense.getProperty(DbnaryModel.senseNumberProperty);
			if (n.equalsIgnoreCase(senseNumStatement.getString())) {
				res.add(sense);
			}
		}
	}


	public ArrayList<String> getSenseNumbers(String nums) {
		ArrayList<String> ns = new ArrayList<String>();

		if (nums.contains(",")) {
			String[] ni = nums.split(",");
			for (int i = 0; i < ni.length; i++) {
				ns.addAll(getSenseNumbers(ni[i]));
			}
		} else if (nums.contains("-") || nums.contains("—") || nums.contains("–")) {
			String[] ni = nums.split("[-—–]");
			if (ni.length != 2) {
				System.err.append("Strange split on dash: " + nums);
			} else {
				try {
					int s = Integer.parseInt(ni[0].trim());
					int e = Integer.parseInt(ni[1].trim());

					if (e <= s) {
						System.err.println("end of range is lower than beginning in: " + nums);
					} else {
						for (int i = s; i <= e ; i++) {
							ns.add(Integer.toString(i));
						}
					}
				} catch (NumberFormatException e) {
					System.err.println(e.getLocalizedMessage());
				}
			}
		} else {
			try {
				ns.add(nums.trim());
			}  catch (NumberFormatException e) {
				System.err.println(e.getLocalizedMessage() + ": " + nums);
			}
		}
		return ns;
	}

}
