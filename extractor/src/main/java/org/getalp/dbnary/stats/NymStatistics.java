package org.getalp.dbnary.stats;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.DbnaryModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class NymStatistics {

	private static int countRelations(Property prop, Model m1) {
		ResIterator resit = m1.listResourcesWithProperty(prop);
		int nb = 0;

		while(resit.hasNext()) {
			resit.next();
			nb++;
		}
		resit.close();

		return nb;
	}
	
	public static void printStats(Model m1, String language, PrintWriter out, boolean verbose) {
		
		if (verbose) {
			out.println(getHeaders());
		}
		
		//out.print(ISO639_3.sharedInstance.getLanguageNameInEnglish(language));
		out.print(countRelations(DbnaryModel.synonymProperty, m1));
		out.print("," + countRelations(DbnaryModel.nearSynonymProperty, m1));
		out.print("," + countRelations(DbnaryModel.antonymProperty, m1));
		out.print("," + countRelations(DbnaryModel.hypernymProperty, m1));
		out.print("," + countRelations(DbnaryModel.hyponymProperty, m1));
		out.print("," + countRelations(DbnaryModel.meronymProperty, m1));
		out.print("," + countRelations(DbnaryModel.holonymProperty, m1));

		out.flush();

	}

	public static String getHeaders() {
		return "syn,qsyn,ant,hyper,hypo,mero,holo";
	}

	public static void printStats(Model m1, String language, PrintWriter printWriter) {
		printStats(m1, language, printWriter, false);
	}
}
