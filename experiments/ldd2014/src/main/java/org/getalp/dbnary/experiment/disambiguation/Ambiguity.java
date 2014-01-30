package org.getalp.dbnary.experiment.disambiguation;


import java.util.List;

public interface Ambiguity extends Comparable<Ambiguity> {
    public String getGloss();

    public String getId();

    public void addDisambiguation(final Disambiguable d);

    public List<Disambiguable> getDisambiguation();

    public Disambiguable getBestDisambiguation();
}
