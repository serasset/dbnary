package org.getalp.dbnary.experiment.disambiguation.translations;

import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.similarity.MongeElkan;
import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;

import java.util.List;


public class TranslationDisambiguator implements Disambiguator {
    @Override
    public void disambiguate(Ambiguity a, final List<Disambiguable> choices) {
        SimilarityMeasure s = new TsverskiIndex(0.1, 0.9, true);
        //SimilarityMeasure s = new MongeElkan();
        for (Disambiguable d : choices) {
            double sim = s.compute(d.getGloss(), a.getGloss());
            Disambiguable newD = new DisambiguableSense(d.getGloss(), d.getId());
            newD.setScore(sim);
            a.addDisambiguation(newD);
        }
    }
}
