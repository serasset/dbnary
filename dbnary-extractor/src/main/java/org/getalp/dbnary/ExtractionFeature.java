package org.getalp.dbnary;

public enum ExtractionFeature {
  MAIN, MORPHOLOGY, ETYMOLOGY, LIME, ENHANCEMENT, STATISTICS, EXOLEXICON;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
