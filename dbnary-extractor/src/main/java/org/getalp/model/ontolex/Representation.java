package org.getalp.model.ontolex;

import org.apache.jena.rdf.model.Resource;

public abstract class Representation {
  protected String value;
  protected String language;

  public abstract Resource attachTo(Resource lexForm);

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  @Override
  public String toString() {
    return "\"" + value + "\"@" + language;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

}
