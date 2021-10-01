package org.getalp.model.lexinfo;

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

  public static final Mood INDICATIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.indicative;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> indicative = s -> s.add(INDICATIVE);

  public static final Mood INFINITIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.infinitive;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> infinitive = s -> s.add(INFINITIVE);

  public static final Mood IMPERATIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.imperative;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> imperative = s -> s.add(IMPERATIVE);

  public static final Mood PARTICIPLE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.participle;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> participle = s -> s.add(PARTICIPLE);

  public static final Mood CONDITIONAL = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.conditional;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> conditional = s -> s.add(CONDITIONAL);

  public static final Mood SUBJUNCTIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.subjunctive;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> subjunctive = s -> s.add(SUBJUNCTIVE);

  public static final Mood GERUNDIVE = new Mood() {
    public RDFNode value() {
      return LexinfoOnt.gerundive;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> gerundive = s -> s.add(GERUNDIVE);

}
