package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Finiteness extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.finiteness;
  }

  public static Finiteness FINITE = new Finiteness() {
    public RDFNode value() {
      return LexinfoOnt.finite;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> finite = s -> s.add(FINITE);

  public static Finiteness NON_FINITE = new Finiteness() {
    public RDFNode value() {
      return LexinfoOnt.nonFinite;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> nonFinite = s -> s.add(NON_FINITE);

}
