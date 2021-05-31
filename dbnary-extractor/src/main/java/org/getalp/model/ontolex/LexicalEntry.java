package org.getalp.model.ontolex;

import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.rdfutils.URI;

public class LexicalEntry {
  private String name;
  private String partOfSpeech;
  private int entryNumber;

  private Set<Resource> types;
  private Set<Resource> partOfSpeeches;

  LexicalForm canonicalForm;
  Set<LexicalForm> otherForms;

  public LexicalEntry() {}

  public LexicalEntry(LexicalForm canonicalForm) {
    this.canonicalForm = canonicalForm;
  }

  public Resource attachTo(Resource page) {
    Resource lexEntry =
        page.getModel().createResource(computeResourceName(page), OntolexOnt.LexicalEntry);
    canonicalForm.attachTo(lexEntry);
    // TODO...
    page.getModel().add(page, DBnaryOnt.describes, lexEntry);
    return page;
  }

  private String computeResourceName(Resource page) {
    String lexEntryPrefix = URI.getNameSpace(page);

    return lexEntryPrefix + canonicalForm + "__" + partOfSpeech + "__" + entryNumber;
  }


}
