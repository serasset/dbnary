package org.getalp.dbnary.experiment.disambiguation.translations;


import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;

import java.util.*;

public class TranslationAmbiguity implements Ambiguity {

    private String gloss;
    private Map<String, List<Disambiguable>> disambiguations;
    private Map<String, Disambiguable> best;
    private String id;

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

    public Disambiguable getBestDisambiguations(String method) {
        return best.get(method);
    }

    public Disambiguable getVote() {
        if (voteType.equals(VoteType.MAJORITY)) {
            Map<String, Double> count = new HashMap<>();
            for (String m : getMethods()) {
                List<Disambiguable> ld = disambiguations.get(m);
                if (!count.containsKey(getBestDisambiguations(m).getId())) {
                    count.put(getBestDisambiguations(m).getId(), getBestDisambiguations(m).getScore());
                } else {
                    count.put(getBestDisambiguations(m).getId(), count.get(getBestDisambiguations(m).getId()) + getBestDisambiguations(m).getScore());
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
        if (getBestDisambiguations(method) != null) {
            ret += getBestDisambiguations(method).getId();
        } else {
            return "\r";
        }
        ret += " 1 ";
        if (getBestDisambiguations(method) != null) {
            ret += getBestDisambiguations(method).getScore();
        } else {
            ret += " 1";
        }
        ret += " run_1";
        return ret;
    }

    public String toStringVote() {
        String ret = getId() + ' ' + "00 ";
        if (getVote() != null) {
            ret += getVote().getId();
        } else {
            return "\r";
        }
        ret += " 1 ";
        if (getVote() != null) {
            ret += getVote().getScore();
        } else {
            ret += " 1";
        }
        ret += " run_1";
        return ret;
    }

    @Override
    public String toString() {
        String ret = "";
        for (String m : getMethods()) {
            ret = getId() + ' ' + "00 ";
            if (getBestDisambiguations(m) != null) {
                ret += getBestDisambiguations(m).getId();
            } else {
                return "\r";
            }
            ret += " 1 ";
            if (getBestDisambiguations(m) != null) {
                ret += getBestDisambiguations(m).getScore();
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
