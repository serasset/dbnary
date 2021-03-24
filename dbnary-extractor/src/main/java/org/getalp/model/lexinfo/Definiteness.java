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

  public static Definiteness DEFINITE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.definite;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> definite = s -> s.add(DEFINITE);

  public static Definiteness INDEFINITE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.indefinite;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> indefinite = s -> s.add(INDEFINITE);

  public static Definiteness FULL_ARTICLE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.fullArticle;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> fullArticle = s -> s.add(FULL_ARTICLE);

  public static Definiteness SHORT_ARTICLE = new Definiteness() {
    public RDFNode value() {
      return LexinfoOnt.shortArticle;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> shortArticle = s -> s.add(SHORT_ARTICLE);

}
