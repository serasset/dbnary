package org.getalp.atef.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinguisticDecoration {
  String name;
  List<Variable> exclusiveVariables = new ArrayList<>();
  List<Variable> nonExclusiveVariables = new ArrayList<>();
  List<Variable> arithVariables = new ArrayList<>();
  List<Variable> stringVariables = new ArrayList<>();
  Map<String, Variable> name2var = new HashMap<>();

  public LinguisticDecoration(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public List<Variable> getExclusiveVariables() {
    return exclusiveVariables;
  }

  public List<Variable> getNonExclusiveVariables() {
    return nonExclusiveVariables;
  }

  public List<Variable> getArithVariables() {
    return arithVariables;
  }

  public List<Variable> getStringVariables() {
    return stringVariables;
  }

  public void addExclusiveVariable(Variable var) {
    name2var.put(var.name, var);
    this.exclusiveVariables.add(var);
  }

  public void addNonExclusiveVariable(Variable var) {
    name2var.put(var.name, var);
    this.nonExclusiveVariables.add(var);
  }

  public void addArithVariable(Variable var) {
    name2var.put(var.name, var);
    this.arithVariables.add(var);
  }

  public void addStringVariable(Variable var) {
    name2var.put(var.name, var);
    this.stringVariables.add(var);
  }

}
