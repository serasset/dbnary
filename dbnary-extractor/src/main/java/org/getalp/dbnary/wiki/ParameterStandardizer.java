package org.getalp.dbnary.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParameterStandardizer {

  private final Map<String, String> aliasesDeclarations = new HashMap<>();
  private Pattern normalizerPattern = null;

  public ParameterStandardizer(Map<String, String> patternMapping) {
    // Add all the canonical parameters to the aliasesDeclarations
    patternMapping.values().forEach(value -> addAlias(value, value));
    patternMapping.forEach(this::addAlias);
  }

  private void addAlias(String alias, String target) {
    aliasesDeclarations.put(alias, target);
  }

  public Pattern getParameterPattern() {
    if (normalizerPattern == null) {// invert the aliasesDeclaration map
      Map<String, List<String>> params = aliasesDeclarations.entrySet().stream().collect(Collectors
          .groupingBy(Entry::getValue, Collectors.mapping(Entry::getKey, Collectors.toList())));
      // Then create the regex for each parameter and aggregate them as a set of named patterns
      StringBuilder pattern = new StringBuilder();
      params.forEach((target, aliases) -> pattern.append("(?<").append(target).append(">")
          .append(String.join("|", aliases)).append(")(?<").append(target).append("N>\\d*)|"));
      if (pattern.length() != 0) {
        pattern.deleteCharAt(pattern.length() - 1);
      }
      normalizerPattern = Pattern.compile(pattern.toString());
    }
    return normalizerPattern;
  }

  private Set<String> canonicalParameters = null;

  public Set<String> getCanonicalParameters() {
    if (null == canonicalParameters) {
      canonicalParameters = aliasesDeclarations.entrySet().stream()
          .filter(entry -> entry.getKey().equals(entry.getValue())).map(Entry::getKey)
          .collect(Collectors.toSet());
    }
    return canonicalParameters;
  }


  private String normalizeParameterName(String parameter) {
    Matcher matcher = getParameterPattern().matcher(parameter);
    if (matcher.matches()) {
      for (String param : getCanonicalParameters()) {
        if (matcher.group(param) != null) {
          return param + matcher.group(param + "N");
        }
      }
    }
    return parameter;
  }

  public Map<String, String> normalizeParameters(Map<String, String> parameterMap) {
    HashMap<String, String> normalizedMap = new HashMap<>();
    parameterMap.forEach((key, value) -> {
      String normalizedKey = normalizeParameterName(key);
      normalizedMap.put(normalizedKey, value);
    });
    return normalizedMap;
  }

}
