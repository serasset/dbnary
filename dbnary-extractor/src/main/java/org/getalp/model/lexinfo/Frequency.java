package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Frequency extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.frequency;
  }

  public static final Frequency RARE = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.rarelyUsed;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> rare = s -> s.add(RARE);

  public static final Frequency COMMON = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.commonlyUsed;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> common = s -> s.add(COMMON);

  public static final Frequency INFREQUENT = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.infrequentlyUsed;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> infrequent = s -> s.add(INFREQUENT);

}
