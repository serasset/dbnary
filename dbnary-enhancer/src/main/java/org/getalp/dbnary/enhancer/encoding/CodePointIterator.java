package org.getalp.dbnary.enhancer.encoding;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over the UTF-16 Codepoints of a String
 */

public final class CodePointIterator implements Iterator<Integer> {

  private final String sequence;
  private int index = 0;

  public CodePointIterator(String sequence) {
    this.sequence = sequence;
  }

  public boolean hasNext() {
    return index < sequence.length();
  }

  public Integer next() {
    try {
      int codePoint = sequence.codePointAt(index++);
      if (Character.charCount(codePoint) == 2) {
        index++;
      }
      return codePoint;
    } catch (IndexOutOfBoundsException e) {
      throw new NoSuchElementException(e.getMessage());
    }
  }

  @Override
  public void remove() {
    return;
  }

  public static void main(String[] args) {
    String sample = "A" + "\uD835\uDD0A" + "B" + "C";
    int match = 0x1D50A;
    CodePointIterator pointIterator = new CodePointIterator(sample);
    while (pointIterator.hasNext()) {
      System.out.println(match == pointIterator.next());
    }
  }
}

