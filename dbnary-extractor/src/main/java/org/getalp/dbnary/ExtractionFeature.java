package org.getalp.dbnary;

import java.util.Locale;

public enum ExtractionFeature {
  MAIN("ontolex"), MORPHOLOGY, ETYMOLOGY, LIME, ENHANCEMENT, STATISTICS, HDT("combined");

  private final String printableName;

  ExtractionFeature(String printableName) {
    this.printableName = printableName;
  }

  ExtractionFeature() {
    this.printableName = this.name().toLowerCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return printableName;
  }
}
