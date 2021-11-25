package org.getalp.model.ontolex;

import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.rdfutils.URI;
import org.getalp.model.dbnary.AcceptTranslation;

public class LexicalSense implements AcceptTranslation {
  private int senseNumber;

  String definition;
  Set<String> examples;

  public LexicalSense() {}

  public LexicalSense(int senseNumber) {
    this.senseNumber = senseNumber;
  }

  public Resource attachTo(Resource lexEntry) {
    Resource sense =
        lexEntry.getModel().createResource(computeResourceName(lexEntry), OntolexOnt.LexicalEntry);
    // TODO...
    lexEntry.getModel().add(lexEntry, OntolexOnt.sense, sense);
    return lexEntry;
  }

  private String computeResourceName(Resource lexEntry) {
    String lexEntryPrefix = URI.getNameSpace(lexEntry);
    String lexEntryName = URI.getLocalName(lexEntry);

    return lexEntryPrefix + "__ws_" + senseNumber + "__" + lexEntryName;
  }

}
