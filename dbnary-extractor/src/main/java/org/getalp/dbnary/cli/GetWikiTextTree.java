package org.getalp.dbnary.cli;

import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiTextPrinter;

public class GetWikiTextTree {

  /**
   * @param args arguments of the command line
   * @throws WiktionaryIndexerException if any error occurs with indexer.
   */
  public static void main(String[] args) throws WiktionaryIndexerException {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }

    WiktionaryIndex wi = new WiktionaryIndex(args[0]);

    for (int i = 1; i < args.length; i++) {
      String source = wi.getTextOfPage(args[i]);
      WikiText text = new WikiText(args[i], source);
      WikiTextPrinter.printTextTree(text);
    }
  }


  public static void printUsage() {
    System.err.println("Usage: ");
    System.err
        .println("  java org.getalp.dbnary.cli.GetWikiTextTree wiktionaryDumpFile entryname ...");
    System.err.println("Displays the raw text of the wiktionary page named \"entryname\".");
  }
}
