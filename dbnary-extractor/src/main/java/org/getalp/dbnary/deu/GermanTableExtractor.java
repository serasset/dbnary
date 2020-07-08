package org.getalp.dbnary.deu;

import org.getalp.dbnary.morphology.TableExtractor;
import org.jsoup.nodes.Element;

public abstract class GermanTableExtractor extends TableExtractor {

  public GermanTableExtractor(String currentEntry) {
    super(currentEntry);
  }

  @Override
  protected boolean shouldIgnoreCurrentH2(Element elt) {
    return !elt.text().contains("Deutsch");
  }

}