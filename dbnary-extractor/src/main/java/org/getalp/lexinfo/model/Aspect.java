package org.getalp.lexinfo.model;

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

  public static Aspect CESSATIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.cessative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> cessative = s -> s.add(CESSATIVE);

  public static Aspect IMPERFECTIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.imperfective;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> imperfective = s -> s.add(IMPERFECTIVE);

  public static Aspect INCHOATIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.inchoative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> inchoative = s -> s.add(INCHOATIVE);

  public static Aspect PERFECTIVE = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.perfective;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> perfective = s -> s.add(PERFECTIVE);

  public static Aspect UNACCOMPLISHED = new Aspect() {
    public RDFNode value() {
      return LexinfoOnt.unaccomplished;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> unaccomplished = s -> s.add(UNACCOMPLISHED);
}
