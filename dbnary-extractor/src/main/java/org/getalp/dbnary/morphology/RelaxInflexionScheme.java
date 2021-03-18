package org.getalp.dbnary.morphology;


public class RelaxInflexionScheme extends InflectionScheme {

  /**
   * Adding a feature to the inflection scheme.
   *
   * In a RelaxInflexionScheme, the feature is added, even if this leads to a non functional
   * property.
   *
   * @param morphoSyntacticFeature
   * @return
   */
  @Override
  public boolean add(MorphoSyntacticFeature morphoSyntacticFeature) {
    return super.add(morphoSyntacticFeature);
  }

}
