package org.getalp.dbnary.morphology;


import java.util.Formatter;

public class StrictInflexionScheme extends InflectionScheme {

  public static class INCOHERENT_INFLECTION_SCHEME extends RuntimeException {

    public INCOHERENT_INFLECTION_SCHEME() {}

    public INCOHERENT_INFLECTION_SCHEME(String message) {
      super(message);
    }

    public INCOHERENT_INFLECTION_SCHEME(String message, Throwable cause) {
      super(message, cause);
    }

    public INCOHERENT_INFLECTION_SCHEME(Throwable cause) {
      super(cause);
    }

    public INCOHERENT_INFLECTION_SCHEME(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
  }

  /**
   * Adding a feature to the inflection scheme.
   *
   * In a StrictInflexionScheme, the feature is added only if no other feature corresponding to the
   * same property is already present in the Set.
   *
   * This guaranties that any code that will lead to a non functional inflection scheme will break.
   *
   * @param morphoSyntacticFeature
   * @return
   */
  @Override
  public boolean add(MorphoSyntacticFeature morphoSyntacticFeature) {
    super.stream().filter(f -> f.property() == morphoSyntacticFeature.property()).findFirst()
        .ifPresent(f -> {
          throw new INCOHERENT_INFLECTION_SCHEME(
              "feature " + f.toString() + " already present in the scheme while trying to insert "
                  + morphoSyntacticFeature.toString());
        });
    return super.add(morphoSyntacticFeature);
  }

}
