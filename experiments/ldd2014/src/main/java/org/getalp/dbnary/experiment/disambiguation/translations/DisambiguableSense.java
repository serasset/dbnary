package org.getalp.dbnary.experiment.disambiguation.translations;

import org.getalp.dbnary.experiment.disambiguation.Disambiguable;

public class DisambiguableSense implements Disambiguable {

    private String gloss;

    private double score;

    private boolean processed;

    private String id;

    {
        processed = false;
    }


    public DisambiguableSense(final String gloss, final String id) {
        this.gloss = gloss;
        this.id = id;
    }

    @Override
    public String getGloss() {
        return gloss;
    }

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public void setScore(final double score) {
        processed = true;
        this.score = score;
    }

    @Override
    public boolean hasBeenProcessed() {
        return processed;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId() + "\t" + getScore();
    }
}
