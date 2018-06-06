package org.getalp.atef.model;

import org.getalp.atef.model.types.VariableType;

public class Variable {
  String name;
  VariableType type;

  public Variable(String name) {
    this.name = name;
  }

  public Variable(String name, VariableType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public VariableType getType() {
    return type;
  }

  public void setType(VariableType type) {
    this.type = type;
  }
}
