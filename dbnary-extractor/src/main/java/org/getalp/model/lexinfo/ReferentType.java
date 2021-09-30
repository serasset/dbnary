package org.getalp.model.lexinfo;

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

  public static final ReferentType PERSONAL = new ReferentType() {
    public RDFNode value() {
      return LexinfoOnt.personal;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> personal = s -> s.add(PERSONAL);

  public static final ReferentType POSSESSIVE = new ReferentType() {
    public RDFNode value() {
      return LexinfoOnt.possessive;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> possessive = s -> s.add(POSSESSIVE);

}
