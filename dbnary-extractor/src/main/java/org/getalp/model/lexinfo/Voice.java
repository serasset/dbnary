package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Voice extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.voice;
  }

  public static Voice ACTIVE = new Voice() {
    public RDFNode value() {
      return LexinfoOnt.activeVoice;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> active = s -> s.add(ACTIVE);

  public static Voice MIDDLE = new Voice() {
    public RDFNode value() {
      return LexinfoOnt.middleVoice;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> middle = s -> s.add(MIDDLE);

  public static Voice PASSIVE = new Voice() {
    public RDFNode value() {
      return LexinfoOnt.passiveVoice;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> passive = s -> s.add(PASSIVE);

}
