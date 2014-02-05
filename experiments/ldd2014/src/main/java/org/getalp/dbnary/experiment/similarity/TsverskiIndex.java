package org.getalp.dbnary.experiment.similarity;

import com.wcohen.ss.AbstractStringDistance;
import org.getalp.dbnary.experiment.encoding.CodePointWrapper;
import org.getalp.dbnary.experiment.segmentation.Segmenter;
import org.getalp.dbnary.experiment.segmentation.SpaceSegmenter;

import java.util.List;

public class TsverskiIndex implements SimilarityMeasure {
    private Segmenter segmenter;

    private double alpha;
    private double beta;
    private boolean fuzzyMatching;
    private AbstractStringDistance distance;
    private boolean lcss;

    public TsverskiIndex(double alpha, double beta) {
        segmenter = new SpaceSegmenter();
        lcss = false;
        this.alpha = alpha;
        this.beta = beta;
        fuzzyMatching = false;
    }

    public TsverskiIndex(double alpha, double beta, boolean fuzzyMatching, AbstractStringDistance distance) {
        this.distance = distance;
        lcss = false;
        segmenter = new SpaceSegmenter();
        this.alpha = alpha;
        this.beta = beta;
        this.fuzzyMatching = fuzzyMatching;
    }

    public TsverskiIndex(double alpha, double beta, boolean fuzzyMatching) {
        lcss = true;
        segmenter = new SpaceSegmenter();
        this.alpha = alpha;
        this.beta = beta;
        this.fuzzyMatching = fuzzyMatching;
    }

    public TsverskiIndex(Segmenter segmenter, double alpha, double beta) {
        this.segmenter = segmenter;
        this.alpha = alpha;
        this.beta = beta;
        fuzzyMatching = false;
    }

    public TsverskiIndex(Segmenter segmenter, double alpha, double beta, boolean fuzzyMatching) {
        this.segmenter = segmenter;
        this.alpha = alpha;
        this.beta = beta;
        this.fuzzyMatching = fuzzyMatching;
        if (!fuzzyMatching) {
            lcss = false;
        }
    }

    public static int longestSubString(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl][sl];
        CodePointWrapper cpFirst = new CodePointWrapper(first);
        int i = 0;
        for (int cpi : cpFirst) {
            CodePointWrapper cpSecond = new CodePointWrapper(second);
            int j = 0;
            for (int cpj : cpSecond) {
                if (cpi == cpj) {
                    if (i == 0 || j == 0) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
                j++;
            }
            i++;
        }
        return maxLen;
    }

    @Override
    public double compute(String a, String b) {
        return compute(segmenter.segment(a), segmenter.segment(b));
    }

    public double compute(List<String> a, List<String> b) {
        double overlap;
        if (!fuzzyMatching) {
            overlap = computeOverlap(a, b);
        } else {
            overlap = computeFuzzyOverlap(a, b);
        }
        double diffA = a.size() - overlap;
        double diffB = b.size() - overlap;
        return overlap / (overlap + diffA * alpha + diffB * beta);
    }

    private double computeOverlap(List<String> a, List<String> b) {
        int size = Math.min(a.size(), b.size());
        double overlap = 0;
        for (int i = 0; i < size && a.get(i).contains(b.get(i)); i++) {
            overlap += 1;
        }
        return overlap;
    }

    private double computeFuzzyOverlap(List<String> la, List<String> lb) {
        double overlap = 0;
        for (String a : la) {
            for (String b : lb) {

                double score = 0;
                double lcss = longestSubString(a, b);
                double md = Math.max(Math.abs(lcss / a.length()), Math.abs(lcss / b.length()));
                if (!this.lcss) {
                    score = distance.score(distance.prepare(a), distance.prepare(b));
                } else {
                    score = md;
                }
                if (score > 0.999 || score < 1.0 && lcss >= 3) {

                    if (!this.lcss) {
                        overlap += score + (1 - score) * (md - 0.5);
                    } else {
                        overlap += md;
                    }
                }
            }
        }

        return overlap;
    }
}
