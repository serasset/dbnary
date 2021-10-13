package org.getalp.model.lexinfo;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Definiteness extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.definiteness;
  }

  public static final Definiteness DEFINITE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.definite;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> definite = s -> s.add(DEFINITE);

  public static final Definiteness INDEFINITE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.indefinite;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> indefinite = s -> s.add(INDEFINITE);

  public static final Definiteness FULL_ARTICLE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.fullArticle;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> fullArticle = s -> s.add(FULL_ARTICLE);

  public static final Definiteness SHORT_ARTICLE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.shortArticle;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> shortArticle =
      s -> s.add(SHORT_ARTICLE);

}
