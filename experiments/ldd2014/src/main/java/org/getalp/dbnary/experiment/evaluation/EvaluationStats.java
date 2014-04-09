package org.getalp.dbnary.experiment.evaluation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EvaluationStats {

    private Map<String, Stat> confidenceMap = new HashMap<String, Stat>();
    private Stat currentStat;

    public void reset(String lang) {
        confidenceMap.put(lang, new Stat());
        currentStat = confidenceMap.get(lang);
    }

    public <T> void registerAnswer(Collection<T> expected, Collection<T> provided) {
        currentStat.registerAnswer(expected, provided);
    }

    public void printConfidenceStats(PrintStream out) {
        for (String lang : confidenceMap.keySet()) {
            Stat lstat = confidenceMap.get(lang);
            System.err.println(lstat);
            out.format("%s,%.4f,%.4f,%.4f", lang, lstat.getPrecision(), lstat.getRecall(), lstat.getF1Score());
        }
    }

    private class Stat {
        private int sumPrec;
        private int sumRecall;
        private int nbReq;

        {
            sumPrec = 0;
            sumRecall = 0;
            nbReq = 0;
        }

        public double getPrecision() {
            if (0 == nbReq) {
                return 0;
            } else {
                return ((double) sumPrec) / ((double) nbReq);
            }
        }

        public double getRecall() {
            if (0 == nbReq) {
                return 0;
            } else {
                return ((double) sumRecall) / (double) (nbReq);
            }
        }

        public double getF1Score() {
            if (0 == getPrecision() + getRecall()) {
                return 0;
            } else {
                return (2.0 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
            }
        }

        public <T> void registerAnswer(Collection<T> expected, Collection<T> provided) {
            double numExp = expected.size();
            double numRet = provided.size();
            double numRel = 0;
            for (T c : provided) {
                if (expected.contains(c)) numRel++;
            }


            if(numExp>0){
                sumRecall += numRel / numExp;
            } else {
                sumRecall+=0;
            }
            if(numRet>0){
                sumPrec += numRel / numRet;
            } else {
                sumPrec+=0;
            }

            nbReq++;
        }

        @Override
        public String toString() {
            return "Stat{" +
                    "sumPrec=" + sumPrec +
                    ", sumRecall=" + sumRecall +
                    ", nbReq=" + nbReq +
                    ", P=" + getPrecision() +
                    ", R=" + getRecall() +
                    '}';
        }
    }
}
