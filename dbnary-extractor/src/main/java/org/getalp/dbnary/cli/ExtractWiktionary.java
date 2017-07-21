package org.getalp.dbnary.cli;

import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.dbnary.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.getalp.dbnary.IWiktionaryDataHandler.Feature;

public class ExtractWiktionary extends DbnaryCommandLine {

    private static final String OUTPUT_FILE_OPTION = "o";
    private static final String DEFAULT_OUTPUT_FILE = "extract";

    private static final String SUFFIX_OUTPUT_FILE_OPTION = "s";

    private static final String COMPRESS_OPTION = "z";
    private static final String DEFAULT_COMPRESS = "no";

    private static final String FROM_PAGE_LONG_OPTION = "frompage";
    private static final String FROM_PAGE_SHORT_OPTION = "F";

    private static final String TO_PAGE_LONG_OPTION = "topage";
    private static final String TO_PAGE_SHORT_OPTION = "T";

    public static final XMLInputFactory2 xmlif;


    private String outputFile = DEFAULT_OUTPUT_FILE;
    private String outputFormat = DEFAULT_OUTPUT_FORMAT;
    private boolean compress;
    private File dumpFile;
    private String outputFileSuffix = "";
    private int fromPage = 0;
    private int toPage = Integer.MAX_VALUE;

    static {
        options.addOption(SUFFIX_OUTPUT_FILE_OPTION, false, "Add a unique suffix to output file. ");
        options.addOption(COMPRESS_OPTION, true,
                "Compress the output using bzip2 (value: yes/no or true/false). " + DEFAULT_COMPRESS + " by default.");
        options.addOption(OUTPUT_FILE_OPTION, true, "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");
        options.addOption(OptionBuilder.withLongOpt(FROM_PAGE_LONG_OPTION)
                .withDescription("Do not process pages before the nth one. 0 by default.")
                .hasArg()
                .withArgName("num")
                .create(FROM_PAGE_SHORT_OPTION));
        options.addOption(OptionBuilder.withLongOpt(TO_PAGE_LONG_OPTION)
                .withDescription("Do not process pages after the nth one. MAXINT by default.")
                .hasArg()
                .withArgName("num")
                .create(TO_PAGE_SHORT_OPTION));
    }

    static {
        try {
            xmlif = (XMLInputFactory2) XMLInputFactory2.newInstance();
            xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            xmlif.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        } catch (Exception ex) {
            System.err.println("Cannot intialize XMLInputFactory while classloading WiktionaryIndexer.");
            throw new RuntimeException("Cannot initialize XMLInputFactory", ex);
        }
    }


    /**
     * @param args arguments
     * @throws IOException                ...
     * @throws WiktionaryIndexerException ...
     */
    public static void main(String[] args) throws WiktionaryIndexerException, IOException {
        ExtractWiktionary cliProg = new ExtractWiktionary();
        cliProg.loadArgs(args);
        cliProg.extract();
    }

    /**
     * Validate and set command line arguments.
     * Exit after printing usage if anything is astray
     *
     * @param args String[] args as featured in public static void main()
     * @throws WiktionaryIndexerException ..
     */
    protected void loadArgs(String[] args) throws WiktionaryIndexerException {
        super.loadArgs(args);

        if (cmd.hasOption(SUFFIX_OUTPUT_FILE_OPTION)) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            outputFileSuffix = df.format(new Date());
        }

        outputFormat = outputFormat.toUpperCase();


        String compress_value = cmd.getOptionValue(COMPRESS_OPTION, DEFAULT_COMPRESS);
        compress = "true".startsWith(compress_value) || "yes".startsWith(compress_value);

        if (cmd.hasOption(OUTPUT_FILE_OPTION)) {
            outputFile = cmd.getOptionValue(OUTPUT_FILE_OPTION);
        }

        if (cmd.hasOption(FROM_PAGE_LONG_OPTION)) {
            fromPage = Integer.valueOf(cmd.getOptionValue(FROM_PAGE_LONG_OPTION));
        }

        if (cmd.hasOption(TO_PAGE_LONG_OPTION)) {
            toPage = Integer.valueOf(cmd.getOptionValue(TO_PAGE_LONG_OPTION));
        }

