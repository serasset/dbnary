package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Person extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.person;
  }

  public static final Person FIRST = new Person() {
    public RDFNode value() {
      return LexinfoOnt.firstPerson;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> first = s -> s.add(FIRST);

  public static final Person SECOND = new Person() {
    public RDFNode value() {
      return LexinfoOnt.secondPerson;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> second = s -> s.add(SECOND);

  public static final Person THIRD = new Person() {
    public RDFNode value() {
      return LexinfoOnt.thirdPerson;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> third = s -> s.add(THIRD);

}
