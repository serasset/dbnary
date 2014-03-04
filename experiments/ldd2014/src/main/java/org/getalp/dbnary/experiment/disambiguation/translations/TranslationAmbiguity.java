package org.getalp.dbnary.experiment.disambiguation.translations;


import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;

import java.util.*;

public class TranslationAmbiguity implements Ambiguity {

    private String gloss;
    private Map<String, List<Disambiguable>> disambiguations;
    private Map<String, Disambiguable> best;
    private String id;
    private double threshold;

    private Disambiguable voteResult;
    private VoteType voteType;

    {
        disambiguations = new HashMap<>();
        best = new HashMap<>();
    }

    public TranslationAmbiguity(final String gloss, final String id) {
        this.gloss = gloss;
        this.id = id;
        voteType = VoteType.MAJORITY;
    }

    public TranslationAmbiguity(final String gloss, final String id,double threshold) {
        this.gloss = gloss;
        this.id = id;
        voteType = VoteType.MAJORITY;
        this.threshold = threshold;
    }

    @Override
    public String getGloss() {
        return gloss;
    }

    @Override
    public void addDisambiguation(String method, final Disambiguable d) {
        if (d.hasBeenProcessed()) {

            if (!best.containsKey(method) || d.getScore() > best.get(method).getScore()) {
                best.put(method, d);
            }
            if (!disambiguations.containsKey(d)) {
                disambiguations.put(method, new ArrayList<Disambiguable>());
            }
            disambiguations.get(method).add(d);
        }
    }

    public List<Disambiguable> getDisambiguations(String method) {
        return disambiguations.get(method);
    }

    public Disambiguable getBestDisambiguation(String method) {
        return best.get(method);
    }

    public List<Disambiguable> getNextBest(String method) {
        List<Disambiguable> next = new ArrayList<>();
        Disambiguable best = getBestDisambiguation(method);
        double bestScore = best.getScore();
        for (Disambiguable d : disambiguations.get(method)) {
            if (!d.equals(best) && Math.abs(d.getScore() - bestScore) < threshold) {
                next.add(d);
            }
        }
        return next;
    }

    public Disambiguable getVote() {
        if (voteType.equals(VoteType.MAJORITY)) {
            Map<String, Double> count = new HashMap<>();
            for (String m : getMethods()) {
                List<Disambiguable> ld = disambiguations.get(m);
                if (!count.containsKey(getBestDisambiguation(m).getId())) {
                    count.put(getBestDisambiguation(m).getId(), getBestDisambiguation(m).getScore());
                } else {
                    count.put(getBestDisambiguation(m).getId(), count.get(getBestDisambiguation(m).getId()) + getBestDisambiguation(m).getScore());
                }
            }
            double bestCount = 0;
            String bestSense = "";
            for (String id : count.keySet()) {
                if (count.get(id) > bestCount) {
                    bestCount = count.get(id);
                    bestSense = id;
                }
            }
            voteResult = new DisambiguableSense("", bestSense);
            voteResult.setScore(bestCount);
        }
        return voteResult;
    }

    @Override
    public String getId() {
        return id;
    }

    public Set<String> getMethods() {
        return disambiguations.keySet();
    }

    public String toString(String method) {
        String ret = getId() + ' ' + "00 ";
        if (getBestDisambiguation(method) != null) {
            ret += getBestDisambiguation(method).getId();
        } else {
            return "\r";
        }
        ret += " 1 ";
        if (getBestDisambiguation(method) != null) {
            ret += getBestDisambiguation(method).getScore();
        } else {
            ret += " 1";
        }
        ret += " run_1";
        int i = 2;
        for (Disambiguable n : getNextBest(method)) {
            ret += "\n" + getId() + ' ' + "00 ";
            ret += n.getId() + " " + i + " "; // Rank
            ret += n.getScore();
            ret += " run_1";
            i++;
        }
        return ret;
    }

    public String toStringVote() {
        if (getVote() == null) {
            return "";
        }
        String ret = getId() + ' ' + "00 ";
        ret += getVote().getId();
        ret += " 1 ";
        ret += getVote().getScore();
        ret += " run_1";
        return ret;
    }

    @Override
    public String toString() {
        String ret = "";
        for (String m : getMethods()) {
            ret = getId() + ' ' + "00 ";
            if (getBestDisambiguation(m) != null) {
                ret += getBestDisambiguation(m).getId();
            } else {
                return "\r";
            }
            ret += " 1 ";
            if (getBestDisambiguation(m) != null) {
                ret += getBestDisambiguation(m).getScore();
            } else {
                ret += " 1";
            }
            ret += " run_1\n";
        }
        return ret;
    }

    @Override
    public int compareTo(Ambiguity o) {
        return o.getId().compareTo(getId());
    }
}
