package org.getalp.dbnary.tools;

/**
 * Simmplified from org apache commons lang 2.5.
 */
public final class CharRange {


  /**
   * The first character, inclusive, in the range.
   */
  private final int start;
  /**
   * The last character, inclusive, in the range.
   */
  private final int end;

  /**
   * Cached toString.
   */
  private transient String iToString;

  // -----------------------------------------------------------------------

  /**
   * <p>
   * Constructs a <code>CharRange</code> over a single character.
   * </p>
   *
   * @param ch only character in this range
   */
  public CharRange(char ch) {
    this(ch, ch);
  }

  /**
   * <p>
   * Constructs a <code>CharRange</code> over a set of characters.
   * </p>
   *
   * @param start first character, inclusive, in this range
   * @param end last character, inclusive, in this range
   */
  public CharRange(int start, int end) {
    super();
    if (start > end) {
      int temp = start;
      start = end;
      end = temp;
    }

    this.start = start;
    this.end = end;
  }

  // Accessors
  // -----------------------------------------------------------------------

  /**
   * <p>
   * Gets the start character for this character range.
   * </p>
   *
   * @return the start char (inclusive)
   */
  public int getStart() {
    return this.start;
  }

  /**
   * <p>
   * Gets the end character for this character range.
   * </p>
   *
   * @return the end char (inclusive)
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * <p>
   * check if a char is inside the current range.
   * </p>
   *
   * @param c the character to check
   * @return true if the range contains c
   */
  public boolean contains(char c) {
    return (c >= this.start && c <= this.end);
  }
  /**
   * <p>
   * check if a code point is inside the current range.
   * </p>
   *
   * @param c the code point to check
   * @return true if the range contains c
   */
  public boolean contains(int c) {
    return (c >= this.start && c <= this.end);
  }


  /**
   * <p>
   * Gets a string representation of the character range.
   * </p>
   *
   * @return string representation of this range
   */
  public String toString() {
    if (iToString == null) {
      StringBuffer buf = new StringBuffer(4);
      buf.append("\\x{").append(Integer.toHexString(start)).append("}");
      if (start != end) {
        buf.append('-');
        buf.append("\\x{").append(Integer.toHexString(end)).append("}");
      }
      iToString = buf.toString();
    }
    return iToString;
  }
}
