package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Aspect extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.aspect;
  }

  public static final Aspect CESSATIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.cessative;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> cessative = s -> s.add(CESSATIVE);

  public static final Aspect IMPERFECTIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.imperfective;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> imperfective = s -> s.add(IMPERFECTIVE);

  public static final Aspect INCHOATIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.inchoative;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> inchoative = s -> s.add(INCHOATIVE);

  public static final Aspect PERFECTIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.perfective;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> perfective = s -> s.add(PERFECTIVE);

  public static final Aspect UNACCOMPLISHED = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.unaccomplished;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> unaccomplished =
      s -> s.add(UNACCOMPLISHED);
}
