package org.getalp.blexisma.wiktionary.cli;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Pattern;

import org.getalp.blexisma.wiktionary.EnglishWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.FrenchWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.GermanWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryGrep;
import org.getalp.blexisma.wiktionary.WiktionaryIndex;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class GrepInWiktionary {

    /**
     * @param args
     * @throws WiktionaryIndexerException if any error occurs with indexer.
     */
    public static void main(String[] args) throws WiktionaryIndexerException {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        Pattern pat = Pattern.compile(args[0]);
        File in = new File(args[1]);
        Writer out = new OutputStreamWriter(System.out);
        
        WiktionaryGrep.grep(in, pat, out);
    }

    
    public static void printUsage() {
        System.err.println("Usage: ");
        System.err.println("  java org.getalp.blexisma.wiktionary.cli.GrepInWiktionary pattern wiktionaryDumpFile");
        System.err.println("Displays the title of the first entry text of the wiktionary page named \"entryname\".");
        System.err.println("OPTIONS:");
        System.err.println("  --all (-a): Display all the xml elements defining the page.");
        System.err.println("  --        : Stops the sequence of options and start the sequence of entrynames.");   
        System.err.println("              This option is usefull when the wiktionaryDumpFile begins with a \"-\".");
    }
}
