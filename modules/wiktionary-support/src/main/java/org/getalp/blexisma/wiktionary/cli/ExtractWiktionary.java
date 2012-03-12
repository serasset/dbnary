package org.getalp.blexisma.wiktionary.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.semnet.SimpleSemanticNetwork;
import org.getalp.blexisma.semnet.StringSemNetGraphMLizer;
import org.getalp.blexisma.wiktionary.EnglishWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.FrenchRDFWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.FrenchWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.GermanWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.OffsetValue;
import org.getalp.blexisma.wiktionary.SemnetWiktionaryDataHandler;
import org.getalp.blexisma.wiktionary.WiktionaryDataHandler;
import org.getalp.blexisma.wiktionary.WiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryIndex;
import org.getalp.blexisma.wiktionary.WiktionaryIndexer;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class ExtractWiktionary {

	private static Options options = null; // Command line options

	private static final String LANGUAGE_OPTION = "l";
	private static final String DEFAULT_LANGUAGE = "fr";

	private static final String OUTPUT_FORMAT_OPTION = "f";
	private static final String DEFAULT_OUTPUT_FORMAT = "raw";
	
	private static final String OUTPUT_FILE_OPTION = "o";
	private static final String DEFAULT_OUTPUT_FILE = "fr_extract";
	
	private static final String SUFFIX_OUTPUT_FILE_OPTION = "s";

    public static final XMLInputFactory2 xmlif;


	private CommandLine cmd = null; // Command Line arguments
	
	private String outputFile = DEFAULT_OUTPUT_FILE;
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	private String language = DEFAULT_LANGUAGE;
	private File dumpFile;
	private String outputFileSuffix = "";
	WiktionaryIndex wi;
	String[] remainingArgs;
	WiktionaryExtractor we;

	private SimpleSemanticNetwork<String, String> s = null;

	private WiktionaryDataHandler wdh;

	
	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(SUFFIX_OUTPUT_FILE_OPTION, false, "Add a unique suffix to output file. ");	
		options.addOption(LANGUAGE_OPTION, true, 
				"Language (graphml, raw, rdf, turtle, ntriple, n3, ttl or rdfabbrev). " + DEFAULT_LANGUAGE + " by default.");
		options.addOption(OUTPUT_FORMAT_OPTION, true, 
				"Output format (graphml or raw). " + DEFAULT_OUTPUT_FORMAT + " by default.");
		options.addOption(OUTPUT_FILE_OPTION, true, "Output file. " + DEFAULT_OUTPUT_FILE + " by default ");	
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
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		ExtractWiktionary cliProg = new ExtractWiktionary();
		cliProg.loadArgs(args);
		cliProg.extract();
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
		
		if (cmd.hasOption(SUFFIX_OUTPUT_FILE_OPTION)){
			SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			outputFileSuffix = df.format(new Date());
		}
		
		if (cmd.hasOption(OUTPUT_FORMAT_OPTION)){
			outputFormat = cmd.getOptionValue(OUTPUT_FORMAT_OPTION);
		}
		outputFormat = outputFormat.toUpperCase();
				
		if (cmd.hasOption(OUTPUT_FILE_OPTION)){
			outputFile = cmd.getOptionValue(OUTPUT_FILE_OPTION);
		}
		
		if (cmd.hasOption(LANGUAGE_OPTION)){
			language = cmd.getOptionValue(LANGUAGE_OPTION);
			language = ISO639_3.sharedInstance.getIdCode(language);
			if (! (language.equals("fra") || language.equals("eng") || language.equals("deu"))) {
				printUsage();
				System.exit(1);
			}
		}
		
		String[] remainingArgs = cmd.getArgs();
		if (remainingArgs.length != 1) {
			printUsage();
			System.exit(1);
		}
		
		we = null;
		if (	outputFormat.equals("RDF") || 
				outputFormat.equals("TURTLE") ||
				outputFormat.equals("NTRIPLE") ||
				outputFormat.equals("N3") ||
				outputFormat.equals("TTL") ||
				outputFormat.equals("RDFABBREV") ) {
			wdh = new FrenchRDFWiktionaryExtractor();
		} else if (outputFormat.equals("RAW") || outputFormat.equals("GRAPHML")) {
			s = new SimpleSemanticNetwork<String, String>();
			wdh = new SemnetWiktionaryDataHandler(s, language);
		} else {
			System.err.println("unsupported format :" + outputFormat);
			System.exit(1);
		}
		
		if (language.equals("fra")) {
			we = new FrenchWiktionaryExtractor(wdh);
		} else if (language.equals("eng")) {
			we = new EnglishWiktionaryExtractor(wdh);
		} else if (language.equals("deu")) {
			we = new GermanWiktionaryExtractor(wdh);
		} else {
			System.err.println("Wiktionary Extraction not yet available for " + ISO639_3.sharedInstance.getLanguageNameInEnglish(language));
			System.exit(1);
		}
		
		outputFile = outputFile + outputFileSuffix;
		 
		dumpFile = new File(remainingArgs[0]);
	}
	
    public void extract() throws WiktionaryIndexerException, IOException {
        
        // create new XMLStreamReader

        long startTime = System.currentTimeMillis();
        long totalRelevantTime = 0, relevantstartTime = 0, relevantTimeOfLastThousands;
        int nbpages = 0, nbrelevantPages = 0;
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
                        nbpages++;
                        int nbnodes = wdh.nbEntries();
                		we.extractData(title, page);
                		if (nbnodes != wdh.nbEntries()) {
                			totalRelevantTime += (System.currentTimeMillis() - relevantstartTime);
                			nbrelevantPages++;
                			if (nbrelevantPages % 1000 == 0) {
                				System.err.println("Extracted: " + nbrelevantPages + " pages in: " + totalRelevantTime + " / Average = " 
                						+ (totalRelevantTime/nbrelevantPages) + " ms/extracted page (" + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000 + " ms) (" + nbpages 
                						+ " processed Pages in " + (System.currentTimeMillis() - startTime) + " ms / Average = " + (System.currentTimeMillis() - startTime) / nbpages + ")" );
                				// System.err.println("      NbNodes = " + s.getNbNodes());
                				relevantTimeOfLastThousands = System.currentTimeMillis();
                			}
                			// if (nbrelevantPages == 1100) break;
                		}	
                	}
                }
            }
        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());

            if (ex.getNestedException() != null) {
                ex.getNestedException().printStackTrace();
            }
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
           
        System.err.println("Dumping " + outputFormat + " representation of the extracted data.");
        if (outputFormat.equals("GRAPHML")) {
        	StringSemNetGraphMLizer gout = new StringSemNetGraphMLizer(new OutputStreamWriter(new FileOutputStream(outputFile)), StringSemNetGraphMLizer.MULLING_OUTPUT);
        	gout.dump(s);
        } else if (outputFormat.equals("RAW")) {  
        	s.dumpToWriter(new PrintStream(outputFile));
        } else if (outputFormat.equals("RDF")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile));
        } else if (outputFormat.equals("TURTLE")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile), "TURTLE");
        } else if (outputFormat.equals("NTRIPLE")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile), "N-TRIPLE");
        } else if (outputFormat.equals("N3")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile), "N3");
        } else if (outputFormat.equals("TTL")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile), "TTL");
        } else if (outputFormat.equals("RDFABBREV")) {
        	((FrenchRDFWiktionaryExtractor) wdh).dump(new PrintStream(outputFile), "RDF/XML-ABBREV");
        } 
  
        System.err.println(nbpages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
        System.err.println("Semnet contains: " + wdh.nbEntries() + " nodes.");
    }

    
    public static void printUsage() {
    	HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.blexisma.wiktionary.cli.ExtractWiktionaryUsingIndex [OPTIONS] dumpFile", 
				"With OPTIONS in:", options, 
				"dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index.", false);
    }

}
