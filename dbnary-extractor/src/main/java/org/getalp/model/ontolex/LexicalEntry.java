package org.getalp.model.ontolex;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.rdfutils.URI;

public class LexicalEntry {
  private String name;
  private String wiktionaryPartOfSpeech;
  private int entryNumber = 0;

  private Set<Resource> types = new HashSet<>();
  private Set<Resource> partOfSpeeches = new HashSet<>();

  LexicalForm canonicalForm;
  Set<LexicalForm> otherForms;

  public LexicalEntry() {}

  public LexicalEntry(String name) {
    this.name = name;
  }

  public LexicalEntry(String name, String pos) {
    this(name);
    this.wiktionaryPartOfSpeech = pos;
  }

  public LexicalEntry(String name, String pos, int entryNumber) {
    this(name, pos);
    this.entryNumber = entryNumber;
  }

  public int getEntryNumber() {
    return entryNumber;
  }

  public void setEntryNumber(int entryNumber) {
    this.entryNumber = entryNumber;
  }

  public String getWiktionaryPartOfSpeech() {
    return wiktionaryPartOfSpeech;
  }

  public Resource getLexinfoPartOfSpeech() {
    return partOfSpeeches.stream().findFirst().orElse(null);
  }

  public void setWiktionaryPartOfSpeech(String wiktionaryPartOfSpeech) {
    this.wiktionaryPartOfSpeech = wiktionaryPartOfSpeech;
  }

  public void setPartOfSpeech(Resource pos) {
    this.partOfSpeeches.clear();
    if (pos != null)
      this.partOfSpeeches.add(pos);
  }

  public void addResourceType(Resource type) {
    this.types.add(type);
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

    return lexEntryPrefix + name + "__" + wiktionaryPartOfSpeech + "__" + entryNumber;
  }


}
