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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.getalp.blexisma.semnet.SimpleSemanticNetwork;
import org.getalp.blexisma.semnet.StringSemNetGraphMLizer;
import org.getalp.blexisma.semnet.TextOnlySemnetReader;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class ConvertSemnet {

	private static Options options = null; // Command line options

	private static final String OUTPUT_FORMAT_OPTION = "t";
	private static final String DEFAULT_OUTPUT_FORMAT = "graphml";
	
	private static final String INPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_INPUT_FORMAT = "raw";
	
	private static final String ENCODING_OPTION = "e";
	private static final String DEFAULT_ENCODING = "UTF-8";

	
	/**
	 * @uml.property  name="cmd"
	 * @uml.associationEnd  
	 */
	private CommandLine cmd = null; // Command Line arguments
	
	/**
	 * @uml.property  name="inputFormat"
	 */
	private String inputFormat = DEFAULT_INPUT_FORMAT;
	/**
	 * @uml.property  name="outputFormat"
	 */
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	/**
	 * @uml.property  name="encoding"
	 */
	private String encoding = DEFAULT_ENCODING;

	/**
	 * @uml.property  name="input"
	 */
	private InputStreamReader input;

	/**
	 * @uml.property  name="output"
	 */
	private OutputStream output;

	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(INPUT_FORMAT_OPTION, true, "Specifies the input format (raw, graphml). " + DEFAULT_INPUT_FORMAT + " by default.");	
		options.addOption(OUTPUT_FORMAT_OPTION, true, "Specifies the output format (raw, graphml). " + DEFAULT_OUTPUT_FORMAT + " by default.");	
		options.addOption(ENCODING_OPTION, true, 
				"Encoding of input and output. " + DEFAULT_ENCODING + " by default.");
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		ConvertSemnet cliProg = new ConvertSemnet();
		cliProg.loadArgs(args);
		cliProg.convert();
	}
	
	private void convert() throws IOException {
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
		//long end = System.currentTimeMillis();
		//System.err.println("Loaded semnet in: " + (end-start) + "ms.");

		//long tmem = Runtime.getRuntime().totalMemory();
		//long fmem = Runtime.getRuntime().freeMemory();
		
		//System.err.println("Memory usage before gc: " + (tmem-fmem));
		
		//System.gc();
		//tmem = Runtime.getRuntime().totalMemory();
		//fmem = Runtime.getRuntime().freeMemory();
		
		//System.err.println("Memory usage after gc: " + (tmem-fmem));
		if ("graphml".equals(outputFormat)) {
			StringSemNetGraphMLizer os = new StringSemNetGraphMLizer(output, encoding, StringSemNetGraphMLizer.MULLING_OUTPUT);
			os.dump(sn);
		} else if ("raw".equals(outputFormat)) {
			sn.dumpToWriter(new PrintStream(output));
		} else {
			System.err.println("Unsupported output format: " + outputFormat);
			System.exit(1);
		}
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

		if (cmd.hasOption(OUTPUT_FORMAT_OPTION)){
			outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
		}

		if (cmd.hasOption(ENCODING_OPTION)){
			encoding = cmd.getOptionValue(ENCODING_OPTION);
		}

		String[] remainingArgs = cmd.getArgs();
		if (remainingArgs.length != 2) {
			printUsage();
			System.exit(1);
		}

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
		
		try {
			String outfn = remainingArgs[1];
			if ("-".equals(outfn)) {
				output = System.out;
			} else {
				output = new FileOutputStream(outfn);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("OutputFile could not be opened for writing.");
			e.printStackTrace();
		}

	}
	
	 public static void printUsage() {
	    	HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -cp /path/to/wiktionary.jar " + ConvertSemnet.class.getCanonicalName() + " [OPTIONS] inputfile outputfile", 
					"With OPTIONS in:", options, 
					"specify - as the inputfile/outputfile  to input/output semnet from/to STDIN/STDOUT.", false);
	 }
}
