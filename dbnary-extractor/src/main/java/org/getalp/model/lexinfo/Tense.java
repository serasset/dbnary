package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Tense extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.tense;
  }

  public static final Tense PRESENT = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.present;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> present = s -> s.add(PRESENT);

  public static final Tense PAST = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.past;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> past = s -> s.add(PAST);

  public static final Tense PRETERITE = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.preterite;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> preterite = s -> s.add(PRETERITE);

  public static final Tense IMPERFECT = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.imperfect;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> imperfect = s -> s.add(IMPERFECT);

  public static final Tense FUTURE = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.future;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> future = s -> s.add(FUTURE);

}
