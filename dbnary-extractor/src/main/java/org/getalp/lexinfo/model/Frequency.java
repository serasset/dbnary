package org.getalp.lexinfo.model;

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

  public static Frequency RARE = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.rarelyUsed;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> rare = s -> s.add(RARE);

  public static Frequency COMMON = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.commonlyUsed;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> common = s -> s.add(COMMON);

  public static Frequency INFREQUENT = new Frequency() {
    public RDFNode value() {
      return LexinfoOnt.infrequentlyUsed;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> infrequent = s -> s.add(INFREQUENT);

}
