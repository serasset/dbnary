package org.getalp.model.dbnary;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.getalp.model.ontolex.LexicalEntry;

public class Page implements Set<LexicalEntry> {
  protected String language;
  protected String name;
  protected Set<LexicalEntry> delegate;

  public Page(String language, String name) {
    this.language = language;
    this.name = name;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<LexicalEntry> iterator() {
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(LexicalEntry lexicalEntry) {
    return delegate.add(lexicalEntry);
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends LexicalEntry> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }
}
