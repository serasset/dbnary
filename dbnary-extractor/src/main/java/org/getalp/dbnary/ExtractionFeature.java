package org.getalp.dbnary;

public enum ExtractionFeature {
  MAIN, MORPHOLOGY, ETYMOLOGY, LIME, ENHANCEMENT, STATS;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
