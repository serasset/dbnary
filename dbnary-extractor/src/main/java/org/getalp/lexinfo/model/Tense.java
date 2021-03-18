package org.getalp.lexinfo.model;

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

  public static Tense PRESENT = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.present;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> present = s -> s.add(PRESENT);

  public static Tense PAST = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.past;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> past = s -> s.add(PAST);

  public static Tense PRETERITE = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.preterite;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> preterite = s -> s.add(PRETERITE);

  public static Tense IMPERFECT = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.imperfect;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> imperfect = s -> s.add(IMPERFECT);

  public static Tense FUTURE = new Tense() {
    public RDFNode value() {
      return LexinfoOnt.future;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> future = s -> s.add(FUTURE);

}
