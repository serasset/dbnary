package org.getalp.blexisma.wiktionary.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.getalp.blexisma.wiktionary.EnglishWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.FrenchWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.GermanWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.SimpleSemanticNetwork;
import org.getalp.blexisma.wiktionary.StringSemNetGraphMLizer;
import org.getalp.blexisma.wiktionary.WiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryIndex;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class ExtractWiktionary {

	private static Options options = null; // Command line options

	private static final String LANGUAGE_OPTION = "l";
	private static final String DEFAULT_LANGUAGE = "fr";

	private static final String OUTPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_OUTPUT_FORMAT = "raw";
	
	private static final String OUTPUT_FILE_OPTION = "o";
	private static final String DEFAULT_OUTPUT_FILE = "fr_extract";
	
	private static final String SUFFIX_OUTPUT_FILE_OPTION = "s";
	
	
	private CommandLine cmd = null; // Command Line arguments
	
	private String outputFile = DEFAULT_OUTPUT_FILE;
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	private String language = DEFAULT_LANGUAGE;
	private String dumpFile;
	private String outputFileSuffix = "";
	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(SUFFIX_OUTPUT_FILE_OPTION, false, "Add a unique suffix to output file. ");	
		options.addOption(LANGUAGE_OPTION, true, 
				"Language (fr, en or de). " + DEFAULT_LANGUAGE + " by default.");
		options.addOption(OUTPUT_FORMAT_OPTION, true, 
				"Output format (graphml or raw). " + DEFAULT_OUTPUT_FORMAT + " by default.");
		options.addOption(OUTPUT_FILE_OPTION, true, "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");	
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		ExtractWiktionary cliProg = new ExtractWiktionary();
		cliProg.loadArgs(args);
		cliProg.extract();
	}
	
	/**
	 * Validate and set command line arguments.
	 * Exit after printing usage if anything is astray
	 * @param args String[] args as featured in public static void main()
	 */
	private void loadArgs(String[] args){
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
		
		if (cmd.hasOption(SUFFIX_OUTPUT_FILE_OPTION)){
			SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			outputFileSuffix = df.format(new Date());
		}
		
		if (cmd.hasOption(OUTPUT_FORMAT_OPTION)){
			outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
		}
				
		if (cmd.hasOption(OUTPUT_FILE_OPTION)){
			outputFile = cmd.getOptionValue(OUTPUT_FILE_OPTION);
		}
		
		if (cmd.hasOption(LANGUAGE_OPTION)){
			language = cmd.getOptionValue(LANGUAGE_OPTION);
			if (! (language.equals("fr") || language.equals("en") || language.equals("de"))) {
				printUsage();
				System.exit(1);
			}
		}
		
		String[] remainingArgs = cmd.getArgs();
		if (remainingArgs.length != 1) {
			printUsage();
			System.exit(1);
		}
		
		outputFile = outputFile + outputFileSuffix;
		 
		dumpFile = remainingArgs[0];
	}
	
    public void extract() throws WiktionaryIndexerException, IOException {
        
        long startTime = System.currentTimeMillis();

        WiktionaryIndex wi = new WiktionaryIndex(dumpFile);
        
        WiktionaryExtractor we = null;
        if (language.equals("fr")) {
            we = new FrenchWiktionaryExtractor(wi);
        } else if (language.equals("en")) {
            we = new EnglishWiktionaryExtractor(wi);
        } else if (language.equals("de")) {
            we = new GermanWiktionaryExtractor(wi);
        } else {
            printUsage();
            System.exit(1);
        }

        long endloadTime = System.currentTimeMillis();
        System.err.println("Loaded index in " + (endloadTime - startTime) +"ms.");
         
        SimpleSemanticNetwork<String, String> s = new SimpleSemanticNetwork<String, String>(100000, 1000000);
        startTime = System.currentTimeMillis();
        long totalRelevantTime = 0, relevantstartTime = 0, relevantTimeOfLastThousands;
        int nbpages = 0, nbrelevantPages = 0;
        relevantTimeOfLastThousands = System.currentTimeMillis();
        for (String page : wi.keySet()) {
            // System.out.println("Extracting: " + page);
            int nbnodes = s.getNbNodes();
            relevantstartTime = System.currentTimeMillis();
            we.extractData(page, s); 
            nbpages ++;
            if (nbnodes != s.getNbNodes()) {
                totalRelevantTime += (System.currentTimeMillis() - relevantstartTime);
                nbrelevantPages++;
                if (nbrelevantPages % 1000 == 0) {
                    System.out.println("Extracted: " + nbrelevantPages + " pages in: " + totalRelevantTime + " / Average = " 
                            + (totalRelevantTime/nbrelevantPages) + " ms/extracted page (" + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000 + " ms) (" + nbpages 
                            + " processed Pages in " + (System.currentTimeMillis() - startTime) + " ms / Average = " + (System.currentTimeMillis() - startTime) / nbpages + ")" );
                    System.out.println("      NbNodes = " + s.getNbNodes());
                    relevantTimeOfLastThousands = System.currentTimeMillis();
                }
                // if (nbrelevantPages == 1100) break;
            }
        }
        
        if (outputFormat.equals("graphml")) {
        	StringSemNetGraphMLizer gout = new StringSemNetGraphMLizer(new OutputStreamWriter(new FileOutputStream(outputFile)));
        	gout.dump(s);
        } else {  
        	s.dumpToWriter(new PrintStream(outputFile));
        }
        System.err.println(nbpages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
        System.err.println("Semnet contains: " + s.getNbNodes() + " nodes and " + s.getNbEdges() + " edges.");
    }

    
    public static void printUsage() {
    	HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -cp /path/to/wiktionary.jar [OPTIONS] dumpFile", 
				"With OPTIONS in:", options, 
				"dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index.", true);
    }

}
