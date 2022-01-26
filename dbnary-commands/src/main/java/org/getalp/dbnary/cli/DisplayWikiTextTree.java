package org.getalp.dbnary.cli;

import java.util.concurrent.Callable;
import org.getalp.dbnary.cli.mixins.WiktionaryIndexMixin;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiTextPrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "tree", mixinStandardHelpOptions = true,
    header = "Parse the specified entries wikitext and display the parse tree to stdout.",
    description = "The wikitext of specified entries is retrieved from the dump and parsed"
        + "using WikiText parser, then the tree is written to stdout.")
public class DisplayWikiTextTree implements Callable<Integer> {

  @Mixin
  protected WiktionaryIndexMixin wi;

  @Option(names = {"--document"},
      description = "Convert the tree as a document tree (hierarchy " + "of sections).")
  private boolean asDocument;

  @Parameters(index = "1..*", description = "The entries to be extracted.", arity = "1..*")
  String[] entries;

  @Override
  public Integer call() throws Exception {
    for (String entry : entries) {
      String source = wi.getTextOfPage(entry);
      WikiText text = new WikiText(entry, source);
      if (asDocument) {
        WikiTextPrinter.printDocumentTree(text.asStructuredDocument());
      } else {
        WikiTextPrinter.printTextTree(text);
      }
    }
    return 0;
  }
}
