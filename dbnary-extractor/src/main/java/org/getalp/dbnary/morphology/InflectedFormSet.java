package org.getalp.dbnary.morphology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InflectedFormSet implements Iterable<Map.Entry<InflectionData, Set<String>>> {
  private Map<InflectionData, Set<String>> map = new HashMap<>();

  /**
   * Add all values in the current Inflected Forms Set for given key
   *
   * @param keys the list of inflections for which the set of values are added
   * @param values the set of values to add to the map.
   */
  public void add(List<? extends InflectionData> keys, Set<String> values) {
    for (InflectionData key : keys) {
      this.add(key, values);
    }
  }

  /**
   * Add all values in the current Inflected Forms Set for given key
   *
   * @param key the inflection for which the set of values are added
   * @param values the set of values to add to the map.
   */
  public void add(InflectionData key, Set<String> values) {
    for (String value : values) {
      this.add(key, value);
    }
  }

  /**
   * Add value in the current Inflected Forms Set for given key
   *
   * @param key the inflection for which the map of values are added
   * @param value the value to add to the map.
   */
  public void add(InflectionData key, String value) {
    if (value.length() == 0 || value.equals("—") || value.equals("-") || value.equals("\u00A0")) {
      return;
    }
    map.computeIfAbsent(key, k -> new HashSet<>()).add(value);
  }

  public void addAll(InflectedFormSet otherSet) {
    if (null != otherSet) {
      for (Entry<InflectionData, Set<String>> kv : otherSet.getMap().entrySet()) {
        this.add(kv.getKey(), kv.getValue());
      }
    }
  }

  public Map<InflectionData, Set<String>> getMap() {
    return map;
  }

  @Override
  public Iterator<Map.Entry<InflectionData, Set<String>>> iterator() {
    return map.entrySet().iterator();
  }
}
