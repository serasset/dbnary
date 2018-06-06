package org.getalp.atef.model.types;

import java.util.List;

public class ExclusiveType extends RestrictedType {

  public ExclusiveType(List<String> possibleValues) {
    super();
    this.addPossibleValues(possibleValues);
  }

}
