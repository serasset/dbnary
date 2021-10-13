package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Number extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.number;
  }

  public static final Number SINGULAR = new Number() {
    public RDFNode value() {
      return LexinfoOnt.singular;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> singular = s -> s.add(SINGULAR);

  public static final Number PLURAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.plural;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> plural = s -> s.add(PLURAL);

  public static final Number DUAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.dual;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> dual = s -> s.add(DUAL);

  public static final Number TRIAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.trial;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> trial = s -> s.add(TRIAL);

  public static final Number QUADRIAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.quadrial;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> quadrial = s -> s.add(QUADRIAL);

  public static final Number MASS_NOUN = new Number() {
    public RDFNode value() {
      return LexinfoOnt.massNoun;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> massNoun = s -> s.add(MASS_NOUN);

  public static final Number COLLECTIVE = new Number() {
    public RDFNode value() {
      return LexinfoOnt.collective;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> collective = s -> s.add(COLLECTIVE);

  public static final Number PAUCAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.paucal;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> paucal = s -> s.add(PAUCAL);

  public static final Number OTHER = new Number() {
    public RDFNode value() {
      return LexinfoOnt.otherGender;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);

}
