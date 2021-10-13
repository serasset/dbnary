package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Animacy extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.animacy;
  }

  public static final Animacy ANIMATE = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.animate;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> animate = s -> s.add(ANIMATE);

  public static final Animacy INANIMATE = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.inanimate;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> inanimate = s -> s.add(INANIMATE);

  public static final Animacy OTHER = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.otherAnimacy;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);
}
