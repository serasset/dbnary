package org.getalp.dbnary.cli.mixins;

import org.getalp.LangTools;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.IWiktionaryExtractor;
import org.getalp.dbnary.languages.WiktionaryDataHandlerFactory;
import org.getalp.dbnary.languages.WiktionaryExtractorFactory;
import org.getalp.wiktionary.WiktionaryIndexerException;
import org.getalp.dbnary.cli.DBnary;
import org.getalp.dbnary.cli.utils.VersionProvider;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

public class Extractor {

  // CommandLine specification
  @Spec
  protected CommandSpec spec;
  @ParentCommand
  protected DBnary parent; // picocli injects reference to parent command
  @Mixin
  protected ExtractionFeaturesMixin features;
  @Mixin
  protected WiktionaryIndexMixin wi;
  @Mixin
  protected SingleLanguageAsOptionMixin lm;

  // Options
  protected IWiktionaryExtractor we;
  protected IWiktionaryDataHandler wdh;

  protected Integer setupHandlers(String tdbDir) throws WiktionaryIndexerException {
    we = null;

    wdh = WiktionaryDataHandlerFactory.getDataHandler(lm.getLanguage(), tdbDir);
    we = WiktionaryExtractorFactory.getExtractor(lm.getLanguage(), wdh);

    if (null == we) {
      spec.commandLine().getErr().println(
          "Wiktionary Extraction not yet available for " + LangTools.inEnglish(lm.getLanguage()));
      return -1;
    }
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

  protected void cleanupHandlers() {
    wdh.closeDataset();
    if (null != wi)
      wi.close();
    wi = null;
    we = null;
    wdh = null;
  }

}
