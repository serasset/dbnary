package org.getalp.dbnary.enhancer;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.preprocessing.StatsModule;
import org.getalp.dbnary.enhancer.preprocessing.StructuredGloss;
import org.getalp.iso639.ISO639_3;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by serasset on 12/04/17.
 */
public abstract class DBnaryEnhancer {

    protected static final String RDF_FORMAT_OPTION = "f";
    protected static final String DEFAULT_RDF_FORMAT = "turtle";
    protected static final String STATS_FILE_OPTION = "s";
    protected static final String OUTPUT_FILE_SUFFIX_OPTION = "o";
    protected static final String DEFAULT_OUTPUT_FILE_SUFFIX = "_disambiguated_translations.ttl";
    protected static final String CONFIDENCE_FILE_OPTION = "c";
    protected static final String COMPRESS_OPTION = "z";
    protected static Options options = null; // Command line op
    protected CommandLine cmd = null; // Command Line arguments
    protected Map<String,String> languages = new HashMap();
    // protected String[] languages;
    protected PrintStream statsOutput = null;
    protected StatsModule stats = null;
    protected String rdfFormat;
    protected PrintStream confidenceOutput;
    protected EvaluationStats evaluator = null;
    protected String outputFileSuffix;
    protected boolean doCompress;

    static {
        options = new Options();
        options.addOption("h", false, "Prints usage and exits. ");
        options.addOption(RDF_FORMAT_OPTION, true, "RDF file format (xmlrdf, turtle, n3, etc.). " + DEFAULT_RDF_FORMAT + " by default.");
        options.addOption(STATS_FILE_OPTION, true, "if present generate a csv file of the specified name containing statistics about available glosses in translations.");
        options.addOption(CONFIDENCE_FILE_OPTION, true, "if present generate a csv file of the specified name containing confidence score of the similarity disambiguation.");
        options.addOption(OUTPUT_FILE_SUFFIX_OPTION, true, "if present, use the specified value as the filename suffix for the output "
                + "RDF model containing the computed disambiguated relations for each language." + DEFAULT_OUTPUT_FILE_SUFFIX + " by default.");
        options.addOption(COMPRESS_OPTION, false, "if present, compress the ouput with BZip2.");
    }

    protected void output(String lang, Model m) {
        String outputModelFileName = lang + outputFileSuffix;
        OutputStream outputModelStream;

        try {
            if (doCompress) {
                outputModelFileName = outputModelFileName + ".bz2";
                outputModelStream = new BZip2CompressorOutputStream(new FileOutputStream(outputModelFileName));
            } else {
                outputModelStream = new FileOutputStream(outputModelFileName);
            }

            m.write(outputModelStream, this.rdfFormat);

        } catch (FileNotFoundException e) {
            System.err.println("Could not create output stream: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            return;
        } catch (IOException e) {
            System.err.println("IOException while creating output stream: " + e.getLocalizedMessage());
            e.printStackTrace();
            return;
        }
    }

    protected void doit() throws FileNotFoundException {
        // for(String lang: languages) {
        //    this.computeStatsOnGlosses(lang);
        //}


        for(Map.Entry<String, String> langAndFile: languages.entrySet()) {
            String lang = langAndFile.getKey();
            String modelFile = langAndFile.getValue();
            Model inputModel = ModelFactory.createDefaultModel();

            try {
                System.err.println("Reading model: " + modelFile);

                if (modelFile.matches("[^:]{2,6}:.*")) {
                    // It's an URL
                    inputModel.read(modelFile);
                } else {
                    // It's a file
                    if (modelFile.endsWith(".bz2")) {
                        InputStreamReader modelReader = new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(modelFile)));
                        inputModel.read(modelReader, null, rdfFormat);
                    } else {
                        InputStreamReader modelReader = new InputStreamReader(new FileInputStream(modelFile));
                        inputModel.read(modelReader, null, rdfFormat);
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("Could not read " + modelFile);
                continue;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.err.println("Ignoring " + modelFile);
                continue;
            }


            System.err.println("Disambiguating " + lang);
            Model disambiguatedModel = ModelFactory.createDefaultModel();
            disambiguatedModel.setNsPrefixes(inputModel.getNsPrefixMap());

            this.processTranslations(inputModel, disambiguatedModel, lang);

            System.err.println("Outputting disambiguation links for " + lang);
            this.output(lang, disambiguatedModel);
        }

        if (null != statsOutput) {
            System.err.println("Writing Stats");
            stats.displayStats(statsOutput);
            statsOutput.close();
        }

        if (null != confidenceOutput) {
            System.err.println("Writing confidence stats");
            evaluator.printConfidenceStats(confidenceOutput);
            confidenceOutput.close();
        }
    }

    protected abstract void printUsage();

    protected void loadArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
            printUsage();
            System.exit(1);
        }
        String[] remainingArgs = cmd.getArgs();

        if (remainingArgs.length == 0) {
            System.err.println("Missing model files or URL.");
            printUsage();
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            printUsage();
            System.exit(0);
        }

        doCompress = cmd.hasOption(COMPRESS_OPTION);

        rdfFormat = cmd.getOptionValue(RDF_FORMAT_OPTION, DEFAULT_RDF_FORMAT);
        rdfFormat = rdfFormat.toUpperCase();

        if (cmd.hasOption(STATS_FILE_OPTION)) {
            String statsFile = cmd.getOptionValue(STATS_FILE_OPTION);
            try {
                statsOutput = new PrintStream(statsFile, "UTF-8");
            } catch (FileNotFoundException e) {
                System.err.println("Cannot output statistics to file " + statsFile);
                System.exit(1);
            } catch (UnsupportedEncodingException e) {
                // Should never happen
                e.printStackTrace();
                System.exit(1);
            }
            stats = new StatsModule();
        }

        if (cmd.hasOption(CONFIDENCE_FILE_OPTION)) {
            String confidenceFile = cmd.getOptionValue(CONFIDENCE_FILE_OPTION);
            try {
                confidenceOutput = new PrintStream(confidenceFile, "UTF-8");
            } catch (FileNotFoundException e) {
                System.err.println("Cannot output statistics to file " + confidenceFile);
                System.exit(1);
            } catch (UnsupportedEncodingException e) {
                // Should never happen
                e.printStackTrace();
                System.exit(1);
            }
            evaluator = new EvaluationStats();
        }

        outputFileSuffix = cmd.getOptionValue(OUTPUT_FILE_SUFFIX_OPTION, DEFAULT_OUTPUT_FILE_SUFFIX);

        for (String arg: remainingArgs) {
            String lang = guessLanguage(arg);
            ISO639_3.Lang l = ISO639_3.sharedInstance.getLang(lang);
            languages.put(l.getId(), arg);

        }

    }

    protected String guessLanguage(String arg) {
        if (arg.matches("[^:]{2,6}:.*")) {
            // It's an URL
            try {
                String fname = new File (new URL(arg).getPath()).getName();
                return ISO639_3.sharedInstance.getIdCode(fname.split("_")[0]);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            // It's a file
            String fname = new File(arg).getName();
            return ISO639_3.sharedInstance.getIdCode(fname.split("_")[0]);
        }
        return null;
    }


    protected abstract void processTranslations(Model inputModel, Model outputModel, String lang) throws FileNotFoundException;
}
