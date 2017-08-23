package org.getalp.dbnary.enhancer.disambiguation.translations;

import org.getalp.dbnary.enhancer.disambiguation.Ambiguity;
import org.getalp.dbnary.enhancer.disambiguation.Disambiguable;
import org.getalp.dbnary.enhancer.disambiguation.Disambiguator;
import org.getalp.dbnary.enhancer.similarity.SimilarityMeasure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MFSTranslationDisambiguator implements Disambiguator {

    private double randomBaseline;
    private int numberWords;

    private Map<String, SimilarityMeasure> measures;

    {
        measures = new HashMap<>();
        numberWords = 0;
    }

    @Override
    public void registerSimilarity(String method, SimilarityMeasure sim) {
        measures.put(method, sim);
    }

    @Override
    public Set<String> getMethods() {
        return measures.keySet();
    }

    @Override
    public void disambiguate(Ambiguity a, final List<Disambiguable> choices) {
        if (!choices.isEmpty()) {
            randomBaseline += 1.0 / ((double) choices.size());
            numberWords++;
            Disambiguable d = choices.get(0);
            Disambiguable newD = new DisambiguableSense(d.getGloss(), d.getId(),d.getNum());
            newD.setScore(1.0);
            a.addDisambiguation("MFS", newD);
        }
    }

    @Override
    public String toString() {
        return "Random Baseline: " + randomBaseline / (double) numberWords + "\n";
    }
}
