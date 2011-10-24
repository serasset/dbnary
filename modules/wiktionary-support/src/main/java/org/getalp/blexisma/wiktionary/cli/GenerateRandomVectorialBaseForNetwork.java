package org.getalp.blexisma.wiktionary.cli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.getalp.blexisma.api.ConceptualVectorRandomizer;
import org.getalp.blexisma.api.ConceptualVectorRandomizer.UninitializedRandomizerException;
import org.getalp.blexisma.api.ConceptualVectorRandomizerFactory;
import org.getalp.blexisma.impl.vectorialbase.String_RAM_VectorialBase;
import org.getalp.blexisma.semnet.SimpleSemanticNetwork;
import org.getalp.blexisma.semnet.StringSemNetGraphMLizer;
import org.getalp.blexisma.semnet.TextOnlySemnetReader;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class GenerateRandomVectorialBaseForNetwork {

	private static Options options = null; // Command line options

	private static final String COEFF_VAR_OPTION = "c";
	private static final double DEFAULT_COEFF_VAR = 3;

	private static final String DIMENSION_OPTION = "d";
	private static final int DEFAULT_DIMENSION = 2000;

	private static final String ENCODING_SIZE_OPTION = "s";
	private static final int DEFAULT_ENCODING_SIZE = 32764;

	private static final String INPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_INPUT_FORMAT = "raw";
	
	private static final String ENCODING_OPTION = "e";
	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final String RANDOMIZER_CLASS_OPTION = "r";
	private static final String DEFAULT_RANDOMIZER_CLASS = "org.getalp.blexisma.api.DeviationBasedCVRandomizer";

	
	private CommandLine cmd = null; // Command Line arguments
	
	private String inputFormat = DEFAULT_INPUT_FORMAT;
	private double coeffVar = DEFAULT_COEFF_VAR;
	private String encoding = DEFAULT_ENCODING;
	private String randomizerClass = DEFAULT_RANDOMIZER_CLASS;
	private int dimension, encodingSize;
	
	private InputStreamReader input;

	private String output;

	private ConceptualVectorRandomizer randomizer;
	
	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(INPUT_FORMAT_OPTION, true, "Specifies the input format (raw, graphml). " + DEFAULT_INPUT_FORMAT + " by default.");	
		options.addOption(COEFF_VAR_OPTION, true, "Specifies the variation coefficient used for random vector generation. " + DEFAULT_COEFF_VAR + " by default.");	
		options.addOption(DIMENSION_OPTION, true, "Specifies the dimension of vectors. " + DEFAULT_DIMENSION + " by default.");	
		options.addOption(ENCODING_SIZE_OPTION, true, "Specifies the dimension of vectors. " + DEFAULT_ENCODING_SIZE + " by default.");	
		options.addOption(ENCODING_OPTION, true, 
				"Encoding of input and output. " + DEFAULT_ENCODING + " by default.");
		options.addOption(RANDOMIZER_CLASS_OPTION, true, 
				"fully qualified classname for the randdomizer. " + DEFAULT_RANDOMIZER_CLASS + " by default.");	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 * @throws UninitializedRandomizerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException, UninitializedRandomizerException {
		GenerateRandomVectorialBaseForNetwork cliProg = new GenerateRandomVectorialBaseForNetwork();
		cliProg.loadArgs(args);
		cliProg.generate();
	}
	
	private void generate() throws IOException, UninitializedRandomizerException {
		// TODO: use graphml as an input format for semantic networks.
		//long start = System.currentTimeMillis();
		SimpleSemanticNetwork<String,String> sn = new SimpleSemanticNetwork<String,String>(500000,1000000);
		
		if ("raw".equals(inputFormat)) {
			TextOnlySemnetReader.readFromReader(sn, new BufferedReader(input));
		} else if ("graphml".equals(inputFormat)) {
			System.err.println("graphml format is currently unsupported as an input format");
			System.exit(1);
		} else {
			System.err.println("Unsupported input format: " + inputFormat);
			System.exit(1);
		}
		
		String_RAM_VectorialBase vb = new String_RAM_VectorialBase(encodingSize, dimension);
		
		Iterator<String> it = sn.getNodesIterator();
		
		while(it.hasNext()) {
			String elem = it.next();
			vb.addVector(elem, randomizer.nextVector());
		}
		
		vb.save(output);
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

		if (cmd.hasOption(INPUT_FORMAT_OPTION)){
			inputFormat = cmd.getOptionValue(INPUT_FORMAT_OPTION);
		}

		if (cmd.hasOption(COEFF_VAR_OPTION)){
			coeffVar = Double.parseDouble(cmd.getOptionValue(COEFF_VAR_OPTION, Double.toString(DEFAULT_COEFF_VAR)));
		}

		if (cmd.hasOption(DIMENSION_OPTION)){
			dimension = Integer.parseInt(cmd.getOptionValue(DIMENSION_OPTION, Integer.toString(DEFAULT_DIMENSION)));
		}

		if (cmd.hasOption(ENCODING_SIZE_OPTION)){
			encodingSize = Integer.parseInt(cmd.getOptionValue(ENCODING_SIZE_OPTION, Integer.toString(DEFAULT_ENCODING_SIZE)));
		}

		if (cmd.hasOption(ENCODING_OPTION)){
			encoding = cmd.getOptionValue(ENCODING_OPTION);
		}
		
		if (cmd.hasOption(RANDOMIZER_CLASS_OPTION)){
			randomizerClass = cmd.getOptionValue(RANDOMIZER_CLASS_OPTION);
		}

		String[] remainingArgs = cmd.getArgs();
		if (remainingArgs.length != 2) {
			printUsage();
			System.exit(1);
		}

		randomizer = ConceptualVectorRandomizerFactory.createRandomizer(dimension, encodingSize, randomizerClass);
		randomizer.setOption("coefVar", coeffVar);
		
		try {

			String infn = remainingArgs[0];
			if ("-".equals(infn)) {
				input = new InputStreamReader(System.in, encoding);
			} else {
				input = new InputStreamReader(new FileInputStream(infn), encoding);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.err.println("Unsupported encoding.");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Input File not found.");
			e.printStackTrace();
		}
		
		output = remainingArgs[1];

	}
	
	 public static void printUsage() {
	    	HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -cp /path/to/wiktionary.jar " + GenerateRandomVectorialBaseForNetwork.class.getCanonicalName() + " [OPTIONS] lexical_network_file vectorial_base_file", 
					"With OPTIONS in:", options, 
					"specify - as the inputfile to input semnet from STDIN.", false);
	 }
}
