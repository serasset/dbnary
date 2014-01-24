package org.getalp.dbnary.experiment;

import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;

public class LLD2014Main {

    public static void main(String[] args) {

        SimilarityMeasure s = new TsverskiIndex(0.5, 0.5, true);
        String a = "the act of expelling or letting go";
        String b = "the act of accomplishing (an obligation); performance";
        System.err.println(s.compute(a, b));
    }
}
