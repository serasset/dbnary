package org.getalp.dbnary.enhancer.disambiguation;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SenseNumberBasedTranslationDisambiguationMethod implements
		DisambiguationMethod {

    public static int NUMSN=0;

	public SenseNumberBasedTranslationDisambiguationMethod() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<Resource> selectWordSenses(Resource lexicalEntry,
			Object context) throws InvalidContextException,
			InvalidEntryException {
		if (! lexicalEntry.hasProperty(RDF.type, OntolexOnt.LexicalEntry) &&
				!lexicalEntry.hasProperty(RDF.type, OntolexOnt.Word) &&
				!lexicalEntry.hasProperty(RDF.type, OntolexOnt.MultiWordExpression))
			throw new InvalidEntryException("Expecting an ontolex Lexical Entry.");
		if (context instanceof Resource) {
			Resource trans = (Resource) context;
			if (! trans.hasProperty(RDF.type, DBnaryOnt.Translation)) throw new InvalidContextException("Expecting a DBnary Translation Resource.");
            // TODO: the sense number property is not defined for translations... However, it is added by the preprocessing.
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

	private void addNumberedWordSenseToResult(Set<Resource> res,
			Resource lexicalEntry, String n) {
		StmtIterator senses = lexicalEntry.listProperties(OntolexOnt.sense);
		while (senses.hasNext()) {
			Resource sense = senses.next().getResource();
			Statement senseNumStatement = sense.getProperty(DBnaryOnt.senseNumber);
			if (n.equalsIgnoreCase(senseNumStatement.getString())) {
                //System.err.println(n+" | "+senseNumStatement.getString());
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
