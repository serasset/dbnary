package org.getalp.dbnary.deu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InflectedFormSet implements Iterable<Map.Entry<GermanInflectionData, Set<String>>> {
  private Map<GermanInflectionData, Set<String>> map = new HashMap<>();

  /**
   * Add all values in the current Inflected Forms Set for given key
   *
   * @param key the inflection for which the set of values are added
   * @param values the set of values to add to the map.
   */
  public void add(GermanInflectionData key, Set<String> values) {
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
  public void add(GermanInflectionData key, String value) {
    if (value.length() == 0 || value.equals("—") || value.equals("-") || value.equals("\u00A0")) {
      return;
    }
    map.computeIfAbsent(key, k -> new HashSet<>()).add(value);
  }

  public void addAll(InflectedFormSet otherSet) {
    if (null != otherSet) {
      for (Entry<GermanInflectionData, Set<String>> kv : otherSet.getMap().entrySet()) {
        this.add(kv.getKey(), kv.getValue());
      }
    }
  }

  public Map<GermanInflectionData, Set<String>> getMap() {
    return map;
  }

  @Override
  public Iterator<Map.Entry<GermanInflectionData, Set<String>>> iterator() {
    return map.entrySet().iterator();
  }
}
