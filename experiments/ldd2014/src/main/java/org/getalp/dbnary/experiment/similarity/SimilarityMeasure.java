package org.getalp.dbnary.experiment.similarity;


import java.util.List;

public interface SimilarityMeasure {
    public double compute(String a, String b);

    public double compute(List<String> a, List<String> b);
}
