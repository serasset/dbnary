package org.getalp.dbnary.enhancer.similarity;


public interface SimilarityMeasure {
  public double compute(String a, String b);
}
