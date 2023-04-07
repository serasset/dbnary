package org.getalp.dbnary.api;

public interface IStructureChecker {
  void setWiktionaryIndex(WiktionaryPageSource wi);

  void checkPage(String wiktionaryPageName, String pageContent);

}
