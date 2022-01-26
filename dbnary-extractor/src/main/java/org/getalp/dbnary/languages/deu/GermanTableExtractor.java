package org.getalp.dbnary.languages.deu;

import org.getalp.dbnary.morphology.TableExtractor;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GermanTableExtractor extends TableExtractor {
  private Logger log = LoggerFactory.getLogger(GermanTableExtractor.class);


  public GermanTableExtractor() {
    super();
  }

  @Override
  protected boolean shouldIgnoreCurrentH2(Element elt) {
    return !elt.text().contains("Deutsch");
  }

}
