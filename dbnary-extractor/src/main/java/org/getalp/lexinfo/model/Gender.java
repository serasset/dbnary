package org.getalp.lexinfo.model.morphoSyntax;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Gender implements MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.gender;
  }

  public static Gender MASCULINE = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.masculine;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> masculine = s -> s.add(MASCULINE);

  public static Gender FEMININE = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.feminine;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> feminine = s -> s.add(FEMININE);

  public static Gender NEUTER = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.neuter;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> neuter = s -> s.add(NEUTER);

  public static Gender COMMON = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.commonGender;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> common = s -> s.add(COMMON);

  public static Gender OTHER = new Gender() {
    public RDFNode value() {
      return LexinfoOnt.otherGender;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> other = s -> s.add(OTHER);
}
