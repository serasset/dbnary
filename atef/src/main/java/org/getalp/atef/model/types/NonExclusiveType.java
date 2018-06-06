package org.getalp.atef.model.types;

import java.util.List;

public class NonExclusiveType extends RestrictedType {

  public NonExclusiveType(List<String> possibleValues) {
    super();
    this.addPossibleValues(possibleValues);
  }

}
