package org.getalp.atef.model.types;

public class ArithType extends VariableType {
  int value; // Don't know what the value means yet...

  public ArithType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
