package org.getalp.model.dbnary;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import org.getalp.LangTools;
import org.getalp.model.ontolex.LexicalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Page implements Set<LexicalEntry> {
  private Logger log = LoggerFactory.getLogger(Page.class);
  protected String language;
  protected String name;
  protected Stack<LexicalEntry> delegate = new Stack<>();

  public Page(String language, String name) {
    this.language = LangTools.getCode(language);
    if (this.language == null) {
      log.warn("Unknown language {} while parsing page {}", language, name);
      this.language = language;
    }
    this.name = name;
  }

  public String getShortLanguageCode() {
    String shortCode = LangTools.getPart1OrId(language);
    return null != shortCode ? shortCode : language;
  }

  public String getLongLanguageCode() {
    return language;
  }

  public String getName() {
    return name;
  }

  public LexicalEntry newEntry(String pos) {
    return this.newEntry(this.getName(), pos);
  }

  public LexicalEntry newEntry(String name, String pos) {
    LexicalEntry le = new LexicalEntry(name, pos, delegate.size());
    this.add(le);
    return le;
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
