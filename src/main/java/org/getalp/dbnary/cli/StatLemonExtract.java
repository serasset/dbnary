package org.getalp.dbnary.cli;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.LemonBasedRDFDataHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class StatLemonExtract extends DbnaryModel {

	protected class IncrementableInt {
		int val;
		
		public IncrementableInt() {
			val = 0;
		}
		
		public IncrementableInt(int val) {
			this.val = val;
		}
		
		public void incr() {
			this.val++;
		}
		
		public void incr(int step) {
			this.val = this.val + step;
		}
		
		public String toString() {
			return Integer.toString(this.val);
		}
	}
	
	private static Options options = null; // Command line options

	private static final String LANGUAGE_OPTION = "l";
	private static final String DEFAULT_LANGUAGE = "fra";

	private static final String OUTPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_OUTPUT_FORMAT = "turtle";	

	private static final String COUNT_LANGUAGE_OPTION = "c";
	private static final String DEFAULT_COUNT_LANGUAGE = "eng,fra,deu,por";	

	private CommandLine cmd = null; // Command Line arguments

	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	private String language = DEFAULT_LANGUAGE;
	private String countLanguages = DEFAULT_COUNT_LANGUAGE;

	// TODO: extract iso code from lexvo entity.
	private SortedMap<String, IncrementableInt> counts = new TreeMap<String,IncrementableInt>();
	
	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(LANGUAGE_OPTION, true, 
				"Language (fra, eng or deu). " + DEFAULT_LANGUAGE + " by default.");
		options.addOption(OUTPUT_FORMAT_OPTION, true, 
				"Output format (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). " + DEFAULT_OUTPUT_FORMAT + " by default.");
		options.addOption(COUNT_LANGUAGE_OPTION, true, 
				"Languages to count (as a comma separated list). " + DEFAULT_COUNT_LANGUAGE + " by default.");
	}	

	String[] remainingArgs;

	Model m1;
	
	String NS;
	
	private void initializeTBox(String lang) {
		NS = NSprefix + "/" + lang + "/";
	}
	
	private void loadArgs(String[] args) {
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
			printUsage();
			System.exit(1);
		}

		// Check for args
		if (cmd.hasOption("h")){
			printUsage();
			System.exit(0);
		}

		if (cmd.hasOption(OUTPUT_FORMAT_OPTION)){
			outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
		}
		outputFormat = outputFormat.toUpperCase();

		if (cmd.hasOption(LANGUAGE_OPTION)) {
			language = cmd.getOptionValue(LANGUAGE_OPTION);
			language = ISO639_3.sharedInstance.getIdCode(language);
			//if (! (language.equals("fra") || language.equals("eng") || language.equals("deu") || language.equals("por") || language.equals("ita") || language.equals("fin") )) {
			//	System.err.println("Unknown language: " + language);
			//	printUsage();
			//	System.exit(1);
			//}
		}

		if (cmd.hasOption(COUNT_LANGUAGE_OPTION)){
			countLanguages = cmd.getOptionValue(COUNT_LANGUAGE_OPTION);
		}
		String clangs[] = countLanguages.split(",");
		int i = 0;
		while(i != clangs.length) {
			counts.put(ISO639_3.sharedInstance.getIdCode(clangs[i]), new IncrementableInt());
			i = i + 1;
		}

		remainingArgs = cmd.getArgs();
		if (remainingArgs.length < 1) {
			printUsage();
			System.exit(1);
		}
		
		initializeTBox(language);
		m1 = ModelFactory.createDefaultModel();
		
		if (	outputFormat.equals("RDF") || 
				outputFormat.equals("TURTLE") ||
				outputFormat.equals("NTRIPLE") ||
				outputFormat.equals("N3") ||
				outputFormat.equals("TTL") ||
				outputFormat.equals("RDFABBREV") ) {
			if ("-".equals(remainingArgs[0])) {
				System.err.println("Reading extract from stdin.");
				m1.read(System.in, "file:///dev/stdin", outputFormat);
			} else {
				System.err.println("Reading extract from " + remainingArgs[0]);
				m1.read(remainingArgs[0], outputFormat);
			}
		} else {
			System.err.println("unsupported format :" + outputFormat);
			System.exit(1);
		}
	}


	private String getCode(Resource resource) {
		// TODO Auto-generated method stub
		return resource.getLocalName();
	}

	public static void main(String args[]) {
		StatLemonExtract cliProg = new StatLemonExtract();
		cliProg.loadArgs(args);
		cliProg.stats();
		
	}

	private void stats() {
		
		System.out.println("Stats on RDF file: " + remainingArgs[0]);
		
		// Number of Lexical Entries

		int nble = countResourcesOfType(lexEntryType);
		int nblv = countResourcesOfType(vocableEntryType);
		int nblw = countResourcesOfType(wordEntryType);
		int nblp = countResourcesOfType(phraseEntryType);
		
				
		int nbEquiv = countResourcesOfType(translationType);
		int nbsense = countResourcesOfType(lexicalSenseType);
		
		System.out.println("Language Edition & Entries & Vocables & Senses & Equivalents\\\\");
		System.out.print("\\textbf{" + language  + "} & ");
		System.out.print("" + nble + " (+" + nblw + " words/+ " + nblp + " phrases) & ");
		System.out.print(nblv + " & ");
		System.out.print(nbsense + " & ");
		System.out.println(nbEquiv + " \\\\");
		
		System.out.println("");
		
		System.out.println("Language Edition & syn & qsyn & ant & hyper & hypo & mero & holo \\\\");
		System.out.print("\\textbf{" + language  + "} & ");
		System.out.print(countRelations(synonymProperty) + "& ");
		System.out.print(countRelations(nearSynonymProperty) + "& ");
		System.out.print(countRelations(antonymProperty) + "& ");
		System.out.print(countRelations(hypernymProperty) + "& ");
		System.out.print(countRelations(hyponymProperty) + "& ");
		System.out.print(countRelations(meronymProperty) + "& ");
		System.out.println(countRelations(holonymProperty) + " \\\\ ");
		System.out.println("");
		System.out.println("");

		printEquivalentsStats();
	}
	
	private int countResourcesOfType(Resource type) {
		ResIterator resit = m1.listResourcesWithProperty(RDF.type, type);
		int nb = 0;
		while(resit.hasNext()) {
			nb++;
			resit.next();
		}
		resit.close();
		return nb;
	}

	private int countRelations(Property prop) {
		ResIterator resit = m1.listResourcesWithProperty(prop);
		int nb = 0;

		while(resit.hasNext()) {
			Resource rel = resit.next();

			nb++;
		}
		resit.close();

		return nb;
	}

	private void printEquivalentsStats() {
		// Number of relations
		ResIterator relations = m1.listResourcesWithProperty(RDF.type, translationType);
		HashSet<String> langs = new HashSet<String>();
		int others = 0;
		while(relations.hasNext()) {
			Resource r = relations.next();
			Statement t = r.getProperty(targetLanguageProperty);
			if (null != t) {
				RDFNode lang = t.getObject();
				langs.add(getCode(lang.asResource()));
				if (counts.containsKey(getCode(lang.asResource()))) {
					counts.get(getCode(lang.asResource())).incr();
				} else {
					others = others + 1;
				}
			}
		}
		relations.close();
		
		int total = 0;
			
		for (Entry<String, IncrementableInt> i : counts.entrySet()) {
			total = total + i.getValue().val;
			System.out.print(" & " + i.getKey());
		}
		total = total + others;
		System.out.println("& others & Total \\\\");
		System.out.print(language);
		for (Entry<String, IncrementableInt> i : counts.entrySet()) {
			System.out.print(" & " + i.getValue().val);
		}
		System.out.println(" & " + others + " & " + total + "\\\\");
		
		System.out.println("-------------------------");
		System.out.println(langs.size() + " different target languages.");
		for (String l : langs) {
			System.out.print(l + " ");
		}
		
		
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		String help = 
			"url must point on an RDF model file extracted from wiktionary." +
			System.getProperty("line.separator", "\n") +
			"Displays stats on the LMF based RDF dump.";
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.cli.StatRDFExtract [OPTIONS] url", 
				"With OPTIONS in:", options, 
				help, false);
	}

}
