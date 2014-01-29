package org.getalp.dbnary.experiment.disambiguation;

import java.util.List;

public interface Disambiguator {
    public void disambiguate(Ambiguity a, final List<Disambiguable> choices);
}
