package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.experiment.evaluation.EvaluationStats;
import org.getalp.dbnary.experiment.preprocessing.StatsModule;
import org.getalp.dbnary.experiment.preprocessing.StructuredGloss;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by serasset on 12/04/17.
 */
public abstract class DBnaryEnhancer {

    protected static final String LANGUAGES_OPTION = "l";
    protected static final String DEFAULT_LANGUAGES = "fra,eng,deu,rus";
    protected static final String RDF_FORMAT_OPTION = "f";
    protected static final String DEFAULT_RDF_FORMAT = "turtle";
    protected static final String STATS_FILE_OPTION = "s";
    protected static final String OUTPUT_FILE_SUFFIX_OPTION = "o";
    protected static final String DEFAULT_OUTPUT_FILE_SUFFIX = "_disambiguated_translations.ttl";
    protected static final String CONFIDENCE_FILE_OPTION = "c";
    protected static final String COMPRESS_OPTION = "z";
    protected static Options options = null; // Command line op
    protected CommandLine cmd = null; // Command Line arguments
    protected String[] languages;
    protected PrintStream statsOutput = null;
    protected StatsModule stats = null;
    protected String rdfFormat;
    protected PrintStream confidenceOutput;
    protected EvaluationStats evaluator = null;
    protected String outputFileSuffix;
    protected boolean doCompress;
    protected HashMap<String,Model> modelMap;

    static {
        options = new Options();
        options.addOption("h", false, "Prints usage and exits. ");
        options.addOption(LANGUAGES_OPTION, true,
                "Language (fra, eng, deu, por). " + DEFAULT_LANGUAGES + " by default.");
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
        System.err.println("Pre-processing translations.");

        for(String lang: languages) {
            this.computeStatsOnGlosses(lang);
        }

        if (null!= statsOutput) {
            System.err.println("Writing Stats");
            stats.displayStats(statsOutput);
            statsOutput.close();
        }

        for(String lang: languages) {
            System.err.println("Disambiguating " + lang);
            Model m = ModelFactory.createDefaultModel();
            m.setNsPrefixes(modelMap.get(lang).getNsPrefixMap());

            this.processTranslations(m, lang);
            System.err.println("Outputting disambiguation links for " + lang);
            this.output(lang, m);
        }

        if(null != confidenceOutput){
            System.err.println("Writing confidence stats");
            evaluator.printConfidenceStats(confidenceOutput);
            confidenceOutput.close();
        }
    }



    private void computeStatsOnGlosses(String lang) {
        // Iterate over all translations
        // TODO: adapt stats module for current language
        if (null != stats) stats.reset(lang);
        Model m = modelMap.get(lang);

        StmtIterator translations = m.listStatements(null, DBnaryOnt.isTranslationOf, (RDFNode) null);
        while (translations.hasNext()) {
            Resource e = translations.next().getSubject();

            Statement g = e.getProperty(DBnaryOnt.gloss);

            if (null == g) {
                if (null != stats) stats.registerTranslation(e.getURI(), null);
            } else {
                StructuredGloss sg = extractGlossStructure(g);
                if (null != stats) stats.registerTranslation(e.getURI(), sg);

                if (null == sg) {
                    // remove gloss from model
                    g.remove();
                }
            }
        }
    }

    protected abstract void printUsage();

    protected void loadArgs(String[] args) {
        CommandLineParser parser = new PosixParser();
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

        languages = cmd.getOptionValue(LANGUAGES_OPTION, DEFAULT_LANGUAGES).split(",");
        for (int i = 0; i < languages.length; i++) {
            ISO639_3.Lang l = ISO639_3.sharedInstance.getLang(languages[i]);
            languages[i] = l.getId();
        }

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

        modelMap = new HashMap<String,Model>();

        for (String arg: remainingArgs) {
            Model m = ModelFactory.createDefaultModel();
            String lang = guessLanguage(arg);
            modelMap.put(lang, m);
            try {
                System.err.println("Reading model: " + arg);
                if (arg.matches("[^:]{2,6}:.*")) {
                    // It's an URL
                    m.read(arg);
                } else {
                    // It's a file
                    if (arg.endsWith(".bz2")) {
                        InputStreamReader modelReader = new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(arg)));
                        m.read(modelReader, null, rdfFormat);
                    } else {
                        InputStreamReader modelReader = new InputStreamReader(new FileInputStream(arg));
                        m.read(modelReader, null, rdfFormat);
                    }
                }

            } catch (FileNotFoundException e) {
                System.err.println("Could not read " + remainingArgs[0]);
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }

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


    private StructuredGloss extractGlossStructure(Statement g) {
        if (null == g) return null;
        RDFNode gloss = g.getObject();
        if (gloss.isLiteral()) return new StructuredGloss(null, gloss.asLiteral().getString());
        if (gloss.isResource()) {
            Resource glossResource = gloss.asResource();
            Statement sn = glossResource.getProperty(DBnaryOnt.senseNumber);
            String senseNumber = null;
            if (sn != null)
                senseNumber = sn.getString();
            Statement glossValue = glossResource.getProperty(RDF.value);
            String glossString = null;
            if (glossValue != null)
                glossString= glossValue.getString();
            return new StructuredGloss(senseNumber, glossString);
        }
        return null;
    }

    protected abstract void processTranslations(Model outputModel, String lang) throws FileNotFoundException;
}
