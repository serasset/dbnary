package org.getalp.dbnary.morphology;


public class KeepFirstInflexionScheme extends InflectionScheme {

  /**
   * Adding a feature to the inflection scheme.
   *
   * In a KeepFirstInflexionScheme, the feature is added only if no feature corresponding to the
   * same property is present immediately before addition.
   *
   * This guaranties that at any time, all properties contained in the inflection set are functional
   * (i.e. are associated to only one value).
   *
   * @param morphoSyntacticFeature
   * @return
   */
  @Override
  public boolean add(MorphoSyntacticFeature morphoSyntacticFeature) {
    return super.stream().noneMatch(f -> f.property() == morphoSyntacticFeature.property())
        && super.add(morphoSyntacticFeature);
  }

}
