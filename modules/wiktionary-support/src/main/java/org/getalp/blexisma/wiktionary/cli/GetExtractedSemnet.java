package org.getalp.blexisma.wiktionary.cli;

import org.getalp.blexisma.wiktionary.FrenchWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.SimpleSemanticNetwork;
import org.getalp.blexisma.wiktionary.WiktionaryIndex;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class GetExtractedSemnet {

    public static void main(String[] args) throws WiktionaryIndexerException {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        
        WiktionaryIndex wi = new WiktionaryIndex(args[0]);
        FrenchWiktionaryExtractor fwe = new FrenchWiktionaryExtractor(wi);
        SimpleSemanticNetwork<String, String> s = new SimpleSemanticNetwork<String, String>(5000, 5000);
        s.clear();
        for(int i = 1; i < args.length; i++) {
            fwe.extractData(args[i], s);
            s.dumpToWriter(System.out);
            s.clear();
        }
    }

    
    public static void printUsage() {
        System.err.println("Usage: ");
        System.err.println("  java org.getalp.blexisma.wiktionary.cli.GetExtractedSemnet wiktionaryDumpFile entryname ...");
        System.err.println("Displays the extracted semnet of the wiktionary page named \"entryname\".");
    }

}
