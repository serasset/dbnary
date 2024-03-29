package org.getalp.dbnary.morphology;


import java.util.HashSet;

public class KeepLastInflexionScheme extends InflectionScheme {

  /**
   * Adding a feature to the inflection scheme.
   *
   * In a FunctionalInflexionScheme, the feature is added, but any feature corresponding to the same
   * property that is present immediately before addition is silently removed from the set.
   *
   * This guaranties that at any time, all properties contained in the inflection set are functional
   * (i.e. are associated to only one value).
   *
   * @param morphoSyntacticFeature
   * @return
   */
  @Override
  public boolean add(MorphoSyntacticFeature morphoSyntacticFeature) {
    super.stream().filter(f -> f.property() == morphoSyntacticFeature.property()).findFirst()
        .ifPresent(super::remove);
    return super.add(morphoSyntacticFeature);
  }

}
