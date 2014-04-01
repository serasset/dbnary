package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wcohen.ss.ScaledLevenstein;
import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.DisambiguableSense;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationAmbiguity;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;
import org.getalp.dbnary.experiment.evaluation.EvaluationStats;
import org.getalp.dbnary.experiment.preprocessing.AbstractGlossFilter;
import org.getalp.dbnary.experiment.preprocessing.StatsModule;
import org.getalp.dbnary.experiment.preprocessing.StructuredGloss;
import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.string.TverskiIndex;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class DisambiguateAllTranslationSources {

    private static final String LANGUAGE_OPTION = "l";
    private static final String DEFAULT_LANGUAGE = "fr";
    private static final String RDF_FORMAT_OPTION = "f";
    private static final String DEFAULT_RDF_FORMAT = "turtle";
    private static final String STATS_FILE_OPTION = "s";
    private static final String OUTPUT_FILE_OPTION = "o";

    private static Options options = null; // Command line op

    static {
        options = new Options();
        options.addOption("h", false, "Prints usage and exits. ");
        options.addOption(LANGUAGE_OPTION, true,
                "Language (fra, eng, deu, por). " + DEFAULT_LANGUAGE + " by default.");
        options.addOption(RDF_FORMAT_OPTION, true, "RDF file format (xmlrdf, turtle, n3, etc.). " + DEFAULT_RDF_FORMAT + " by default.");
        options.addOption(STATS_FILE_OPTION, true, "if present generate a csv file of the specified name containing statistics about available glosses in translations.");
        options.addOption(OUTPUT_FILE_OPTION, true, "if present, use the specified value as the filename for the output RDF models containing the computed disambiguated relations.");
    }

    private static Map<String, Model> models;
    private static Model outputModel;
    private Map<String, String> langNS = new HashMap<>();
    private CommandLine cmd = null; // Command Line arguments
    private Property senseNumProperty;

    {
        senseNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationSenseNumber");
    }

    private Disambiguator disambiguator;
    private double deltaThreshold;
    // private Locale language;
    private String lang;

    private Map<String,AbstractGlossFilter> filter = new HashMap<>();
    private PrintStream statsOutput = null;
    private Map<String, StatsModule> stats = null;
    private EvaluationStats evalStats = new EvaluationStats();
    private OutputStream outputModelStream;
    private String rdfFormat;
    private SimilarityMeasure similarityMeasure;


    private DisambiguateAllTranslationSources() {

        disambiguator = new TranslationDisambiguator();
        double w1 = 0.1;
        double w2 = 1d - w1;
        String mstr = String.format("_%f_%f", w1, w2);

        similarityMeasure = new TverskiIndex(w1, w2, true, false, new ScaledLevenstein());
        disambiguator.registerSimilarity("FTiLs" + mstr, similarityMeasure);
    }

    public static void main(String[] args) throws IOException {

        DisambiguateAllTranslationSources lld = new DisambiguateAllTranslationSources();
        lld.loadArgs(args);

        lld.preprocessAndDisambiguateGlossedTraslationSources();
    }

    public static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String help =
                "urlOrFile must point on an RDF models file extracted from wiktionary by DBnary.";
        formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources [OPTIONS] urlOrFile",
                "With OPTIONS in:", options,
                help, false);
    }

    private void preprocessAndDisambiguateGlossedTraslationSources() throws FileNotFoundException {
        for (String clang : models.keySet()) {
            System.err.println("Pre-processing translations.");
            this.preprocessTranslations(models.get(clang),clang);

            if (statsOutput != null) {
                stats.get(clang).displayStats(statsOutput);
                statsOutput.close();
            }
            double deltaT = 0.05;
            this.setDeltaThreshold(deltaT);
            System.err.println("Processing translations.");
            this.processGlossedTranslations(models.get(clang), clang);
        }
        outputModel.write(outputModelStream, rdfFormat);

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
        String[] remainingArgs = cmd.getArgs();

        if (remainingArgs.length == 0) {
            System.err.println("Missing models file or URL.");
            printUsage();
            System.exit(1);
        }
        if (cmd.hasOption("h")) {
            printUsage();
            System.exit(0);
        }

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
        }

        try {
            if (cmd.hasOption(OUTPUT_FILE_OPTION)) {
                String outputModelFileName = cmd.getOptionValue(OUTPUT_FILE_OPTION);
                if ("-".equals(outputModelFileName)) {
                    outputModelStream = System.out;
                } else {
                    outputModelStream = new FileOutputStream(outputModelFileName);
                }
            } else {
                outputModelStream = new FileOutputStream(lang + "_disambiguated_translations.ttl");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not create output stream: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        lang = cmd.getOptionValue(LANGUAGE_OPTION, DEFAULT_LANGUAGE);
        Lang l = ISO639_3.sharedInstance.getLang(lang);
        lang = (l.getPart1() != null) ? l.getPart1() : l.getId();
        stats = new HashMap<>();

        models = new HashMap<>();

        try {
            File dataDirectory = new File(remainingArgs[0]);

            if (dataDirectory.isDirectory()) {
                for (File f : dataDirectory.listFiles()) {
                    // It's a file
                    String fname = f.getName();
                    String lang = fname.split("_")[0];
                    Model m = ModelFactory.createDefaultModel();
                    if (fname.endsWith(".bz2")) {
                        InputStreamReader modelReader = new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(f)));
                        m.read(modelReader, null, rdfFormat);
                    } else {
                        InputStreamReader modelReader = new InputStreamReader(new FileInputStream(f));
                        m.read(modelReader, null, rdfFormat);
                    }

                    Lang lf = ISO639_3.sharedInstance.getLang(lang);
                    lang = (lf.getPart1() != null) ? lf.getPart1() : lf.getId();
                    String lang3 = lf.getId();
                    initializeTBox(lang);

                    stats.put(lang3,new StatsModule(lf.getEn()));
                    filter.put(lang3, createGlossFilter(lang3));
                    models.put(lang3, m);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not read " + remainingArgs[0]);
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }


        outputModel = ModelFactory.createDefaultModel();
        //outputModel.setNsPrefixes(models.getNsPrefixMap());

    }

    private void initializeTBox(String lang) {
        langNS.put(lang, DbnaryModel.DBNARY_NS_PREFIX + "/" + lang + "/");
    }

    private AbstractGlossFilter createGlossFilter(String lang) {
        AbstractGlossFilter f = null;
        String cname = AbstractGlossFilter.class.getCanonicalName();
        int dpos = cname.lastIndexOf('.');
        String pack = cname.substring(0, dpos);
        Class<?> wec = null;
        try {
            wec = Class.forName(pack + "." + lang + ".GlossFilter");
            f = (AbstractGlossFilter) wec.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("No gloss filter found for " + lang + " reverting to default " + pack + ".DefaultGlossFilter");
            try {
                wec = Class.forName(pack + ".DefaultGlossFilter");
                f = (AbstractGlossFilter) wec.getConstructor().newInstance();
            } catch (ClassNotFoundException e1) {
                System.err.println("Default gloss filter not found");
            } catch (InvocationTargetException e1) {
                System.err.println("Default gloss filter failed to be instanciated");
            } catch (NoSuchMethodException e1) {
                System.err.println("Default gloss filter failed to be instanciated");
            } catch (InstantiationException e1) {
                System.err.println("Default gloss filter failed to be instanciated");
            } catch (IllegalAccessException e1) {
                System.err.println("Default gloss filter failed to be instanciated");
            }
        } catch (InstantiationException e) {
            System.err.println("Could not instanciate wiktionary extractor for " + lang);
        } catch (IllegalAccessException e) {
            System.err.println("Illegal access to wiktionary extractor for " + lang);
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument passed to wiktionary extractor's constructor for " + lang);
            e.printStackTrace(System.err);
        } catch (SecurityException e) {
            System.err.println("Security exception while instanciating wiktionary extractor for " + lang);
            e.printStackTrace(System.err);
        } catch (InvocationTargetException e) {
            System.err.println("InvocationTargetException exception while instanciating wiktionary extractor for " + lang);
            e.printStackTrace(System.err);
        } catch (NoSuchMethodException e) {
            System.err.println("No appropriate constructor when instanciating wiktionary extractor for " + lang);
        }
        return f;
    }

    private void preprocessTranslations(Model m, String language) {
        // Iterate over all translations

        StmtIterator translations = m.listStatements((Resource) null, DbnaryModel.isTranslationOf, (RDFNode) null);

        while (translations.hasNext()) {
            Resource e = translations.next().getSubject();

            Statement g = e.getProperty(DbnaryModel.glossProperty);

            if (null == g) {
                stats.get(language).registerTranslation(e.getURI(), null);
            } else {
                StructuredGloss sg = filter.get(language).extractGlossStructure(g.getString());
                stats.get(language).registerTranslation(e.getURI(), sg);

                if (null == sg) {
                    // remove gloss from models
                    g.remove();
                } else {
                    if (null != sg.getSenseNumber()) {
                        g.getModel().add(g.getModel().createLiteralStatement(g.getSubject(), senseNumProperty, sg.getSenseNumber()));
                    }
                    if (null == sg.getGloss()) {
                        // remove gloss from models
                        g.remove();
                    } else {
                        g.changeObject(sg.getGloss());
                    }
                }
            }

        }
    }

    private void processGlossedTranslations(Model m1, String lang) throws FileNotFoundException {

        StmtIterator translations = m1.listStatements(null, DbnaryModel.isTranslationOf, (RDFNode) null);

        while (translations.hasNext()) {
            Statement next = translations.next();

            Resource e = next.getSubject();

            Statement s = e.getProperty(senseNumProperty);
            Statement g = e.getProperty(DbnaryModel.glossProperty);

            boolean connected = false;
            if (null != s) {
                // Process sense number
                // System.out.println("Avoiding treating " + s.toString());
                connected = connectNumberedSenses(s, outputModel);
            }

            Resource lexicalEntry = next.getObject().asResource();

            if (!connected && null != g) {
                String uri = g.getSubject().toString();
                String gloss = g.getObject().toString();
                Ambiguity ambiguity = new TranslationAmbiguity(gloss, e.getLocalName(), deltaThreshold);
                // Compute set of sense + definitions to be chosen among

                StmtIterator senses = m1.listStatements(lexicalEntry, DbnaryModel.lemonSenseProperty, (RDFNode) null);
                List<Disambiguable> choices = new ArrayList<>();
                while (senses.hasNext()) {
                    Statement nextSense = senses.next();
                    String sstr = nextSense.getObject().toString();
                    sstr = sstr.substring(sstr.indexOf("__ws_"));
                    Statement dRef = nextSense.getProperty(DbnaryModel.lemonDefinitionProperty);
                    Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
                    String deftext = dVal.getObject().toString();
                    choices.add(new DisambiguableSense(deftext, sstr));
                }
                disambiguator.disambiguate(ambiguity, choices);

                Resource sense = m1.createResource(uri);
                for (Disambiguable d : ambiguity.getBestSolutions("FTiLs")) {
                    m1.add(m1.createStatement(sense, DbnaryModel.isTranslationOf, m1.createResource(langNS.get(lang) + d.getId())));
                }
            } else {
                //TODO: Call new code in prod
                List<String> transClo = computeTranslationClosure(e,3);
            }
        }
    }

    private String getTranslationLanguage(String uri){
        String lang = "";
        Pattern lp = Pattern.compile(".*__tr_(...)_[0-9].*");
        Matcher lm = lp.matcher(uri);
        if(lm.find()){
            lang = lm.group(1);
        }
        return lang;
    }

    private List<String> computeTranslationClosure(Resource translation, int degree){
        return computeTranslationClosure(translation,degree,this.lang);
    }

    private List<String> computeTranslationClosure(Resource translation, int degree, String topLevelLang){
        String currentLang = getTranslationLanguage(translation.getURI());
        List<String> output = new ArrayList<>();
        if(degree!=0 && models.containsKey(currentLang)){
            String writtenForm = translation.getProperty(DbnaryModel.equivalentTargetProperty).getObject().toString();
            String uri = DbnaryModel.uriEncode(writtenForm);
            Resource r = models.get(currentLang).getResource(uri);
            //Find translations pointing back to top level lang
            StmtIterator trans = models.get(currentLang).listStatements(r, DbnaryModel.isTranslationOf, (RDFNode) null);
            while (trans.hasNext()) {
                Statement ctransstmt = trans.next();
                Resource ctrans = ctransstmt.getSubject();
                Statement l = ctrans.getProperty(DbnaryModel.targetLanguageCodeProperty);
            }

        }
        return output;
    }

    private boolean connectNumberedSenses(Statement s, Model outModel) {
        boolean connected = false;
        Resource translation = s.getSubject();
        Resource lexEntry = translation.getPropertyResourceValue(DbnaryModel.isTranslationOf);
        String nums = s.getString();

        if (lexEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType)) {
            ArrayList<String> ns = getSenseNumbers(nums);
            for (String n : ns) {
                connected = connected || attachTranslationToNumberedSense(translation, lexEntry, n, outModel);
            }
        }
        return connected;
    }

    private boolean attachTranslationToNumberedSense(Resource translation, Resource lexEntry, String n,
                                                     Model outModel) {
        boolean connected = false;
        StmtIterator senses = lexEntry.listProperties(DbnaryModel.lemonSenseProperty);
        while (senses.hasNext()) {
            Resource sense = senses.next().getResource();
            Statement senseNumStatement = sense.getProperty(DbnaryModel.senseNumberProperty);
            if (n.equalsIgnoreCase(senseNumStatement.getString())) {
                connected = true;
                outModel.add(outModel.createStatement(translation, DbnaryModel.isTranslationOf, sense));
            }
        }
        return connected;
    }

    public ArrayList<String> getSenseNumbers(String nums) {
        ArrayList<String> ns = new ArrayList<String>();

        if (nums.contains(",")) {
            String[] ni = nums.split(",");
            for (int i = 0; i < ni.length; i++) {
                ns.addAll(getSenseNumbers(ni[i]));
            }
        } else if (nums.contains("-") || nums.contains("—") || nums.contains("–")) {
            String[] ni = nums.split("[-—–]");
            if (ni.length != 2) {
                System.err.append("Strange split on dash: " + nums);
            } else {
                try {
                    int s = Integer.parseInt(ni[0].trim());
                    int e = Integer.parseInt(ni[1].trim());

                    if (e <= s) {
                        System.err.println("end of range is lower than beginning in: " + nums);
                    } else {
                        for (int i = s; i <= e; i++) {
                            ns.add(Integer.toString(i));
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }
        } else {
            try {
                ns.add(nums.trim());
            } catch (NumberFormatException e) {
                System.err.println(e.getLocalizedMessage() + ": " + nums);
            }
        }
        return ns;
    }

    public void setDeltaThreshold(double deltaThreshold) {
        this.deltaThreshold = deltaThreshold;
    }


}

