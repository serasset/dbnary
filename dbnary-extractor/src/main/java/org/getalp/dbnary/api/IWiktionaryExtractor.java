package org.getalp.dbnary.api;

import org.getalp.dbnary.api.WiktionaryPageSource;

public interface IWiktionaryExtractor {

  void setWiktionaryIndex(WiktionaryPageSource wi);

  void extractData(String wiktionaryPageName, String pageContent);

  void postProcessData(String dumpFileName);

  void populateMetadata(String dumpFileName, String extractorVersion);

  void computeStatistics(String dumpVersion);
}
