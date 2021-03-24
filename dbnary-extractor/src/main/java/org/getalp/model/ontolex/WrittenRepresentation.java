package org.getalp.model.ontolex;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.OntolexOnt;

public class WrittenRepresentation extends Representation {

  public WrittenRepresentation(String value, String language) {
    this.value = value;
    this.language = language;
  }

  public Resource attachTo(Resource lexForm) {
    Model box = lexForm.getModel();
    box.add(lexForm, OntolexOnt.writtenRep, box.createLiteral(value, language));
    return lexForm;
  }

  @Override
  public String toString() {
    return "WrittenRepresentation{" + super.toString() + "}";
  }
}
