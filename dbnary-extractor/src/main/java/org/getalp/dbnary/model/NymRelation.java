package org.getalp.dbnary.model;

import org.apache.jena.rdf.model.Property;
import org.getalp.dbnary.DBnaryOnt;

public enum NymRelation {
  SYNONYM(DBnaryOnt.synonym), APPROXIMATE_SYNONYM(DBnaryOnt.approximateSynonym), ANTONYM(
      DBnaryOnt.antonym), HYPERNYM(DBnaryOnt.hypernym), HYPONYM(DBnaryOnt.hyponym), MERONYM(
          DBnaryOnt.meronym), HOLONYM(DBnaryOnt.holonym), TROPONYM(DBnaryOnt.troponym);

  private Property property;

  NymRelation(Property p) {
    this.property = p;
  }

  public Property getProperty() {
    return property;
  }

  public static NymRelation of(String nymShortName) {
    switch (nymShortName) {
      case "syn":
        return SYNONYM;
      case "qsyn":
        return APPROXIMATE_SYNONYM;
      case "ant":
        return ANTONYM;
      case "hyper":
        return HYPERNYM;
      case "hypo":
        return HYPONYM;
      case "mero":
        return MERONYM;
      case "holo":
        return HOLONYM;
      case "tropo":
        return TROPONYM;
      default:
        return null;
    }
  }
}
