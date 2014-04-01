package org.getalp.dbnary.experiment.evaluation;

public class EvaluationStats {
    private int correct;
    private int incorrect;
    private int expected;
    private int provided;

    {
        correct = 0;
        incorrect = 0;
        expected = 0;
        provided = 0;
    }

    public double getPrecision() {
        return (correct)/(provided);
    }

    public double getRecall() {
        return (correct)/(expected);
    }

    public double getF1Score() {
        return (2.0 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
    }

    private <T> void registerAnswer(Comparable<T> exp, Comparable<T> prov) {
        if (exp != null) {
            expected++;
        }
        if (prov != null) {
            provided++;
        }
        if(exp != null && prov != null){
            if(exp.equals(prov)){
                correct++;
            } else {
                incorrect++;
            }
        }
    }
}
