package org.getalp.dbnary;

import static org.getalp.dbnary.ExtractionFeature.*;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class ExtractionFeature2 {
  public final ExtractionFeature feature;
  private final boolean exolex;

  public ExtractionFeature2(ExtractionFeature feature, boolean exolex) {
    this.feature = feature;
    this.exolex = exolex;
  }

  public ExtractionFeature2(ExtractionFeature feature) {
    this(feature, false);
  }

  public boolean isHDT() {
    return feature.equals(HDT);
  }

  public boolean isExolex() {
    return exolex;
  }

  public String getName() {
    return (isExolex() ? "exolex_" : "") + feature.toString();
  }

  public static final ExtractionFeature2 MAIN_ENDOLEX = new ExtractionFeature2(MAIN) {};
  public static final ExtractionFeature2 MAIN_EXOLEX = new ExtractionFeature2(MAIN, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableMain = s -> s.add(MAIN_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableMainExolex = s -> s.add(MAIN_EXOLEX);


  public static final ExtractionFeature2 MORPHOLOGY_ENDOLEX = new ExtractionFeature2(MORPHOLOGY) {};
  public static final ExtractionFeature2 MORPHOLOGY_EXOLEX =
      new ExtractionFeature2(MORPHOLOGY, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableMorphology =
      s -> s.add(MORPHOLOGY_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableMorphologyExolex =
      s -> s.add(MORPHOLOGY_EXOLEX);

  public static final ExtractionFeature2 ETYMOLOGY_ENDOLEX = new ExtractionFeature2(ETYMOLOGY) {};
  public static final ExtractionFeature2 ETYMOLOGY_EXOLEX =
      new ExtractionFeature2(ETYMOLOGY, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableEtymology =
      s -> s.add(ETYMOLOGY_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableEtymologyExolex =
      s -> s.add(ETYMOLOGY_EXOLEX);

  public static final ExtractionFeature2 LIME_ENDOLEX = new ExtractionFeature2(LIME) {};
  public static final ExtractionFeature2 LIME_EXOLEX = new ExtractionFeature2(LIME, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableLime = s -> s.add(LIME_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableLimeExolex = s -> s.add(LIME_EXOLEX);

  public static final ExtractionFeature2 ENHANCEMENT_ENDOLEX =
      new ExtractionFeature2(ENHANCEMENT) {};
  public static final ExtractionFeature2 ENHANCEMENT_EXOLEX =
      new ExtractionFeature2(ENHANCEMENT, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableEnhancement =
      s -> s.add(ENHANCEMENT_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableEnhancementExolex =
      s -> s.add(ENHANCEMENT_EXOLEX);

  public static final ExtractionFeature2 STATISTICS_ENDOLEX = new ExtractionFeature2(STATISTICS) {};
  public static final ExtractionFeature2 STATISTICS_EXOLEX =
      new ExtractionFeature2(STATISTICS, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableStatistics =
      s -> s.add(STATISTICS_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableStatisticsExolex =
      s -> s.add(STATISTICS_EXOLEX);

  public static final ExtractionFeature2 HDT_ENDOLEX = new ExtractionFeature2(HDT) {};
  public static final ExtractionFeature2 HDT_EXOLEX = new ExtractionFeature2(HDT, true) {};
  public static final Consumer<Set<ExtractionFeature2>> enableHdt = s -> s.add(HDT_ENDOLEX);
  public static final Consumer<Set<ExtractionFeature2>> enableHdtExolex = s -> s.add(HDT_EXOLEX);

}
