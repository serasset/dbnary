package org.getalp.dbnary.api;

public interface WiktionaryPageSource {

  String getTextOfPageWithRedirects(String key);

  String getTextOfPage(String key);

  String getFullXmlForPage(String key);
}
