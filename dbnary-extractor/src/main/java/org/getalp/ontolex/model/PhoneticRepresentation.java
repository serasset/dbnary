package org.getalp.ontolex.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.OntolexOnt;

public class PhoneticRepresentation extends Representation {

  public PhoneticRepresentation(String value, String language) {
    this.value = value;
    this.language = language;
  }

  public Resource attachTo(Resource lexForm) {
    Model box = lexForm.getModel();
    box.add(lexForm, OntolexOnt.phoneticRep, box.createLiteral(value, language + "-fonipa"));
    return lexForm;
  }

}
