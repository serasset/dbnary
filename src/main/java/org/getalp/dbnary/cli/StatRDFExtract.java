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
import org.getalp.dbnary.LMFBasedRDFDataHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class StatRDFExtract {

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

	/**
	 * @uml.property  name="cmd"
	 * @uml.associationEnd  
	 */
	private CommandLine cmd = null; // Command Line arguments

	/**
	 * @uml.property  name="outputFormat"
	 */
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	/**
	 * @uml.property  name="language"
	 */
	private String language = DEFAULT_LANGUAGE;
	/**
	 * @uml.property  name="countLanguages"
	 */
	private String countLanguages = DEFAULT_COUNT_LANGUAGE;
	/**
	 * @uml.property  name="counts"
	 * @uml.associationEnd  inverse="this$0:org.getalp.dbnary.cli.StatRDFExtract$IncrementableInt" qualifier="lang:java.lang.String org.getalp.dbnary.cli.StatRDFExtract$IncrementableInt"
	 */
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

	/**
	 * @uml.property  name="remainingArgs" multiplicity="(0 -1)" dimension="1"
	 */
	String[] remainingArgs;

	/**
	 * @uml.property  name="m1"
	 * @uml.associationEnd  
	 */
	Model m1;
	
	protected static final String NSprefix = "http://getalp.org/dbnary/";
	protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	
	/**
	 * @uml.property  name="lexEntryType"
	 * @uml.associationEnd  
	 */
	protected Resource lexEntryType;
	/**
	 * @uml.property  name="lemmaType"
	 * @uml.associationEnd  
	 */
	protected Resource lemmaType;
	/**
	 * @uml.property  name="translationType"
	 * @uml.associationEnd  
	 */
	protected Resource translationType;
	/**
	 * @uml.property  name="senseType"
	 * @uml.associationEnd  
	 */
	protected Resource senseType;
	/**
	 * @uml.property  name="definitionType"
	 * @uml.associationEnd  
	 */
	protected Resource definitionType;
	/**
	 * @uml.property  name="lexicalEntryRelationType"
	 * @uml.associationEnd  
	 */
	protected Resource lexicalEntryRelationType;

	/**
	 * @uml.property  name="posProperty"
	 * @uml.associationEnd  
	 */
	protected Property posProperty;
	/**
	 * @uml.property  name="formProperty"
	 * @uml.associationEnd  
	 */
	protected Property formProperty;
	/**
	 * @uml.property  name="isPartOf"
	 * @uml.associationEnd  
	 */
	protected Property isPartOf;
	/**
	 * @uml.property  name="langProperty"
	 * @uml.associationEnd  
	 */
	protected Property langProperty;
	/**
	 * @uml.property  name="equivalentTargetProperty"
	 * @uml.associationEnd  
	 */
	protected Property equivalentTargetProperty;
	/**
	 * @uml.property  name="gloseProperty"
	 * @uml.associationEnd  
	 */
	protected Property gloseProperty;
	/**
	 * @uml.property  name="usageProperty"
	 * @uml.associationEnd  
	 */
	protected Property usageProperty;
	/**
	 * @uml.property  name="textProperty"
	 * @uml.associationEnd  
	 */
	protected Property textProperty;
	/**
	 * @uml.property  name="senseNumberProperty"
	 * @uml.associationEnd  
	 */
	protected Property senseNumberProperty;
	/**
	 * @uml.property  name="entryRelationTargetProperty"
	 * @uml.associationEnd  
	 */
	protected Property entryRelationTargetProperty;
	/**
	 * @uml.property  name="entryRelationLabelProperty"
	 * @uml.associationEnd  
	 */
	protected Property entryRelationLabelProperty;

	/**
	 * @uml.property  name="tBox"
	 * @uml.associationEnd  
	 */
	Model tBox;
	/**
	 * @uml.property  name="nS"
	 */
	String NS;
	
	private void initializeTBox(String lang) {
		NS = NSprefix + lang + "#";


		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		InputStream fis = LMFBasedRDFDataHandler.class.getResourceAsStream("LMF-rdf-rev14.xml");
		tBox.read( fis, LMF );

		lexEntryType = tBox.getResource(LMF + "LexicalEntry");
		lemmaType = tBox.getResource(LMF + "Lemma");

		translationType = tBox.getResource(LMF + "Equivalent");
		senseType = tBox.getResource(LMF + "Sense");
		definitionType = tBox.getResource(LMF + "Definition");
		lexicalEntryRelationType = tBox.getResource(NS + "LexicalEntryRelation");

		posProperty = tBox.getProperty(NS + "partOfSpeech");
		formProperty = tBox.getProperty(NS + "writtenForm");
		langProperty = tBox.getProperty(NS + "language");
		equivalentTargetProperty = formProperty;
		gloseProperty = tBox.getProperty(NS + "glose");
		usageProperty = tBox.getProperty(NS + "usage");
		textProperty = tBox.getProperty(NS + "text");
		senseNumberProperty = tBox.getProperty(NS + "senseNumber");
		entryRelationLabelProperty = tBox.getProperty(NS + "label");
		entryRelationTargetProperty = tBox.getProperty(NS + "target");

		isPartOf = tBox.getProperty(LMF + "isPartOf");
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
		StatRDFExtract cliProg = new StatRDFExtract();
		cliProg.loadArgs(args);
		cliProg.stats();
		
	}

	private void stats() {
		
		System.out.println("Stats on RDF file: " + remainingArgs[0]);
		
		// Number of Lexical Entries

		int nble = countResourcesOfType(lexEntryType);
		
		// Number of Lemmas
		int nblemmas = countResourcesOfType(lemmaType);
		
		int nbEquiv = countResourcesOfType(translationType);
		int nbrel = countResourcesOfType(lexicalEntryRelationType);
		int nbdef = countResourcesOfType(definitionType);
		int nbsense = countResourcesOfType(senseType);
		
		System.out.println(nble + " lexical entries.");
		System.out.println(nblemmas + " lemmas.");
		System.out.println(nbEquiv + " equivalents.");
		System.out.println(nbrel + " relations.");
		System.out.println(nbdef + " definitions.");
		System.out.println(nbsense + " senses.");
		System.out.println((nble + nblemmas + nbEquiv + nbrel + nbdef + nbsense) + " total nodes.");
		
		printRelationsStats();
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

	private void printRelationsStats() {
		// Number of relations
		SortedMap<String, IncrementableInt> rels = new TreeMap<String, IncrementableInt>();
		ResIterator relations = m1.listResourcesWithProperty(RDF.type, lexicalEntryRelationType);
		while(relations.hasNext()) {
			Resource r = relations.next();
			String label = r.getProperty(entryRelationLabelProperty).getString();
			if (rels.containsKey(label))
				rels.get(label).incr();
			else 
				rels.put(label, new IncrementableInt());
		}
		relations.close();
		for (Entry<String, IncrementableInt> i : rels.entrySet()) {
			System.out.println(i.getKey() + ": " + i.getValue());
		}
		
	}

	private void printEquivalentsStats() {
		// Number of relations
		ResIterator relations = m1.listResourcesWithProperty(RDF.type, translationType);
		HashSet<String> langs = new HashSet<String>();
		int others = 0;
		while(relations.hasNext()) {
			Resource r = relations.next();
			String lang = r.getProperty(langProperty).getString();
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
