package org.getalp.atef.model.types;

import java.util.ArrayList;
import java.util.List;

public abstract class RestrictedType extends VariableType {
  ArrayList<String> possibleValues = new ArrayList<>();


  public ArrayList<String> getPossibleValues() {
    return possibleValues;
  }

  public void addPossibleValue(String value) {
    this.possibleValues.add(value);
  }

  public void addPossibleValues(List<String> values) {
    this.possibleValues.addAll(values);
  }
}
