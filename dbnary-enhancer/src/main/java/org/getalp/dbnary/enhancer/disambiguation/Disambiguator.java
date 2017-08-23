package org.getalp.dbnary.enhancer.disambiguation;

import org.getalp.dbnary.enhancer.similarity.SimilarityMeasure;

import java.util.List;
import java.util.Set;

public interface Disambiguator {
    public void disambiguate(Ambiguity a, final List<Disambiguable> choices);

    public void registerSimilarity(String method, SimilarityMeasure sim);

    public Set<String> getMethods();
}
