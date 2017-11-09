package org.getalp.dbnary.tools;

/**
 * Simmplified from org apache commons lang 2.5.
 */
public final class CharRange {


  /**
   * The first character, inclusive, in the range.
   */
  private final char start;
  /**
   * The last character, inclusive, in the range.
   */
  private final char end;

  /**
   * Cached toString.
   */
  private transient String iToString;

  // -----------------------------------------------------------------------

  /**
   * <p> Constructs a <code>CharRange</code> over a single character. </p>
   *
   * @param ch only character in this range
   */
  public CharRange(char ch) {
    this(ch, ch);
  }

  /**
   * <p> Constructs a <code>CharRange</code> over a set of characters. </p>
   *
   * @param start first character, inclusive, in this range
   * @param end last character, inclusive, in this range
   */
  public CharRange(char start, char end) {
    super();
    if (start > end) {
      char temp = start;
      start = end;
      end = temp;
    }

    this.start = start;
    this.end = end;
  }

  // Accessors
  // -----------------------------------------------------------------------

  /**
   * <p> Gets the start character for this character range. </p>
   *
   * @return the start char (inclusive)
   */
  public char getStart() {
    return this.start;
  }

  /**
   * <p> Gets the end character for this character range. </p>
   *
   * @return the end char (inclusive)
   */
  public char getEnd() {
    return this.end;
  }

  /**
   * <p> Gets the end character for this character range. </p>
   *
   * @return the end char (inclusive)
   */
  public boolean contains(char c) {
    return (c >= this.start && c <= this.end);
  }


  /**
   * <p> Gets a string representation of the character range. </p>
   *
   * @return string representation of this range
   */
  public String toString() {
    if (iToString == null) {
      StringBuffer buf = new StringBuffer(4);
      buf.append(start);
      if (start != end) {
        buf.append('-');
        buf.append(end);
      }
      iToString = buf.toString();
    }
    return iToString;
  }
}
