package org.getalp.dbnary.cli;

import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RDFDiff {
	static Model m1;
	static Model m2;
	
	public static void main(String args[]) {
		
		if (args.length != 2) {
			usage();
			System.exit(1);
		}
		
		m1 = ModelFactory.createDefaultModel();
		m2 = ModelFactory.createDefaultModel();
		
		// read the RDF/XML files
		System.err.println("Reading first model.");
		m1.read(args[0], "TURTLE");
		System.err.println("Reading second model.");
		m2.read(args[1], "TURTLE");
		System.err.println("Computing differences.");

		// merge the Models
		Model model = m1.difference(m2);
		
		for (Entry<String, String> e : m1.getNsPrefixMap().entrySet()) {
			model.setNsPrefix(e.getKey(), e.getValue());
		}
		// print the Model as RDF/XML
		model.write(System.out, "TURTLE");
	}

	private static void usage() {
		System.out.println("Usage: java -Xmx8G " + RDFDiff.class.getCanonicalName() + " url1 url2");
	}
}
