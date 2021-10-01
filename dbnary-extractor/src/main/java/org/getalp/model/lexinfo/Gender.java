package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Gender extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.gender;
  }

  public static final Gender MASCULINE = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.masculine;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> masculine = s -> s.add(MASCULINE);

  public static final Gender FEMININE = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.feminine;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> feminine = s -> s.add(FEMININE);

  public static final Gender NEUTER = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.neuter;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> neuter = s -> s.add(NEUTER);

  public static final Gender COMMON = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.commonGender;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> common = s -> s.add(COMMON);

  public static final Gender OTHER = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.otherGender;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);
}
