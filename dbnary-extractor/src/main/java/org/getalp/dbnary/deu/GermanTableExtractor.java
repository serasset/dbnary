package org.getalp.dbnary.deu;

import java.util.HashSet;
import java.util.Set;
import org.getalp.dbnary.morphology.TableExtractor;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GermanTableExtractor extends TableExtractor {
  private Logger log = LoggerFactory.getLogger(GermanTableExtractor.class);


  public GermanTableExtractor(String currentEntry) {
    super(currentEntry);
  }

  @Override
  protected boolean shouldIgnoreCurrentH2(Element elt) {
    return !elt.text().contains("Deutsch");
  }

}
