package org.getalp.dbnary.experiment.jdm;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.cli.*;
import org.getalp.blexisma.api.ISO639_3;

import java.io.*;

/**
 * Created by tchechem on 18/02/14.
 */
public class JDMModelParser {
    private BufferedReader jdmReader;


    private CommandLine cmd = null; // Command Line arguments
    private static Options options = null; // Command line options

    private static final String LANGUAGE_OPTION = "l";
    private static final String DEFAULT_LANGUAGE = "fr";

    private static final String OUTPUT_FORMAT_OPTION = "f";
    private static final String DEFAULT_OUTPUT_FORMAT = "turtle";

    private String outputFormat = DEFAULT_OUTPUT_FORMAT;
    private String language = DEFAULT_LANGUAGE;

    private String langName;

    String[] remainingArgs;

    static{
        options = new Options();
        options.addOption("h", false, "Prints usage and exits. ");
        options.addOption(OUTPUT_FORMAT_OPTION, true,
                "Output format (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). " + DEFAULT_OUTPUT_FORMAT + " by default.");
        options.addOption(LANGUAGE_OPTION, true,
                "Language (fra, eng or deu). " + DEFAULT_LANGUAGE + " by default.");
    }

    private Model m1;

    private void loadArgs(String[] args) throws FileNotFoundException {
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
        ISO639_3.Lang lg = ISO639_3.sharedInstance.getLang(language);
        language = lg.getId();
        langName = lg.getEn();

        remainingArgs = cmd.getArgs();
        if (remainingArgs.length < 1) {
            printUsage();
            System.exit(1);
        }

        //initializeTBox();

        m1 = ModelFactory.createDefaultModel();


        if (	outputFormat.equals("RDF") ||
                outputFormat.equals("TURTLE") ||
                outputFormat.equals("NTRIPLE") ||
                outputFormat.equals("N3") ||
                outputFormat.equals("TTL") ||
                outputFormat.equals("RDFABBREV") ) {
            if ("-".equals(remainingArgs[0])) {
                System.err.println("Reading extract from stdin.");
                jdmReader = new BufferedReader(new InputStreamReader(System.in));
                //m1.read(System.in, outputFormat, "file:///dev/stdin");
            } else {
                System.err.println("Reading extract from " + remainingArgs[0]);
                jdmReader = new BufferedReader(new FileReader(remainingArgs[0]));
            }
        } else {
            System.err.println("unsupported format :" + outputFormat);
            System.exit(1);
        }
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

   private void extractJDM(){
       
   }


    public static void main(String[] args) throws FileNotFoundException {
        JDMModelParser jdmmp = new JDMModelParser();
        jdmmp.loadArgs(args);
        jdmmp.extractJDM();
    }


}
