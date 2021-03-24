package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Degree extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.degree;
  }

  public static Degree COMPARATIVE = new Degree() {
    public RDFNode value() {
      return LexinfoOnt.comparative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> comparative = s -> s.add(COMPARATIVE);

  public static Degree POSITIVE = new Degree() {
    public RDFNode value() {
      return LexinfoOnt.positive;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> positive = s -> s.add(POSITIVE);

  public static Degree SUPERLATIVE = new Degree() {
    public RDFNode value() {
      return LexinfoOnt.superlative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> superlative = s -> s.add(SUPERLATIVE);

}
