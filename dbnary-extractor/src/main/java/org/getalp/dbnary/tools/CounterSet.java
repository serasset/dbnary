package org.getalp.dbnary.tools;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;

public class CounterSet implements Set<String> {

  static class MutableInteger {

    int _val;

     private MutableInteger(int v) {
      super();
      _val = v;
    }

    int incr() {
      return ++_val;
    }

    public String toString() {
      return Integer.toString(_val);
    }
  }

  private final HashMap<String, MutableInteger> counters;

  public CounterSet() {
    super();
    counters = new HashMap<>();
  }

  @Override
  public void clear() {
    counters.clear();
  }

  public void resetAll() {
    clear();
  }

  public void reset(String key) {
    counters.remove(key);
  }

  public int get(String key) {
    MutableInteger i = counters.get(key);
    return (null != i) ? i._val : 0;
  }

  public int incr(String key) {
    MutableInteger i = counters.get(key);
    if (null != i) {
      return i.incr();
    } else {
      counters.put(key, new MutableInteger(1));
      return 1;
    }
  }

  public void logCounters(Logger log) {
    for (String s : counters.keySet()) {
      log.debug("{}: {}", s, counters.get(s)._val);
    }
  }

  @Override
  public int size() {
    return counters.size();
  }

  @Override
  public boolean isEmpty() {
    return counters.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    if (o instanceof String) return counters.containsKey(o);
    return false;
  }

  @Override
  public Iterator<String> iterator() {
    return counters.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return counters.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return counters.keySet().toArray(a);
  }

  @Override
  public boolean add(String s) {
    return (1 == this.incr(s));
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return counters.keySet().containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    boolean changed = false;
    for (String s: c) {
      changed = (changed || add(s));
    }
    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }
}
