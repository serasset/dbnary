package org.getalp.dbnary.experiment.similarity;

import com.wcohen.ss.Level2JaroWinkler;

import java.util.List;

/**
 * Created by tchechem on 03/02/14.
 */
public class MongeElkan implements SimilarityMeasure {

    @Override
    public double compute(String a, String b) {
        Level2JaroWinkler me = new Level2JaroWinkler();
        //Level2Levenstein me = new Level2Levenstein();
        //com.wcohen.ss.MongeElkan me = new com.wcohen.ss.MongeElkan();
        return me.score(a, b);
    }

    public double compute(List<String> a, List<String> b) {
        return 0;
    }
}
