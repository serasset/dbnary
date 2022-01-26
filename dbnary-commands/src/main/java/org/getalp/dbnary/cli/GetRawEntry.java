package org.getalp.dbnary.cli;

import java.util.concurrent.Callable;
import org.getalp.dbnary.cli.mixins.WiktionaryIndexMixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "source", mixinStandardHelpOptions = true,
    header = "get the wikitext source of the specified pages.",
    description = "The wikitext of specified pages is retrieved from the dump and written "
        + "to stdout.")
public class GetRawEntry implements Callable<Integer> {

  @Mixin
  protected WiktionaryIndexMixin wi;

  @Option(names = {"--all"}, description = "dump the whole xml structure of the page.")
  private boolean all;

  @Option(names = {"--redirect"}, description = "process redirects to get the target page.")
  private boolean redirect;

  @Parameters(index = "1..*", description = "The entries to be extracted.", arity = "1..*")
  String[] entries;

  @Override
  public Integer call() {
    for (String entry : entries) {
      if (all) {
        System.out.println(wi.getFullXmlForPage(entry));
      } else if (redirect) {
        System.out.println(wi.getTextOfPageWithRedirects(entry));
      } else {
        System.out.println(wi.getTextOfPage(entry));
      }
    }
    return 0;
  }
}
