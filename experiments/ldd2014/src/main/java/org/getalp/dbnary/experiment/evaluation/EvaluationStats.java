package org.getalp.dbnary.experiment.evaluation;

public class EvaluationStats {
    private int correct;
    private int expected;
    private int provided;

    {
        correct = 0;
        expected = 0;
        provided = 0;
    }

    public double getPrecision() {
        if(0 == provided){
            return 0;
        } else {
            return (correct) / (provided);
        }
    }

    public double getRecall() {
        if(0 == provided){
            return 0;
        } else {
            return (correct) / (expected);
        }
    }

    public double getF1Score() {
        if(0==getPrecision() + getRecall()){
            return 0;
        } else {
            return (2.0 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
        }
    }

    public <T> void registerAnswer(Comparable<T> exp, Comparable<T> prov) {
        System.err.println("Registered "+exp+"|"+prov);
        if (exp != null) {
            expected++;
        }
        if (prov != null) {
            provided++;
        }
        if(exp != null && prov != null){
            if(exp.equals(prov)){
                correct++;
            }
        }
    }
}
