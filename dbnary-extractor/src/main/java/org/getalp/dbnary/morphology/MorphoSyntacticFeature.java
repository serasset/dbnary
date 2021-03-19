package org.getalp.dbnary.morphology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

public abstract class MorphoSyntacticFeature implements Comparable<MorphoSyntacticFeature> {
  public abstract Property property();

  public abstract RDFNode value();

  @Override
  public String toString() {
    return new StringBuilder().append(property().toString()).append("->").append(value().toString())
        .toString();
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public int compareTo(MorphoSyntacticFeature o) {
    return this.toString().compareTo(o.toString());
  }
}
