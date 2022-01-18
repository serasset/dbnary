package org.getalp.dbnary.cli.mixins;

import java.io.File;
import org.getalp.LangTools;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.IWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.WiktionaryExtractorFactory;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.DBnary;
import org.getalp.dbnary.cli.utils.VersionProvider;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

public class Extractor {

  private static final String DEFAULT_LANGUAGE = "en";

  // CommandLine specification
  @Spec
  protected CommandSpec spec;
  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  protected ExtractionFeaturesMixin features;

  // Options
  protected String language = DEFAULT_LANGUAGE;
  protected WiktionaryIndex wi;
  protected IWiktionaryExtractor we;
  protected IWiktionaryDataHandler wdh;

  // Parameter
  @Parameters(index = "0", description = "The dump file of the wiki to be extracted.", arity = "1")
  protected File dumpFile;

  @Option(names = {"-l", "--language"}, paramLabel = "LANGUAGE", defaultValue = DEFAULT_LANGUAGE,
      description = "language edition of the dump to be extracted; uses a 2 or 3 iso letter code;"
          + " Default: ${DEFAULT-VALUE}.")
  public void setLanguage(String language) {
    this.language = LangTools.getCode(language);
    if (null == this.language) {
      throw new ParameterException(spec.commandLine(), String.format(
          "Invalid language '%s' for option '--language': unknown language code.", language));
    }
  }

  protected Integer setupHandlers(String tdbDir) throws WiktionaryIndexerException {
    we = null;

    wdh = WiktionaryDataHandlerFactory.getDataHandler(language, tdbDir);
    we = WiktionaryExtractorFactory.getExtractor(language, wdh);

    if (null == we) {
      spec.commandLine().getErr()
          .println("Wiktionary Extraction not yet available for " + LangTools.inEnglish(language));
      return -1;
    }
    wi = new WiktionaryIndex(dumpFile);
    we.setWiktionaryIndex(wi);

    features.getEndolexFeatures().forEach(f -> wdh.enableEndolexFeatures(f));
    if (null != features.getExolexFeatures())
      features.getExolexFeatures().forEach(f -> wdh.enableExolexFeatures(f));

    return 0;
  }

  protected void postProcessAfterExtraction(String version) {
    if (parent.isVerbose())
      spec.commandLine().getErr().println("Postprocessing extracted entries.");
    we.postProcessData(version);
    we.computeStatistics(version);
    we.populateMetadata(version, new VersionProvider().getExtractorVersion());
  }


}
