package org.getalp.dbnary.experiment.disambiguation.translations;


import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;

import java.util.ArrayList;
import java.util.List;

public class TranslationAmbiguity implements Ambiguity {

    private String gloss;
    private List<Disambiguable> disambiguations;
    private Disambiguable best;
    private String id;

    {
        disambiguations = new ArrayList<>();
    }

    public TranslationAmbiguity(final String gloss, final String id) {
        this.gloss = gloss;
        this.id = id;
    }

    @Override
    public String getGloss() {
        return gloss;
    }

    @Override
    public void addDisambiguation(final Disambiguable d) {
        if (d.hasBeenProcessed()) {
            if (best == null || d.getScore() > best.getScore()) {
                best = d;
            }
            disambiguations.add(d);
        }
    }

    @Override
    public List<Disambiguable> getDisambiguation() {
        return disambiguations;
    }

    @Override
    public Disambiguable getBestDisambiguation() {
        return best;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        String ret = getId() + ' ' + "00 ";
        if (getBestDisambiguation() != null) {
            ret += getBestDisambiguation().getId();
        }
        ret += "1 ";
        if (getBestDisambiguation() != null) {
            ret += getBestDisambiguation().getScore();
        }
        return ret;
    }

    @Override
    public int compareTo(Ambiguity o) {
        return o.getId().compareTo(getId());
    }
}
