package org.getalp.dbnary;

public interface IWiktionaryExtractor {

  void setWiktionaryIndex(WiktionaryIndex wi);

  void extractData(String wiktionaryPageName, String pageContent);

  void postProcessData(String dumpFileName);

  void populateMetadata(String dumpFileName, String extractorVersion);

  void computeStatistics(String dumpVersion);
}
