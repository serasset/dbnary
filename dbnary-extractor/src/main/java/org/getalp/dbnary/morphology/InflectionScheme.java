package org.getalp.dbnary.morphology;

import java.io.Serial;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public abstract class InflectionScheme extends TreeSet<MorphoSyntacticFeature> {

  @Serial
  private static final long serialVersionUID = 9079566863179637469L;

  public Resource attachTo(Resource lexForm) {
    Model box = lexForm.getModel();
    this.forEach(f -> box.add(lexForm, f.property(), f.value()));
    return lexForm;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("[").append(this.stream().map(MorphoSyntacticFeature::toString).collect(Collectors.joining("|"))).append("]").toString();
  }


}
