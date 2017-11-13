package org.getalp.dbnary.enhancer.evaluation;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class EvaluationStats {

  private Map<String, Stat> confidenceMap = new TreeMap<String, Stat>();
  private Stat currentStat;

  public static String getHeaders() {
    return "Similarity Precision,Similarity Recall, Similarity F1, Random Precision, Random Recall";
  }

  public void reset(String lang) {
    confidenceMap.put(lang, new Stat());
    currentStat = confidenceMap.get(lang);
  }

  public <T> void registerAnswer(Collection<T> expected, Collection<T> provided,
      int nbAlternatives) {
    currentStat.registerAnswer(expected, provided, nbAlternatives);
  }

  public void printStat(String lang, PrintStream out) {
    this.printStat(lang, new PrintWriter(out));
  }

  public void printStat(String lang, PrintWriter out) {
    Stat lstat = confidenceMap.get(lang);
    out.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f", lang, lstat.getPrecision(), lstat.getRecall(),
        lstat.getF1Score(), lstat.getRandomPrec(), lstat.getRandomRecall());
  }

  public void printConfidenceStats(PrintStream out) {
    out.println("Language," + getHeaders());
    for (String lang : confidenceMap.keySet()) {
      // System.err.println(lstat);
      printStat(lang, out);
      out.println();
    }
  }

  private class Stat {
    private double sumPrec;
    private double sumRecall;
    private int nbReq;
    private double randRecall;
    private int randPrec;

    {
      sumPrec = 0;
      sumRecall = 0;
      nbReq = 0;
      randRecall = 0;
      randPrec = 0;
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

    public <T> void registerAnswer(Collection<T> expected, Collection<T> provided,
        int nbAlternatives) {
      double numExp = expected.size();
      double numRet = provided.size();
      double numRel = 0;
      for (T c : provided) {
        if (expected.contains(c))
          numRel++;
      }


      if (numExp > 0) {
        sumRecall += numRel / numExp;
      }
      if (numRet > 0) {
        sumPrec += numRel / numRet;
      }
      if (nbAlternatives > 0 && numExp > 0) {
        randRecall += 1 / (nbAlternatives);
      }
      if (nbAlternatives > 0) {
        randPrec += numExp / (nbAlternatives);
      }

      nbReq++;
    }

    @Override
    public String toString() {
      return "Stat{" + "sumPrec=" + sumPrec + ", sumRecall=" + sumRecall + ", nbReq=" + nbReq
          + ", P=" + getPrecision() + ", R=" + getRecall() + ", randPrec=" + getRandomPrec()
          + ", randRecall=" + getRandomRecall() + '}';
    }

    private double getRandomRecall() {
      if (0 == nbReq) {
        return 0;
      } else {
        return ((double) randPrec) / ((double) nbReq);
      }
    }

    private double getRandomPrec() {
      if (0 == nbReq) {
        return 0;
      } else {
        return ((double) randRecall) / (double) (nbReq);
      }
    }
  }
}
