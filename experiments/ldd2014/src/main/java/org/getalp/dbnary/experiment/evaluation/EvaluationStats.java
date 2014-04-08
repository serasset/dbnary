package org.getalp.dbnary.experiment.evaluation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EvaluationStats {
	
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
	        if(0 == nbReq){
	            return 0;
	        } else {
	            return (sumPrec) / (nbReq);
	        }
	    }

	    public double getRecall() {
	        if(0 == nbReq){
	            return 0;
	        } else {
	            return (sumRecall) / (nbReq);
	        }
	    }

	    public double getF1Score() {
	        if(0==getPrecision() + getRecall()){
	            return 0;
	        } else {
	            return (2.0 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
	        }
	    }

	    public <T> void registerAnswer(Collection<T> expected, Collection<T> provided) {
	        System.err.println("Registered "+expected+"|"+provided);
	        double numExp = expected.size();
	        double numRet = provided.size();
	        double numRel = 0;
	        for (T c : provided) {
				if (expected.contains(c)) numRel++;
			}
	        
	        sumRecall += numRel/numExp;
	        sumPrec += numRel/numRet;
	        nbReq++;
	    }

	}


	private Map<String,Stat> confidenceMap = new HashMap<String, Stat>();
	private Stat currentStat;
	
    
	public void reset(String lang) {
		confidenceMap.put(lang, new Stat());
		currentStat = confidenceMap.get(lang);
	}
	
    public <T> void registerAnswer(Collection<T> expected, Collection<T> provided) {
        currentStat.registerAnswer(expected, provided);
    }
    
	public void printConfidenceStats(PrintStream out) {
        for(String lang: confidenceMap.keySet()){
            Stat lstat = confidenceMap.get(lang);
            out.format("%s,%.2f,%.2f,.2f",lang+lstat.getPrecision(),lstat.getRecall(),lstat.getF1Score());
        }
	}
}
