package org.getalp.lexinfo.model;

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

  public static Number SINGULAR = new Number() {
    public RDFNode value() {
      return LexinfoOnt.singular;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> singular = s -> s.add(SINGULAR);

  public static Number PLURAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.plural;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> plural = s -> s.add(PLURAL);

  public static Number DUAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.dual;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> dual = s -> s.add(DUAL);

  public static Number TRIAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.trial;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> trial = s -> s.add(TRIAL);

  public static Number QUADRIAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.quadrial;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> quadrial = s -> s.add(QUADRIAL);

  public static Number MASS_NOUN = new Number() {
    public RDFNode value() {
      return LexinfoOnt.massNoun;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> massNoun = s -> s.add(MASS_NOUN);

  public static Number COLLECTIVE = new Number() {
    public RDFNode value() {
      return LexinfoOnt.collective;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> collective = s -> s.add(COLLECTIVE);

  public static Number PAUCAL = new Number() {
    public RDFNode value() {
      return LexinfoOnt.paucal;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> paucal = s -> s.add(PAUCAL);

  public static Number OTHER = new Number() {
    public RDFNode value() {
      return LexinfoOnt.otherGender;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);

}
