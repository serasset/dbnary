package org.getalp.dbnary.stats;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.DbnaryModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.getalp.dbnary.LemonOnt;

public class GeneralStatistics {

	private static int countResourcesOfType(Resource type, Model m1) {
		ResIterator resit = m1.listResourcesWithProperty(RDF.type, type);
		int nb = 0;
		while(resit.hasNext()) {
			resit.next();
			nb++;
		}
		resit.close();
		return nb;
	}
	
	public static void printStats(Model m1, String language, PrintWriter out) {
		printStats(m1,language,out,false);
	}
	
	public static void printStats(Model m1, String language, PrintWriter out, boolean verbose) {
		
		// Number of Lexical Entries

		int nble = countResourcesOfType(LemonOnt.LexicalEntry, m1);
		int nblv = countResourcesOfType(DBnaryOnt.Vocable, m1);
		int nblw = countResourcesOfType(LemonOnt.Word, m1);
		int nblp = countResourcesOfType(LemonOnt.Phrase, m1);
		
				
		int nbEquiv = countResourcesOfType(DBnaryOnt.Translation, m1);
		int nbsense = countResourcesOfType(LemonOnt.LexicalSense, m1);
		
		if (verbose) {
			out.println(getHeaders());
		}
		
		//out.print(ISO639_3.sharedInstance.getLanguageNameInEnglish(language));
		out.print((nble + nblw + nblp));
		out.print("," + nblv);
		out.print("," + nbsense);
		out.print("," + nbEquiv);
		
		out.flush();
	}
	
	public static String getHeaders() {
		return "Entries,Vocables,Senses,Translations";
	}

}
