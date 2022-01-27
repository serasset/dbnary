package org.getalp.dbnary.wiki.visit;

public interface Visitable {
  void accept(Visitor visitor);
}
