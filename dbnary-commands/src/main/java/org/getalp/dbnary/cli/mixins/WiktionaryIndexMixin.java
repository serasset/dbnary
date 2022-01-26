package org.getalp.dbnary.cli.mixins;

import java.io.File;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.wiktionary.WiktionaryIndex;
import org.getalp.wiktionary.WiktionaryIndexerException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

public class WiktionaryIndexMixin implements WiktionaryPageSource {
  @Spec(Spec.Target.MIXEE)
  CommandSpec mixee;

  protected WiktionaryIndex wi;

  protected File dumpFile;

  @Parameters(index = "0", description = "The dump file of the wiki to be extracted.", arity = "1")
  protected void setDumpFile(File dump) {
    this.dumpFile = dump;
    try {
      this.wi = new WiktionaryIndex(dumpFile);
    } catch (WiktionaryIndexerException e) {
      e.printStackTrace();
      throw new ParameterException(mixee.commandLine(),
          String.format("Could not use dump file '%s'", dump.toString()));
    }
  }

  public File getDumpFile() {
    return dumpFile;
  }

  @Override
  public String getTextOfPageWithRedirects(String key) {
    return wi.getTextOfPageWithRedirects(key);
  }

  @Override
  public String getTextOfPage(String key) {
    return wi.getTextOfPage(key);
  }

  @Override
  public String getFullXmlForPage(String key) {
    return wi.getFullXmlForPage(key);
  }

  public void close() {
    if (null != wi)
      wi.close();
    wi = null;
    dumpFile = null;
  }
}
