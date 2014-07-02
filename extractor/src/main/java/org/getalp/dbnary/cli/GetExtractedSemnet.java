package org.getalp.dbnary.cli;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.WiktionaryExtractorFactory;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;

public class GetExtractedSemnet {

	private static Options options = null; // Command line options

	private static final String LANGUAGE_OPTION = "l";
	private static final String DEFAULT_LANGUAGE = "fra";

	private static final String OUTPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_OUTPUT_FORMAT = "ttl";	
	
	private static final String MODEL_OPTION = "m";
	private static final String DEFAULT_MODEL = "lemon";

	private CommandLine cmd = null; // Command Line arguments

	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	private String language = DEFAULT_LANGUAGE;
	private String model = DEFAULT_MODEL;

	private static final String FOREIGN_EXTRACTION_OPTION = "x";

	static {
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(LANGUAGE_OPTION, true, 
				"Language (fr, en,it,pt de, fi or ru). " + DEFAULT_LANGUAGE + " by default.");
		options.addOption(OUTPUT_FORMAT_OPTION, true, 
				"Output format (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). " + DEFAULT_OUTPUT_FORMAT + " by default.");
		options.addOption(MODEL_OPTION, true, 
				"Ontology Model used  (lmf or lemon). Only useful with rdf base formats." + DEFAULT_MODEL + " by default.");
		options.addOption(FOREIGN_EXTRACTION_OPTION, false, "Extract foreign languages");
	}
	
	WiktionaryIndex wi;
	String[] remainingArgs;
	IWiktionaryExtractor we;
	WiktionaryDataHandler wdh;
	
	/**
	 * Validate and set command line arguments.
	 * Exit after printing usage if anything is astray
	 * @param args String[] args as featured in public static void main()
	 * @throws WiktionaryIndexerException 
	 */
	private void loadArgs(String[] args) throws WiktionaryIndexerException {
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

		if (cmd.hasOption(MODEL_OPTION)){
			model = cmd.getOptionValue(MODEL_OPTION);
		}
		model = model.toUpperCase();

		if (cmd.hasOption(LANGUAGE_OPTION)){
			language = cmd.getOptionValue(LANGUAGE_OPTION);
			language = ISO639_3.sharedInstance.getIdCode(language);
		}

		remainingArgs = cmd.getArgs();
		if (remainingArgs.length <= 1) {
			printUsage();
			System.exit(1);
		}

		we = null;
		if (	outputFormat.equals("RDF") || 
				outputFormat.equals("TURTLE") ||
				outputFormat.equals("NTRIPLE") ||
				outputFormat.equals("N3") ||
				outputFormat.equals("TTL") ||
				outputFormat.equals("RDFABBREV") ) {
			if (model.equals("LEMON")) {
				if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)){
					wdh = WiktionaryDataHandlerFactory.getForeignDataHandler(language);
				} else {
					wdh = WiktionaryDataHandlerFactory.getDataHandler(language);
				}
			} else {
				System.err.println("LMF format not supported anymore.");
				System.exit(1);
			}
		} else {
			System.err.println("unsupported format :" + outputFormat);
			System.exit(1);
		}
		
		if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)){
			we = WiktionaryExtractorFactory.getForeignExtractor(language, wdh);
		} else {
			we = WiktionaryExtractorFactory.getExtractor(language, wdh);
		}

		if (null == we) {
			System.err.println("Wiktionary Extraction not yet available for " + ISO639_3.sharedInstance.getLanguageNameInEnglish(language));
			System.exit(1);
		}

		wi = new WiktionaryIndex(remainingArgs[0]);
		we.setWiktionaryIndex(wi);
	}

	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		GetExtractedSemnet cliProg = new GetExtractedSemnet();
		cliProg.loadArgs(args);
		cliProg.extract();
	}


	private void extract() throws IOException {

		for(int i = 1; i < remainingArgs.length; i++) {
			String pageContent = wi.getTextOfPage(remainingArgs[i]);
			we.extractData(remainingArgs[i], pageContent);
		}
		
		if (outputFormat.equals("RDF")) {
        	wdh.dump(System.out);
        } else if (outputFormat.equals("TURTLE")) {
        	wdh.dump(System.out, "TURTLE");
        } else if (outputFormat.equals("NTRIPLE")) {
        	wdh.dump(System.out, "N-TRIPLE");
        } else if (outputFormat.equals("N3")) {
        	wdh.dump(System.out, "N3");
        } else if (outputFormat.equals("TTL")) {
        	wdh.dump(System.out, "TTL");
        } else if (outputFormat.equals("RDFABBREV")) {
        	wdh.dump(System.out, "RDF/XML-ABBREV");
        }
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		String help = 
			"dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index." +
			System.getProperty("line.separator", "\n") +
			"Displays the extracted semnet of the wiktionary page(s) named \"entryname\", ...";
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.cli.GetExtractedSemnet [OPTIONS] dumpFile entryname ...", 
				"With OPTIONS in:", options, 
				help, false);
	}

}
