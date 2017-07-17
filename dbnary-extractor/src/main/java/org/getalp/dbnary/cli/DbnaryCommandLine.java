package org.getalp.dbnary.cli;

import org.apache.commons.cli.*;
import org.getalp.dbnary.*;

import java.io.IOException;

/**
 * An abstract class gathering all common arguments of the main extraction command lines.
 * Created by serasset on 17/07/17.
 *
 */
public abstract class DbnaryCommandLine {
    protected static final String LANGUAGE_OPTION = "l";
    protected static final String DEFAULT_LANGUAGE = "en";

    protected static final String OUTPUT_FORMAT_OPTION = "f";
    protected static final String DEFAULT_OUTPUT_FORMAT = "ttl";

    protected static final String MODEL_OPTION = "m";
    protected static final String DEFAULT_MODEL = "ontolex";

    protected static final String URI_PREFIX_LONG_OPTION = "prefix";
    protected static final String URI_PREFIX_SHORT_OPTION = "p";

    protected static final String FOREIGN_EXTRACTION_OPTION = "x";

    protected static final String MORPHOLOGY_OUTPUT_FILE_LONG_OPTION = "morpho";
    protected static final String MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION = "M";

    protected static final String ETYMOLOGY_OUTPUT_FILE_LONG_OPTION = "etymology";
    protected static final String ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION = "E";

    protected static Options options = null; // Command line options

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
        options.addOption(OptionBuilder.withLongOpt(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION)
                .withDescription("extract morphology data.")
                .hasArg()
                .withArgName("file")
                .create(MORPHOLOGY_OUTPUT_FILE_SHORT_OPTION));
        options.addOption(OptionBuilder.withLongOpt(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION)
                .withDescription("extract etymology data.")
                .hasArg()
                .withArgName("file")
                .create(ETYMOLOGY_OUTPUT_FILE_SHORT_OPTION));
        options.addOption(OptionBuilder.withLongOpt(URI_PREFIX_LONG_OPTION)
                .withDescription("set the URI prefix used in the extracted dataset. Default: " + DbnaryModel.DBNARY_NS_PREFIX)
                .hasArg()
                .withArgName("uri")
                .create(URI_PREFIX_SHORT_OPTION));

    }

    protected String outputFormat = DEFAULT_OUTPUT_FORMAT;
    protected String language = DEFAULT_LANGUAGE;
    protected String model = DEFAULT_MODEL;
    protected String morphoOutputFile = null;
    protected String etymologyOutputFile = null;

    protected CommandLine cmd = null; // Command Line arguments

    WiktionaryIndex wi;
    String[] remainingArgs;
    IWiktionaryExtractor we;
    IWiktionaryDataHandler wdh;


    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String help = getHelpText();
        formatter.printHelp("java -cp /path/to/dbnary.jar " + this.getClass().getCanonicalName() +  " [OPTIONS] dumpFile entryname ...",
                "With OPTIONS in:", options,
                help, false);
    }

    protected abstract String getHelpText();

    /**
     * Validate and set command line arguments.
     * Exit after printing usage if anything is astray
     *
     * @param args String[] args as featured in public static void main()
     * @throws WiktionaryIndexerException
     */
    protected void loadArgs(String[] args) throws WiktionaryIndexerException {
        CommandLineParser parser = new PosixParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
            printUsage();
            System.exit(1);
        }

        // Check for args
        if (cmd.hasOption("h")) {
            printUsage();
            System.exit(0);
        }

        if (cmd.hasOption(OUTPUT_FORMAT_OPTION)) {
            outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
        }
        outputFormat = outputFormat.toUpperCase();

        if (cmd.hasOption(URI_PREFIX_LONG_OPTION)) {
            DbnaryModel.setGlobalDbnaryPrefix(cmd.getOptionValue(URI_PREFIX_SHORT_OPTION));
        }

        if (cmd.hasOption(MODEL_OPTION)) {
            System.err.println("WARN: the " + MODEL_OPTION + " option is now deprecated. Forcibly using model: " + DEFAULT_MODEL);
            // model = cmd.getOptionValue(MODEL_OPTION);
        }
        model = model.toUpperCase();

        if (cmd.hasOption(LANGUAGE_OPTION)) {
            language = cmd.getOptionValue(LANGUAGE_OPTION);
            language = LangTools.getCode(language);
        }

        if (cmd.hasOption(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION)) {
            morphoOutputFile = cmd.getOptionValue(MORPHOLOGY_OUTPUT_FILE_LONG_OPTION);
        }

        if (cmd.hasOption(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION)) {
            etymologyOutputFile = cmd.getOptionValue(ETYMOLOGY_OUTPUT_FILE_LONG_OPTION);
        }


        remainingArgs = cmd.getArgs();
        if (remainingArgs.length <= 1) {
            printUsage();
            System.exit(1);
        }

        we = null;
        if (outputFormat.equals("RDF") ||
                outputFormat.equals("TURTLE") ||
                outputFormat.equals("NTRIPLE") ||
                outputFormat.equals("N3") ||
                outputFormat.equals("TTL") ||
                outputFormat.equals("RDFABBREV")) {
            if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)) {
                wdh = WiktionaryDataHandlerFactory.getForeignDataHandler(language);
            } else {
                wdh = WiktionaryDataHandlerFactory.getDataHandler(language);
            }
            if (morphoOutputFile != null) wdh.enableFeature(IWiktionaryDataHandler.Feature.MORPHOLOGY);
            if (etymologyOutputFile != null) wdh.enableFeature(IWiktionaryDataHandler.Feature.ETYMOLOGY);
        } else {
            System.err.println("unsupported format :" + outputFormat);
            System.exit(1);
        }

        if (cmd.hasOption(FOREIGN_EXTRACTION_OPTION)) {
            we = WiktionaryExtractorFactory.getForeignExtractor(language, wdh);
        } else {
            we = WiktionaryExtractorFactory.getExtractor(language, wdh);
        }

        if (null == we) {
            System.err.println("Wiktionary Extraction not yet available for " + LangTools.inEnglish(language));
            System.exit(1);
        }

        wi = new WiktionaryIndex(remainingArgs[0]);
        we.setWiktionaryIndex(wi);
    }

}
