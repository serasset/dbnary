package org.getalp.dbnary.commons;

import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiText;

public class FunctionalWiktionaryExtractor extends AbstractWiktionaryExtractor {

  protected WikiText wikiText;

  public FunctionalWiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  // SHOULD WE USE TREE VISITING PATTERN or attach actions to XPath/like patterns ?


  @Override
  public void extractData() {
    wikiText = new WikiText(pageContent);


  }
}
