package org.getalp.dbnary.wiki.visit;

public interface Visitable {
  <T> T accept(Visitor<T> visitor);
}