        outputFile = outputFile + outputFileSuffix;

        dumpFile = new File(remainingArgs[0]);
    }

    public void extract() throws WiktionaryIndexerException, IOException {

        // create new XMLStreamReader

        long startTime = System.currentTimeMillis();
        long totalRelevantTime = 0, relevantStartTime = 0, relevantTimeOfLastThousands;
        int nbPages = 0, nbRelevantPages = 0;
        relevantTimeOfLastThousands = System.currentTimeMillis();

        XMLStreamReader2 xmlr = null;
        try {
            // pass the file name. all relative entity references will be
            // resolved against this as base URI.
            xmlr = xmlif.createXMLStreamReader(dumpFile);

            // check if there are more events in the input stream
            String title = "";
            String page = "";
            while (xmlr.hasNext()) {
                xmlr.next();
                if (xmlr.isStartElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
                    title = "";
                    page = "";
                } else if (xmlr.isStartElement() && xmlr.getLocalName().equals(WiktionaryIndexer.titleTag)) {
                    title = xmlr.getElementText();
                } else if (xmlr.isStartElement() && xmlr.getLocalName().equals("text")) {
                    page = xmlr.getElementText();
                } else if (xmlr.isEndElement() && xmlr.getLocalName().equals(WiktionaryIndexer.pageTag)) {
                    if (!title.equals("")) {
                        nbPages++;
                        int nbnodes = wdh.nbEntries();
                        if (nbPages < fromPage) continue;
                        if (nbPages > toPage) break;
                        try {
                            we.extractData(title, page);
                        } catch (RuntimeException ex) {
                            System.err.println("ERROR : Unexpected/uncaught Rutine Exception while extracting page " + title);
                            System.err.println(ex.getMessage());
                            ex.printStackTrace();
                        }
                        if (nbnodes != wdh.nbEntries()) {
                            totalRelevantTime += (System.currentTimeMillis() - relevantStartTime);
                            nbRelevantPages++;
                            if (nbRelevantPages % 1000 == 0) {
                                System.err.println("Extracted: " + nbRelevantPages + " pages in: " + totalRelevantTime + " / Average = "
                                        + (totalRelevantTime / nbRelevantPages) + " ms/extracted page (" + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000 + " ms) (" + nbPages
                                        + " processed Pages in " + (System.currentTimeMillis() - startTime) + " ms / Average = " + (System.currentTimeMillis() - startTime) / nbPages + ")");
                                // System.err.println("      NbNodes = " + s.getNbNodes());
                                relevantTimeOfLastThousands = System.currentTimeMillis();
                            }
                            // if (nbRelevantPages == 1100) break;
                        }
                    }
                }
            }

            saveBox(Feature.MAIN, outputFile);
            System.err.println(nbPages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
            System.err.println("Semnet contains: " + wdh.nbEntries() + " nodes.");
            if (null != morphoOutputFile) {
                saveBox(Feature.MORPHOLOGY, morphoOutputFile);
            }
            if (null != etymologyOutputFile) {
                saveBox(Feature.ETYMOLOGY, etymologyOutputFile);
            }

        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());

            if (ex.getNestedException() != null) {
                ex.getNestedException().printStackTrace();
            }
            throw new IOException("XML Stream Exception while reading dump", ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (xmlr != null)
                    xmlr.close();
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }


    }

    public void saveBox(IWiktionaryDataHandler.Feature f, String of) throws IOException {
        OutputStream ostream;
        if (compress) {
            // outputFile = outputFile + ".bz2";
            ostream = new BZip2CompressorOutputStream(new FileOutputStream(of));
        } else {
            ostream = new FileOutputStream(of);
        }
        try {
            System.err.println("Dumping " + outputFormat + " representation of " + f.name() + ".");
            if (outputFormat.equals("RDF")) {
                wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), null);
            } else {
                wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), outputFormat);
            }
        } catch (IOException e) {
            System.err.println("Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            throw e;
        } finally {
            if (null != ostream) {
                ostream.flush();
                ostream.close();
            }
        }
    }

    @Override
    protected String getHelpText() {
        return "dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index." +
                System.getProperty("line.separator", "\n") +
                "Extracts the full wiktionary dump and store the resulting dataset in the specified file.";
    }

}
