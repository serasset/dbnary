package org.getalp.dbnary;

public class StructuredGloss {

  protected String senseNumber = null;
  protected String gloss = null;

  public StructuredGloss() {
    super();
  }

  public StructuredGloss(String senseNumber, String gloss) {
    if (null != gloss && (gloss = gloss.trim()).isEmpty()) {
      gloss = null;
    }
    this.senseNumber = senseNumber;
    this.gloss = gloss;
  }

  public String getSenseNumber() {
    return senseNumber;
  }

  public void setSenseNumber(String senseNumber) {
    this.senseNumber = senseNumber;
  }

  public String getGloss() {
    return gloss;
  }

  public void setGloss(String gloss) {
    this.gloss = gloss;
  }
}
