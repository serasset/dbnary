package org.getalp.dbnary.experiment.preprocessing;

import com.hp.hpl.jena.rdf.model.*;
import org.apache.commons.cli.*;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.DbnaryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateGoldStandard {
	
	private CommandLine cmd = null; // Command Line arguments
	private static Options options = null; // Command line options
	
	private static final String LANGUAGE_OPTION = "l";
	private static final String DEFAULT_LANGUAGE = "fr";

	private static final String OUTPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_OUTPUT_FORMAT = "turtle";	

	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(OUTPUT_FORMAT_OPTION, true, 
				"Output format (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). " + DEFAULT_OUTPUT_FORMAT + " by default.");
		options.addOption(LANGUAGE_OPTION, true, 
				"Language (fra, eng or deu). " + DEFAULT_LANGUAGE + " by default.");
	}	

	String[] remainingArgs;
	Model m1;
	
	String NS;
	
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	private String language = DEFAULT_LANGUAGE;

	private String langName;
	private Property senseNumProperty;
	private Property transNumProperty;

	private void initializeTBox(String lang) {
		NS = DbnaryModel.DBNARY_NS_PREFIX + "/" + lang + "/";
		senseNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationSenseNumber");
		transNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationNumber");
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

		language = cmd.getOptionValue(LANGUAGE_OPTION, DEFAULT_LANGUAGE);
		Lang lg = ISO639_3.sharedInstance.getLang(language);
		language = lg.getId();
		langName = lg.getEn();

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
				m1.read(System.in, outputFormat, "file:///dev/stdin");
			} else {
				System.err.println("Reading extract from " + remainingArgs[0]);
				m1.read(remainingArgs[0], outputFormat);
			}
		} else {
			System.err.println("unsupported format :" + outputFormat);
			System.exit(1);
		}
	}

	public static void main(String args[]) {
		CreateGoldStandard cliProg = new CreateGoldStandard();
		cliProg.loadArgs(args);
		
		cliProg.processTranslations();
		
		cliProg.displayResults();
	}
	
	
	private void displayResults() {

	}

	private void processTranslations() {
		// Iterate over all translations

        List<String> gsEntries = new ArrayList<>();

        StmtIterator translations = m1.listStatements((Resource) null, DbnaryModel.isTranslationOf, (RDFNode) null);
		
		while (translations.hasNext()) {
            Statement next = translations.next();

            Resource e = next.getSubject();

            Statement n = e.getProperty(transNumProperty);
			Statement s = e.getProperty(senseNumProperty);


            if (null != s) {

                Resource lexicalEntry = next.getObject().asResource();
                List<String> senseIds = new ArrayList<>();
                StmtIterator senses = m1.listStatements(lexicalEntry, DbnaryModel.lemonSenseProperty, (RDFNode) null);
                while (senses.hasNext()) {
                    Statement nextSense = senses.next();
                    String sstr = nextSense.getObject().toString();
                    senseIds.add(sstr);
                }

                String sn = s.getString();
				List<Integer> nums = parseSenseNumbers(sn);
                int rank = 1;
                for (int num : nums) {
                    if (num < senseIds.size()) {
                        System.out.println(n.getObject().toString().split("\\^\\^")[0] + " 0 " + senseIds.get(num) + " " + rank);
                        rank++;
                    }
                }

            }
		}
    }


    Pattern onlyDigitsAndComma = Pattern.compile("[\\d,]*");
	Matcher matchDigitAndComma = onlyDigitsAndComma.matcher("");

    private List<Integer> parseSenseNumbers(String sn) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		sn = sn.trim();
		if (sn.length() == 0) return res;
		String senses = sn.replaceAll(" ","");
        senses = senses.replaceAll("(\\d)\\|.*", "$1");
        senses = senses.replaceAll("/", ",");
        senses = senses.replaceAll("(\\(|\\))", "");
        senses = senses.replaceAll("et", ",");
        senses = senses.replaceAll("(\\d)([a-c])", "$1");
        senses = senses.replaceAll("(sens)(\\d)", "$2");


        matchDigitAndComma.reset(senses);
		if (matchDigitAndComma.matches()) {
            String[] senseNumbers = senses.split(",");
            for (int i = 0; i < senseNumbers.length; i++) {
                if (senseNumbers[i].length() > 0) {
                    res.add(Integer.valueOf(senseNumbers[i]));
                }
            }
		} else {
			System.err.println("Unsupported format: " + sn);
		}
		return res;
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		String help = 
			"url must point on an RDF model file extracted from wiktionary and cleaned up (with sense numbers and translation numbers." +
			System.getProperty("line.separator", "\n") +
			"Displays stats on the LMF based RDF dump.";
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.cli.StatRDFExtract [OPTIONS] url", 
				"With OPTIONS in:", options, 
				help, false);
	}

}
