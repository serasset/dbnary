package org.getalp.lexinfo.model;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Mood extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.verbFormMood;
  }

  public static Mood INDICATIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.indicative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> indicative = s -> s.add(INDICATIVE);

  public static Mood INFINITIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.infinitive;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> infinitive = s -> s.add(INFINITIVE);

  public static Mood IMPERATIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.imperative;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> imperative = s -> s.add(IMPERATIVE);

  public static Mood PARTICIPLE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.participle;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> participle = s -> s.add(PARTICIPLE);

  public static Mood CONDITIONAL = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.conditional;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> conditional = s -> s.add(CONDITIONAL);

  public static Mood SUBJUNCTIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.subjunctive;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> subjunctive = s -> s.add(SUBJUNCTIVE);

  public static Mood GERUNDIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.gerundive;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> gerundive = s -> s.add(GERUNDIVE);

}
