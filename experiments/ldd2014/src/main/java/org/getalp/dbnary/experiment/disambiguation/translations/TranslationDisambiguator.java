package org.getalp.dbnary.experiment.disambiguation.translations;

import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;

import java.util.List;


public class TranslationDisambiguator implements Disambiguator {
    @Override
    public void disambiguate(Ambiguity a, final List<Disambiguable> choices) {
        SimilarityMeasure s = new TsverskiIndex(0.5, 0.5, true);
        for (Disambiguable d : choices) {
            double sim = s.compute(d.getGloss(), a.getGloss());
            d.setScore(sim);
            a.addDisambiguation(d);
        }
    }
}
