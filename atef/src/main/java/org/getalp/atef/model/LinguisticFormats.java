package org.getalp.atef.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LinguisticFormats {
  static class Format {
    String name;
    Map<Variable, ArrayList<String>> varval = new HashMap<>();

    public Format(String name) {
      this.name = name;
    }
  }

  String name;
  ArrayList<Format> formats = new ArrayList<>();

}
