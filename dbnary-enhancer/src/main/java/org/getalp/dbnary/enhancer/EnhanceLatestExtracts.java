package org.getalp.dbnary.enhancer;

import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.getalp.LangTools;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.preprocessing.StatsModule;

import java.io.*;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class EnhanceLatestExtracts {


    private static Options options = null; // Command line options

    private static final String PREFIX_DIR_OPTION = "d";
    private static final String DEFAULT_PREFIX_DIR = ".";

    private CommandLine cmd = null; // Command Line arguments

    private String extractsDir = null;
    private String statsDir = null;

    static {
        options = new Options();
        options.addOption("h", false, "Prints usage and exits. ");
        options.addOption(PREFIX_DIR_OPTION, true,
                "directory containing the extracts and stats. " + DEFAULT_PREFIX_DIR + " by default ");
    }

    String[] remainingArgs;
    private StatsModule stats;
    private EvaluationStats evaluator;
    private TranslationSourcesDisambiguator disambiguator;

    private void loadArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
            printUsage();
            System.exit(1);
        }

        remainingArgs = cmd.getArgs();

        // Check for args
        if (cmd.hasOption("h")) {
            printUsage();
            System.exit(0);
        }

        String prefixDir = DEFAULT_PREFIX_DIR;
        if (cmd.hasOption(PREFIX_DIR_OPTION)) {
            prefixDir = cmd.getOptionValue(PREFIX_DIR_OPTION);
        }
        extractsDir = prefixDir + File.separator + "ontolex" + File.separator + "latest";
        statsDir = prefixDir + File.separator + "stats";

        stats = new StatsModule();
        evaluator = new EvaluationStats();
        disambiguator =
                new TranslationSourcesDisambiguator(0.1, 0.9, 0.05, true, stats, evaluator);

    }

    public static void main(String args[]) throws Exception {
        EnhanceLatestExtracts cliProg = new EnhanceLatestExtracts();
        cliProg.loadArgs(args);
        cliProg.enhanceExtract();

    }

    private void enhanceExtract() throws Exception {

        File d = new File(extractsDir);
        File ds = new File(statsDir);

        if (!d.isDirectory()) {
            System.err.println("Extracts directory not found: " + extractsDir);
        }
        if (!ds.isDirectory()) {
            ds.mkdirs();
        }

        String enhConfidenceFile = statsDir + File.separator + "latest_enhancement_confidence.csv";
        Map<String, String> enhConfidence = readAndParseStats(enhConfidenceFile);

        String glossStatsFile = statsDir + File.separator + "latest_glosses_stats.csv";
        Map<String, String> glossStats = readAndParseStats(glossStatsFile);

        for (File e : d.listFiles((dir, name) -> name.matches(".._dbnary_ontolex\\..*"))) {
            String l2 = e.getName().substring(0, 2);
            String language = LangTools.getCode(l2);
            String elang = LangTools.inEnglish(language);

            String checksum = getCheckSumColumn(enhConfidence.get(elang));
            String md5 = getMD5Checksum(e);
            if (md5.equals(checksum)) {
                System.err.println("Ignoring already available enhancements for: " + e.getName());
                continue;
            }

            System.err.println("Enhancing: " + e.getName());

            try {
                Model inputModel = ModelFactory.createDefaultModel();
                InputStream in = new FileInputStream(e);
                if (e.getName().endsWith(".bz2")) {
                    in = new BZip2CompressorInputStream(in);
                }
                inputModel.read(in, null, "TURTLE");

                Model outputModel = ModelFactory.createDefaultModel();

                disambiguator.processTranslations(inputModel, outputModel, language);

                // Update disambiguation confidence
                StringWriter ow = new StringWriter();

                evaluator.printStat(language, new PrintWriter(ow));
                String stat = ow.toString();
                stat = elang + "," + md5 + "," + stat;
                enhConfidence.put(elang, stat);

                // Update gloss statistics
                ow = new StringWriter();
                stats.printStat(language, new PrintWriter(ow));
                stat = ow.toString();
                stat = elang + "," + stat;
                glossStats.put(elang, stat);

            } catch (Exception ex) {
                System.err.println("Exception caught while computing disambiguations for: " + e.getName());
                System.err.println(ex.getLocalizedMessage());
                ex.printStackTrace(System.err);
            }

        }

        writeStats(enhConfidence, "Language,MD5," + EvaluationStats.getHeaders(), enhConfidenceFile);
        writeStats(glossStats, "Language," + StatsModule.getHeaders(), glossStatsFile);

    }

    private void writeStats(Map<String, String> gstats, String headers, String gstatFile) throws IOException {
        File gs = new File(gstatFile);

        if (!gs.exists() || (gs.isFile() && gs.canWrite())) {
            PrintWriter stats = new PrintWriter(gs, "UTF-8");
            // Print Header
            stats.println(headers);
            for (String s : gstats.values()) {
                stats.println(s);
            }
            stats.flush();
            stats.close();
        }
    }

    private String getCheckSumColumn(String s) {
        return (null == s) ? null : s.split(",")[1];
    }

    private Map<String, String> readAndParseStats(String gstatFile) throws IOException {
        TreeMap<String, String> m = new TreeMap<String, String>();

        File gs = new File(gstatFile);

        if (gs.isFile() && gs.canRead()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(gs), "UTF-8"));
            String h = br.readLine(); // reading header
            String s = br.readLine();
            while (s != null) {
                String line[] = s.split(",");
                m.put(line[0], s);
                s = br.readLine();
            }
            br.close();
        }
        return m;
    }

    public static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String help =
                "Update Latest statistics based on latest extracts.";
        formatter.printHelp("java -cp /path/to/dbnary.jar " + EnhanceLatestExtracts.class.getCanonicalName() + "[OPTIONS]",
                "With OPTIONS in:", options,
                help, false);
    }

    public static byte[] createChecksum(File file) throws Exception {
        InputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[4096];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(File file) throws Exception {
        byte[] b = createChecksum(file);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

}
