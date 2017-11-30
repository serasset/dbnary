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
   * @return
   */
  public void add(GermanInflectionData key, Set<String> values) {
    Set<String> myValues = map.get(key);
    if (null == myValues) {
      map.put(key, values);
    } else {
      myValues.addAll(values);
    }
  }

  /**
   * Add value in the current Inflected Forms Set for given key
   *
   * @param key the inflection for which the map of values are added
   * @param value the value to add to the map.
   * @return
   */
  public void add(GermanInflectionData key, String value) {
    map.computeIfAbsent(key, k -> {
      HashSet<String> newSet = new HashSet<>();
      newSet.add(value);
      return newSet;
    });
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
