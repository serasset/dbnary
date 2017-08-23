package org.getalp.dbnary.enhancer.disambiguation;

public interface Disambiguable {
    public String getGloss();

    public double getScore();

    public String getId();
    public String getNum();
    public void setScore(final double score);

    public boolean hasBeenProcessed();
}
