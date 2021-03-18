package org.getalp.lexinfo.model.morphoSyntax;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Animacy implements MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.animacy;
  }

  public static Animacy ANIMATE = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.animate;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> animate = s -> s.add(ANIMATE);

  public static Animacy INANIMATE = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.inanimate;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> inanimate = s -> s.add(INANIMATE);

  public static Animacy OTHER = new Animacy() {
    public RDFNode value() {
      return LexinfoOnt.otherAnimacy;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);
}
