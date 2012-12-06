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
import org.getalp.dbnary.LemonBasedRDFDataHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class StatLEMONExtract {

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
	
	protected static final String NSprefix = "http://kaiko.getalp.org/dbnary";
	protected static final String DBNARY = NSprefix + "#";
	// protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	protected static final String LEMON = "http://www.monnetproject.eu/lemon#";
	protected static final String LEXINFO = "http://www.lexinfo.net/ontology/2.0/lexinfo#";
	protected static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	
	protected static final Resource lexEntryType;
	protected static final Resource wordEntryType;
	protected static final Resource phraseEntryType;
	protected static final Resource lexicalFormType;
	protected static final Resource translationType;
	protected static final Resource lexicalSenseType;
	// protected Resource definitionType;
	// protected Resource lexicalEntryRelationType;

	protected static final Property canonicalFormProperty;
	protected static final Property lexicalVariantProperty;
	protected static final Property writtenRepresentationProperty;
	
	// DBNARY properties
	protected static final Property dbnaryPosProperty;
	protected static final Resource vocableEntryType;
	protected static final Property refersTo;

	// LEMON properties
	protected static final Property posProperty;
	protected static final Property lemonSenseProperty;
	protected static final Property lemonDefinitionProperty;
	protected static final Property lemonValueProperty;
	protected static final Property languageProperty;
	protected static final Property pronProperty;

	//LMF properties
	// protected Property formProperty;
	protected static final Property isTranslationOf;
	protected static final Property targetLanguageProperty;
	protected static final Property equivalentTargetProperty;
	protected static final Property gloseProperty;
	protected static final Property usageProperty;
	// protected static final Property textProperty;

	protected static final Property synonymProperty ;
	protected static final Property antonymProperty ;
	protected static final Property hypernymProperty ;
	protected static final Property hyponymProperty ;
	protected static final Property nearSynonymProperty ;
	protected static final Property meronymProperty ;
	protected static final Property holonymProperty ;

	
	static Model tBox;

	static {
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		// InputStream fis = LemonBasedRDFDataHandler.class.getResourceAsStream("LMF-rdf-rev14.xml");
		// tBox.read( fis, LMF );
		InputStream lis = LemonBasedRDFDataHandler.class.getResourceAsStream("lemon.ttl");
		tBox.read( lis, LEMON, "TURTLE");

		lexEntryType = tBox.getResource(LEMON + "LexicalEntry");
		lexicalFormType = tBox.getResource(LEMON + "LexicalForm");
		lexicalSenseType = tBox.getResource(LEMON + "LexicalSense");
		canonicalFormProperty = tBox.getProperty(LEMON + "canonicalForm");
		lemonSenseProperty = tBox.getProperty(LEMON + "sense");
		lexicalVariantProperty = tBox.getProperty(LEMON + "lexicalVariant");
		writtenRepresentationProperty =  tBox.getProperty(LEMON + "writtenRep");
		lemonDefinitionProperty = tBox.getProperty(LEMON + "definition");
		lemonValueProperty = tBox.getProperty(LEMON + "value");
		languageProperty = tBox.getProperty(LEMON + "language");
		
		vocableEntryType = tBox.getResource(DBNARY + "vocable");

		translationType = tBox.getResource(DBNARY + "Equivalent");
		// definitionType = tBox.getResource(LMF + "Definition");
		// lexicalEntryRelationType = tBox.getResource(NS + "LexicalEntryRelation");

		// formProperty = tBox.getProperty(NS + "writtenForm");
		targetLanguageProperty = tBox.getProperty(DBNARY + "targetLanguage");
		equivalentTargetProperty = tBox.getProperty(DBNARY + "writtenForm");
		gloseProperty = tBox.getProperty(DBNARY + "glose");
		usageProperty = tBox.getProperty(DBNARY + "usage");
		// textProperty = tBox.getProperty(DBNARY + "text");
		// entryRelationLabelProperty = tBox.getProperty(DBNARY + "label");
		// entryRelationTargetProperty = tBox.getProperty(DBNARY + "target");
		refersTo = tBox.getProperty(DBNARY + "refersTo");
		isTranslationOf = tBox.getProperty(DBNARY + "isTranslationOf");
				
		posProperty = tBox.getProperty(LEXINFO + "partOfSpeech");
		dbnaryPosProperty = tBox.getProperty(DBNARY + "partOfSpeech");
		
		pronProperty = tBox.getProperty(LEXINFO + "pronunciation");

		synonymProperty = tBox.getProperty(DBNARY + "synonym");
		antonymProperty = tBox.getProperty(DBNARY + "antonym");
		hypernymProperty = tBox.getProperty(DBNARY + "hypernym");
		hyponymProperty = tBox.getProperty(DBNARY + "hyponym");
		nearSynonymProperty = tBox.getProperty(DBNARY + "approximateSynonym");
		meronymProperty = tBox.getProperty(DBNARY + "meronym");
		holonymProperty = tBox.getProperty(DBNARY + "holonym");

		Property lxfSynonymProperty = tBox.getProperty(LEXINFO + "synonym");
		Property lxfAntonymProperty = tBox.getProperty(LEXINFO + "antonym");
		Property lxfHypernymProperty = tBox.getProperty(LEXINFO + "hypernym");
		Property lxfHyponymProperty = tBox.getProperty(LEXINFO + "hyponym");
		Property lxfNearSynonymProperty = tBox.getProperty(LEXINFO + "approximateSynonym");

		// non standard nym (not in lexinfo);
		
		Resource nounPOS = tBox.getResource(LEXINFO + "noun");
		Resource adjPOS = tBox.getResource(LEXINFO + "adj");
		Resource properNounPOS = tBox.getResource(LEXINFO + "properNoun");
		Resource verbPOS = tBox.getResource(LEXINFO + "verb");
		Resource adverbPOS = tBox.getResource(LEXINFO + "adverb");
		Resource otherPOS = tBox.getResource(LEXINFO + "otherPartOfSpeech");

		wordEntryType = tBox.getResource(LEMON + "Word");
		phraseEntryType = tBox.getResource(LEMON + "Phrase");

	}
	
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
			if (! (language.equals("fra") || language.equals("eng") || language.equals("deu") || language.equals("por") || language.equals("ita") || language.equals("fin") )) {
				System.err.println("Unknown language: " + language);
				printUsage();
				System.exit(1);
			}
		}

		if (cmd.hasOption(COUNT_LANGUAGE_OPTION)){
			countLanguages = cmd.getOptionValue(COUNT_LANGUAGE_OPTION);
		}
		String clangs[] = countLanguages.split(",");
		int i = 0;
		while(i != clangs.length) {
			counts.put(clangs[i], new IncrementableInt());
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
			m1.read(remainingArgs[0], outputFormat);
		} else {
			System.err.println("unsupported format :" + outputFormat);
			System.exit(1);
		}
	}


	public static void main(String args[]) {
		StatLEMONExtract cliProg = new StatLEMONExtract();
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
		
		System.out.println("Language Edition & syn & ant & hyper & hypo & mero & holo \\\\");
		System.out.print("\\textbf{" + language  + "} & ");
		System.out.print(countRelations(synonymProperty) + "& ");
		// System.out.print(countRelations(nearSynonymProperty) + "& ");
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
			String lang = r.getProperty(targetLanguageProperty).getString();
			langs.add(lang);
			if (counts.containsKey(lang)) {
				counts.get(lang).incr();
			} else {
				others = others + 1;
			}
		}
		relations.close();
		
		int total = 0;
			
		for (Entry<String, IncrementableInt> i : counts.entrySet()) {
			total = total + i.getValue().val;
			System.out.print(" & " + i.getKey());
		}
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
