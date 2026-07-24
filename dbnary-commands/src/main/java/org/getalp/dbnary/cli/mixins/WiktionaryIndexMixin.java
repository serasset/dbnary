package org.getalp.dbnary.cli.mixins;

import java.io.File;
import java.nio.file.Path;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.cli.utils.DBnaryCommandLineException;
import org.getalp.wiktionary.WiktionaryIndex;
import org.getalp.wiktionary.WiktionaryIndexerException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

public class WiktionaryIndexMixin implements WiktionaryPageSource {
  @Spec(Spec.Target.MIXEE)
  CommandSpec mixee;

  protected WiktionaryIndex wi;

  protected Path dump;
  protected File dumpFile;

  // Set explicitly (not a @Parameters/@Option setter) by the owning command once its own
  // language option has been parsed, so we never depend on picocli's argument processing order.
  protected String language;

  @Option(names = {"--fill-missing-pages"}, scope = ScopeType.INHERIT,
      description = "MediaWiki's XML export is known to occasionally omit pages that do exist "
          + "on the live wiki. When set, a page missing from the dump is looked up on the live "
          + "MediaWiki API instead of being treated as non existent; the outcome is cached next "
          + "to the dump so a title is never queried more than once. Requires the language to be " + "known (see --language). Default: ${DEFAULT-VALUE}.")
  protected boolean fillMissingPages = false;

  @Parameters(index = "0", description = "The dump file of the wiki to be extracted.", arity = "1", scope = ScopeType.INHERIT)
  protected void setDumpFile(Path dump) {
    this.dump = dump;
    this.dumpFile = dump.toFile();
  }

  /**
   * Lets the owning command supply the language edition of the dump, needed to reach the
   * corresponding wiktionary API when {@code --fill-missing-pages} is set. Must be called (if at all)
   * before the first page lookup.
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  public File getDumpFile() {
    return dumpFile;
  }

  private WiktionaryIndex index() {
    if (null == wi) {
      try {
        wi = new WiktionaryIndex(dump, language, fillMissingPages);
      } catch (WiktionaryIndexerException e) {
        throw new DBnaryCommandLineException(String.format("Could not use dump file '%s'", dump), e);
      }
    }
    return wi;
  }

  @Override
  public String getTextOfPageWithRedirects(String key) {
    return index().getTextOfPageWithRedirects(key);
  }

  @Override
  public String getTextOfPage(String key) {
    return index().getTextOfPage(key);
  }

  @Override
  public String getFullXmlForPage(String key) {
    return index().getFullXmlForPage(key);
  }

  public void close() {
    if (null != wi)
      wi.close();
    wi = null;
    dump = null;
    dumpFile = null;
  }
}
