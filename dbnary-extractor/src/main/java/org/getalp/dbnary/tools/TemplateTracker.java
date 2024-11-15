package org.getalp.dbnary.tools;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;

public class TemplateTracker implements Set<String> {

  static class MutableLong {

    long _val;

    private MutableLong(long v) {
      super();
      _val = v;
    }

    long incr() {
      return ++_val;
    }

    long incr(long v) {
      return _val += v;
    }

    public String toString() {
      return Long.toString(_val);
    }
  }

  private final HashMap<String, MutableLong> counters;

  public TemplateTracker() {
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

  public long get(String key) {
    MutableLong i = counters.get(key);
    return (null != i) ? i._val : 0;
  }

  public long incr(String key) {
    MutableLong i = counters.get(key);
    if (null != i) {
      return i.incr();
    } else {
      counters.put(key, new MutableLong(1));
      return 1;
    }
  }

  public long incr(String key, long increment) {
    MutableLong i = counters.get(key);
    if (null != i) {
      return i.incr(increment);
    } else {
      counters.put(key, new MutableLong(increment));
      return increment;
    }
  }


  public void trace(Logger log) {
    trace(log, "Template Expansion");
  }

  public void trace(Logger log, String msg) {
    for (Entry<String, MutableLong> s : counters.entrySet()) {
      log.trace("{}: {} --> {}", msg, s.getKey(), s.getValue()._val);
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
    if (o instanceof String)
      return counters.containsKey(o);
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
    for (String s : c) {
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
