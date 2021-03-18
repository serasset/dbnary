package org.getalp.lexinfo.model;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class ReferentType extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.referentType;
  }

  public static ReferentType PERSONAL = new ReferentType() {
    public RDFNode value() {
      return LexinfoOnt.personal;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> personal = s -> s.add(PERSONAL);

  public static ReferentType POSSESSIVE = new ReferentType() {
    public RDFNode value() {
      return LexinfoOnt.possessive;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> possessive = s -> s.add(POSSESSIVE);

}
